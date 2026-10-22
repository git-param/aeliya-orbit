package com.aeliya.orbit.modules.employee.controller;

import com.aeliya.orbit.modules.employee.entity.OrganizationOption;
import com.aeliya.orbit.modules.employee.repository.OrganizationOptionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/organization/options")
public class OrganizationOptionController {
    private final OrganizationOptionRepository options;
    public OrganizationOptionController(OrganizationOptionRepository options) { this.options = options; }
    public record OptionResponse(Long id, String kind, String name) {}
    public record OptionRequest(OrganizationOption.Kind kind, String name) {}

    @GetMapping
    public List<OptionResponse> list(@RequestParam OrganizationOption.Kind kind) {
        return options.findByKindAndActiveTrueOrderByName(kind).stream()
            .map(o -> new OptionResponse(o.getId(), o.getKind().name(), o.getName())).toList();
    }
    @PostMapping
    public OptionResponse create(@RequestBody OptionRequest input) {
        if (input == null || input.kind() == null || input.name() == null || input.name().isBlank() || input.name().trim().length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid organization option");
        String name = input.name().trim();
        if (options.existsByKindAndName(input.kind(), name)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Option exists");
        OrganizationOption result = options.save(new OrganizationOption(input.kind(), name));
        return new OptionResponse(result.getId(), result.getKind().name(), result.getName());
    }
}
