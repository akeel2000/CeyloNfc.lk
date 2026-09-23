package com.nfcplatform.product.service;

import com.nfcplatform.common.exception.ConflictException;
import com.nfcplatform.common.exception.ResourceNotFoundException;
import com.nfcplatform.common.exception.ValidationException;
import com.nfcplatform.product.dto.ProductRequest;
import com.nfcplatform.product.dto.ProductResponse;
import com.nfcplatform.product.entity.Product;
import com.nfcplatform.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for product SKU uniqueness on both create and update - see the fix alongside these
 * tests in docs/PROJECT_PROGRESS.md: update previously only reused create's validation for
 * every field except the SKU conflict check, so renaming a product's SKU to collide with
 * another product's would have surfaced as a raw DB unique-constraint error instead of a clean
 * ConflictException. Pure Mockito, no Spring context/DB.
 */
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productService = new ProductService(productRepository);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createRejectsADuplicateSku() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request("SKU-1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsAnUnrecognizedProductType() {
        when(productRepository.existsBySku(any())).thenReturn(false);

        assertThatThrownBy(() -> productService.create(
                new ProductRequest("Widget", "SKU-1", null, BigDecimal.TEN, null, "NOT_A_TYPE", true)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateThrowsForAnUnknownProduct() {
        when(productRepository.findByUuid("uuid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update("uuid", request("SKU-1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsRenamingTheSkuToOneAlreadyUsedByAnotherProduct() {
        Product product = product(1L, "SKU-OLD");
        when(productRepository.findByUuid("uuid")).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-TAKEN", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.update("uuid", request("SKU-TAKEN")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateAllowsKeepingTheProductsOwnUnchangedSku() {
        Product product = product(1L, "SKU-1");
        when(productRepository.findByUuid("uuid")).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-1", 1L)).thenReturn(false);

        ProductResponse response = productService.update("uuid", request("SKU-1"));

        assertThat(response.sku()).isEqualTo("SKU-1");
    }

    private ProductRequest request(String sku) {
        return new ProductRequest("Widget", sku, "A widget", new BigDecimal("100.00"), null, "BUSINESS_CARD", true);
    }

    private Product product(long id, String sku) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        return product;
    }
}
