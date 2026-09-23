package com.nfcplatform.product.service;

import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.product.dto.ProductRequest;
import com.nfcplatform.product.dto.ProductResponse;
import com.nfcplatform.product.entity.Product;
import com.nfcplatform.product.entity.ProductType;
import com.nfcplatform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ConflictException("A product with this SKU already exists");
        }
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(String uuid, ProductRequest request) {
        Product product = productRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found"));
        if (productRepository.existsBySkuAndIdNot(request.sku(), product.getId())) {
            throw new ConflictException("A product with this SKU already exists");
        }
        apply(product, request);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listAllForAdmin() {
        return productRepository.findAllByOrderByCreatedAtDesc().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveForPublic() {
        return productRepository.findAllByActiveTrueOrderByCreatedAtDesc().stream().map(ProductResponse::from).toList();
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setImage(request.image());
        product.setType(parseType(request.type()));
        product.setActive(request.active() == null || request.active());
    }

    private ProductType parseType(String value) {
        try {
            return ProductType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid product type: " + value);
        }
    }
}
