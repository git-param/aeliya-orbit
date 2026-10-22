package com.aeliya.orbit.modules.employee.repository;

import com.aeliya.orbit.modules.employee.entity.OrganizationOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationOptionRepository extends JpaRepository<OrganizationOption, Long> {
    List<OrganizationOption> findByKindAndActiveTrueOrderByName(OrganizationOption.Kind kind);
    boolean existsByKindAndName(OrganizationOption.Kind kind, String name);
}
