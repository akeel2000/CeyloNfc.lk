package com.nfcplatform.order.service;

import com.nfcplatform.audit.service.AuditService;
import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.repository.ClientRepository;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.order.dto.OrderCreateRequest;
import com.nfcplatform.order.entity.Order;
import com.nfcplatform.order.repository.OrderItemRepository;
import com.nfcplatform.order.repository.OrderRepository;
import com.nfcplatform.product.entity.Product;
import com.nfcplatform.product.entity.ProductType;
import com.nfcplatform.product.repository.ProductRepository;
import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import com.nfcplatform.security.UserPrincipal;
import com.nfcplatform.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for order total computation and order number generation - see
 * docs/PROJECT_PROGRESS.md Phase 8 ("resolves each line's product to snapshot its current
 * price into OrderItem"). Pure Mockito, no Spring context/DB.
 */
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AuditService auditService;

    private OrderService orderService;

    private static final String CLIENT_UUID = "client-uuid";
    private static final long CLIENT_ID = 5L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, orderItemRepository, productRepository, clientRepository,
                auditService);

        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setDisplayName("Test Client");
        when(clientRepository.findByUuidAndDeletedAtIsNull(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(any())).thenReturn(List.of());
    }

    @Test
    void subtotalIsTheSumOfEachLinesCurrentUnitPriceTimesQuantity() {
        when(orderRepository.countByOrderNumberStartingWith(any())).thenReturn(0L);
        when(productRepository.findByUuid("card-uuid")).thenReturn(Optional.of(product("card-uuid", "49.99")));
        when(productRepository.findByUuid("stand-uuid")).thenReturn(Optional.of(product("stand-uuid", "15.00")));

        var request = new OrderCreateRequest(CLIENT_UUID, List.of(
                new OrderCreateRequest.OrderLineRequest("card-uuid", 3),
                new OrderCreateRequest.OrderLineRequest("stand-uuid", 2)
        ), null);

        var principal = principal();
        orderService.create(request, principal);

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(savedOrder.capture());

        // 3 * 49.99 + 2 * 15.00 = 149.97 + 30.00 = 179.97
        Order finalSave = savedOrder.getAllValues().get(savedOrder.getAllValues().size() - 1);
        assertThat(finalSave.getSubtotal()).isEqualByComparingTo("179.97");
        assertThat(finalSave.getTotal()).isEqualByComparingTo("179.97");
    }

    @Test
    void orderNumberIncrementsThePerDaySequenceFromExistingCount() {
        when(orderRepository.countByOrderNumberStartingWith(any())).thenReturn(4L);
        when(productRepository.findByUuid("card-uuid")).thenReturn(Optional.of(product("card-uuid", "10.00")));

        var request = new OrderCreateRequest(CLIENT_UUID,
                List.of(new OrderCreateRequest.OrderLineRequest("card-uuid", 1)), null);

        orderService.create(request, principal());

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(savedOrder.capture());

        assertThat(savedOrder.getValue().getOrderNumber())
                .matches("ORD-\\d{8}-005");
    }

    @Test
    void rejectsAZeroOrNegativeQuantityLine() {
        when(orderRepository.countByOrderNumberStartingWith(any())).thenReturn(0L);

        var request = new OrderCreateRequest(CLIENT_UUID,
                List.of(new OrderCreateRequest.OrderLineRequest("card-uuid", 0)), null);

        assertThatThrownBy(() -> orderService.create(request, principal()))
                .isInstanceOf(ValidationException.class);
    }

    private Product product(String uuid, String price) {
        Product product = new Product();
        product.setUuid(uuid);
        product.setName("Product " + uuid);
        product.setSku("SKU-" + uuid);
        product.setPrice(new BigDecimal(price));
        product.setType(ProductType.METAL_CARD);
        return product;
    }

    private UserPrincipal principal() {
        Role role = new Role();
        role.setCode(RoleCode.SUPER_ADMIN);
        role.setName("SUPER_ADMIN");

        User user = new User();
        user.setId(1L);
        user.setEmail("admin@test.local");
        user.setPasswordHash("hash");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }
}
