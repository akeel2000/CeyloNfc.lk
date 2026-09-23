package com.nfcplatform.order.repository;

import com.nfcplatform.order.entity.Order;
import com.nfcplatform.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByUuid(String uuid);

    long countByOrderNumberStartingWith(String prefix);

    List<Order> findAllByClientIdOrderByCreatedAtDesc(Long clientId);

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) " +
            "AND (:search IS NULL OR LOWER(o.orderNumber) LIKE :search)")
    Page<Order> search(@Param("status") OrderStatus status, @Param("search") String search, Pageable pageable);
}
