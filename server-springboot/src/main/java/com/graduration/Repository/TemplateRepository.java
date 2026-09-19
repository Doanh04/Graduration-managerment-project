package com.graduration.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.Constain.TemplateTypeConstain;
import com.graduration.entity.TemplateEntity;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Integer> {
    boolean existsByTemplateNameIgnoreCase(String templateName);

    boolean existsByTemplateNameIgnoreCaseAndTemplateIdNot(String templateName, Integer templateId);

    Page<TemplateEntity> findByTemplateType(TemplateTypeConstain templateType, Pageable pageable);
}
