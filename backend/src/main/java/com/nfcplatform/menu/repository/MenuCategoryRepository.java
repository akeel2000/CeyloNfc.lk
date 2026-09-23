package com.nfcplatform.menu.repository;

import com.nfcplatform.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findAllByMenuIdOrderBySortOrderAsc(Long menuId);

    Optional<MenuCategory> findByUuidAndMenuId(String uuid, Long menuId);
}
