package com.aeliya.orbit.modules.employee.controller;

import com.aeliya.orbit.modules.employee.dto.EmployeeRequest;
import com.aeliya.orbit.modules.employee.dto.EmployeeResponse;
import com.aeliya.orbit.modules.employee.service.EmployeeService;
import java.io.IOException;
import org.springframework.core.io.Resource;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employees;
    public EmployeeController(EmployeeService employees) { this.employees = employees; }


    @GetMapping
    public Page<EmployeeResponse> list(@RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return employees.list(search, page, size);
    }
    @GetMapping("/me")
    public EmployeeResponse own(Authentication actor) { return employees.own(actor); }
    @GetMapping("/count")
    public Map<String, Long> count(Authentication actor) { return Map.of("activeEmployees", employees.activeCount(actor)); }
    @GetMapping("/manager-options")
    public java.util.List<EmployeeService.ManagerOption> managers(@RequestParam(required = false) String search, Authentication actor) {
        return employees.managerChoices(search, actor);
    }
    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable long id, Authentication actor) { return employees.get(id, actor); }
    @PostMapping
    public EmployeeResponse create(@RequestBody EmployeeRequest request, Authentication actor) {
        return employees.create(request, actor);
    }
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable long id, @RequestBody EmployeeRequest request, Authentication actor) {
        return employees.update(id, request, actor);
    }
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable long id, Authentication actor) {
        employees.deactivate(id, actor);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable long id, Authentication actor) {
        employees.reactivate(id, actor);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(path = "/{id}/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> pictureUpload(@PathVariable long id, @RequestParam MultipartFile file, Authentication actor) throws IOException {
        employees.savePicture(id, file, actor);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/picture")
    public ResponseEntity<Resource> picture(@PathVariable long id, Authentication actor) throws IOException {
        EmployeeService.Picture picture = employees.picture(id, actor);
        return picture == null ? ResponseEntity.notFound().build() : ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(picture.type()))
            .header("Cache-Control", "private, no-store")
            .body(picture.resource());
    }
}
