package com.nfcplatform.nfc.repository;

import com.nfcplatform.nfc.entity.NfcCard;
import com.nfcplatform.nfc.entity.NfcCardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NfcCardRepository extends JpaRepository<NfcCard, Long> {

    Optional<NfcCard> findByUuid(String uuid);

    /** Tenant-safe: only returns the card if it belongs to the given client. */
    Optional<NfcCard> findByUuidAndClientId(String uuid, Long clientId);

    Optional<NfcCard> findByTokenHash(String tokenHash);

    boolean existsBySerialNumber(String serialNumber);

    List<NfcCard> findAllByClientIdOrderByCreatedAtDesc(Long clientId);

    List<NfcCard> findAllByClientIdIn(List<Long> clientIds);

    @Query("SELECT c FROM NfcCard c WHERE " +
            "(:status IS NULL OR c.status = :status) " +
            "AND (:clientId IS NULL OR c.clientId = :clientId) " +
            "AND (:search IS NULL OR LOWER(c.serialNumber) LIKE :search)")
    Page<NfcCard> search(@Param("status") NfcCardStatus status,
                          @Param("clientId") Long clientId,
                          @Param("search") String search,
                          Pageable pageable);
}
