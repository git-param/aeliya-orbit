package com.aeliya.orbit.modules.employee.controller;

import com.aeliya.orbit.modules.employee.entity.OrganizationOption;
import com.aeliya.orbit.modules.employee.service.OrganizationOptionService;
import com.aeliya.orbit.modules.employee.service.OrganizationOptionService.OptionInput;
import com.aeliya.orbit.modules.employee.service.OrganizationOptionService.OptionView;
import com.aeliya.orbit.modules.employee.service.OrganizationOptionService.RenameInput;
import com.aeliya.orbit.modules.employee.service.OrganizationOptionService.StatusInput;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/organization/options")
public class OrganizationConfigController {
    private final OrganizationOptionService options;
    public OrganizationConfigController(OrganizationOptionService options) { this.options = options; }

    @GetMapping
    public List<OptionView> list(@RequestParam OrganizationOption.Kind kind, Authentication actor) {
        return options.list(kind, actor);
    }
    @PostMapping
    public OptionView create(@RequestBody OptionInput input, Authentication actor) {
        return options.create(input, actor);
    }
    @PutMapping("/{id}")
    public OptionView rename(@PathVariable long id, @RequestBody RenameInput input, Authentication actor) {
        return options.rename(id, input, actor);
    }
    @PutMapping("/{id}/status")
    public OptionView status(@PathVariable long id, @RequestBody StatusInput input, Authentication actor) {
        return options.status(id, input, actor);
    }
}
