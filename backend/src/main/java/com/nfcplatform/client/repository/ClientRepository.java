package com.nfcplatform.client.repository;

import com.nfcplatform.client.entity.Client;
import com.nfcplatform.client.entity.ClientStatus;
import com.nfcplatform.client.entity.ClientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUuidAndDeletedAtIsNull(String uuid);

    Optional<Client> findByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @Query("SELECT c FROM Client c WHERE c.deletedAt IS NULL " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:type IS NULL OR c.type = :type) " +
            "AND (:search IS NULL OR LOWER(c.displayName) LIKE :search OR LOWER(c.email) LIKE :search)")
    Page<Client> search(@Param("status") ClientStatus status,
                         @Param("type") ClientType type,
                         @Param("search") String search,
                         Pageable pageable);
}
