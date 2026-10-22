package com.aeliya.orbit.modules.employee.controller;

import com.aeliya.orbit.modules.employee.entity.OrganizationOption;
import com.aeliya.orbit.modules.employee.repository.OrganizationOptionRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organization/options")
public class OrganizationOptionController {
    private final OrganizationOptionRepository options;
    public OrganizationOptionController(OrganizationOptionRepository options) { this.options = options; }
    public record OptionResponse(Long id, String kind, String name) {}

    @GetMapping
    public List<OptionResponse> list(@RequestParam OrganizationOption.Kind kind) {
        return options.findByKindAndActiveTrueOrderByName(kind).stream()
            .map(o -> new OptionResponse(o.getId(), o.getKind().name(), o.getName())).toList();
    }
}
