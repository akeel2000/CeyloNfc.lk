package com.nfcplatform.template.repository;

import com.nfcplatform.template.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    Optional<Template> findByUuid(String uuid);

    List<Template> findAllByOrderBySortOrderAsc();

    List<Template> findAllByActiveTrueOrderBySortOrderAsc();
}
