package com.nfcplatform.menu.repository;

import com.nfcplatform.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByClientId(Long clientId);

    Optional<Menu> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndClientIdNot(String slug, Long clientId);
}
