package com.nfcplatform.destination.repository;

import com.nfcplatform.destination.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    Optional<Destination> findByUuidAndClientId(String uuid, Long clientId);

    Optional<Destination> findByUuid(String uuid);

    List<Destination> findAllByClientIdOrderByCreatedAtDesc(Long clientId);
}
