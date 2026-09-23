package com.nfcplatform.role.repository;

import com.nfcplatform.role.entity.Role;
import com.nfcplatform.role.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleCode code);
}
