package com.nfcplatform.packageplan.repository;

import com.nfcplatform.packageplan.entity.PackagePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackagePlanRepository extends JpaRepository<PackagePlan, Long> {

    Optional<PackagePlan> findByUuid(String uuid);

    List<PackagePlan> findAllByOrderBySortOrderAsc();

    List<PackagePlan> findAllByActiveTrueOrderBySortOrderAsc();
}
