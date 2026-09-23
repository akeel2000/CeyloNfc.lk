package com.nfcplatform.product.repository;

import com.nfcplatform.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByUuid(String uuid);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findAllByActiveTrueOrderByCreatedAtDesc();
}
