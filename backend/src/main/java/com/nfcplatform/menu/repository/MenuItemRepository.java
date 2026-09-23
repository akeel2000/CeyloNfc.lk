package com.nfcplatform.menu.repository;

import com.nfcplatform.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findAllByCategoryIdOrderBySortOrderAsc(Long categoryId);

    List<MenuItem> findAllByCategoryIdInOrderBySortOrderAsc(List<Long> categoryIds);

    Optional<MenuItem> findByUuidAndCategoryId(String uuid, Long categoryId);
}
