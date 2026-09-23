package com.nfcplatform.order.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.dto.PageResponse;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.order.dto.*;
import com.nfcplatform.order.entity.Order;
import com.nfcplatform.order.entity.OrderItem;
import com.nfcplatform.order.entity.OrderStatus;
import com.nfcplatform.order.entity.PaymentStatus;
import com.nfcplatform.order.repository.OrderItemRepository;
import com.nfcplatform.order.repository.OrderRepository;
import com.nfcplatform.product.entity.Product;
import com.nfcplatform.product.repository.ProductRepository;
import com.nfcplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final AuditService auditService;

    @Transactional
    public OrderResponse create(OrderCreateRequest request, UserPrincipal actor) {
        Client client = clientRepository.findByUuidAndDeletedAtIsNull(request.clientUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Client was not found"));

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setClientId(client.getId());
        order.setNotes(request.notes());
        order = orderRepository.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderCreateRequest.OrderLineRequest line : request.items()) {
            if (line.quantity() <= 0) {
                throw new ValidationException("Item quantity must be at least 1");
            }
            Product product = productRepository.findByUuid(line.productUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Product was not found: " + line.productUuid()));

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setQuantity(line.quantity());
            item.setUnitPrice(product.getPrice());
            orderItemRepository.save(item);

            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        order.setSubtotal(subtotal);
        order.setTotal(subtotal);
        order = orderRepository.save(order);

        auditService.record(actor.getId(), "ORDER_CREATE", "Order", order.getUuid(), null,
                Map.of("orderNumber", order.getOrderNumber(), "clientUuid", client.getUuid()));

        return toResponse(order, client);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(String status, String search, Pageable pageable) {
        OrderStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status);
        String searchPattern = search == null || search.isBlank() ? null : "%" + search.toLowerCase() + "%";
        Page<Order> page = orderRepository.search(statusFilter, searchPattern, pageable);
        return enrich(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByUuid(String uuid) {
        Order order = orderRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Order was not found"));
        Client client = clientRepository.findById(order.getClientId()).orElse(null);
        return toResponse(order, client);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForOwnClient(UserPrincipal actor) {
        Client client = clientRepository.findByOwnerUserIdAndDeletedAtIsNull(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No client account linked to this user"));
        return orderRepository.findAllByClientIdOrderByCreatedAtDesc(client.getId()).stream()
                .map(order -> toResponse(order, client))
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(String uuid, String status, UserPrincipal actor) {
        Order order = orderRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Order was not found"));
        order.setStatus(parseStatus(status));
        order = orderRepository.save(order);

        auditService.record(actor.getId(), "ORDER_STATUS_CHANGE", "Order", order.getUuid(), null,
                Map.of("status", order.getStatus().name()));

        Client client = clientRepository.findById(order.getClientId()).orElse(null);
        return toResponse(order, client);
    }

    @Transactional
    public OrderResponse updatePaymentStatus(String uuid, String paymentStatus, UserPrincipal actor) {
        Order order = orderRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Order was not found"));
        order.setPaymentStatus(parsePaymentStatus(paymentStatus));
        order = orderRepository.save(order);

        auditService.record(actor.getId(), "ORDER_PAYMENT_STATUS_CHANGE", "Order", order.getUuid(), null,
                Map.of("paymentStatus", order.getPaymentStatus().name()));

        Client client = clientRepository.findById(order.getClientId()).orElse(null);
        return toResponse(order, client);
    }

    private String generateOrderNumber() {
        String prefix = "ORD-" + Instant.now().truncatedTo(ChronoUnit.DAYS).toString().substring(0, 10).replace("-", "");
        long countToday = orderRepository.countByOrderNumberStartingWith(prefix);
        return prefix + "-" + String.format("%03d", countToday + 1);
    }

    /**
     * Batch-fetches clients/items/products for the whole page instead of the N+1 pattern
     * {@link #toResponse} uses for single-order lookups - a list endpoint under load is
     * exactly where "one query per row" turns into hundreds of round trips, so the list path
     * gets its own three-batched-query version instead of reusing toResponse per row.
     */
    private PageResponse<OrderResponse> enrich(Page<Order> page) {
        List<Order> orders = page.getContent();

        List<Long> clientIds = orders.stream().map(Order::getClientId).distinct().toList();
        Map<Long, Client> clientsById = new HashMap<>();
        if (!clientIds.isEmpty()) {
            clientRepository.findAllById(clientIds).forEach(c -> clientsById.put(c.getId(), c));
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> allItems = orderIds.isEmpty()
                ? List.of()
                : orderItemRepository.findAllByOrderIdIn(orderIds);

        List<Long> productIds = allItems.stream().map(OrderItem::getProductId).distinct().toList();
        Map<Long, Product> productsById = new HashMap<>();
        if (!productIds.isEmpty()) {
            productRepository.findAllById(productIds).forEach(p -> productsById.put(p.getId(), p));
        }

        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(OrderItem::getOrderId));

        return PageResponse.of(page, order -> {
            List<OrderItemDto> items = itemsByOrderId.getOrDefault(order.getId(), List.of()).stream()
                    .map(item -> {
                        Product product = productsById.get(item.getProductId());
                        return new OrderItemDto(product == null ? null : product.getUuid(),
                                product == null ? "Unknown product" : product.getName(),
                                item.getQuantity(), item.getUnitPrice());
                    })
                    .toList();
            Client client = clientsById.get(order.getClientId());
            return OrderResponse.from(order, client == null ? null : client.getUuid(),
                    client == null ? null : client.getDisplayName(), items);
        });
    }

    private OrderResponse toResponse(Order order, Client client) {
        List<OrderItemDto> items = orderItemRepository.findAllByOrderId(order.getId()).stream()
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    return new OrderItemDto(product == null ? null : product.getUuid(),
                            product == null ? "Unknown product" : product.getName(),
                            item.getQuantity(), item.getUnitPrice());
                })
                .toList();
        return OrderResponse.from(order, client == null ? null : client.getUuid(),
                client == null ? null : client.getDisplayName(), items);
    }

    private OrderStatus parseStatus(String value) {
        try {
            return OrderStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid order status: " + value);
        }
    }

    private PaymentStatus parsePaymentStatus(String value) {
        try {
            return PaymentStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid payment status: " + value);
        }
    }
}
