package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.TemplateEntity;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Integer> {
    boolean existsByTemplateNameIgnoreCase(String templateName);

    boolean existsByTemplateNameIgnoreCaseAndTemplateIdNot(String templateName, Integer templateId);
}
