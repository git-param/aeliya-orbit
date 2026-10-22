package com.aeliya.orbit.modules.employee.service;

import com.aeliya.orbit.modules.employee.dto.EmployeeRequest;
import com.aeliya.orbit.modules.employee.dto.EmployeeResponse;
import com.aeliya.orbit.modules.employee.entity.Employee;
import com.aeliya.orbit.modules.employee.entity.OrganizationOption;
import com.aeliya.orbit.modules.employee.repository.EmployeeRepository;
import com.aeliya.orbit.modules.employee.repository.OrganizationOptionRepository;
import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import com.aeliya.orbit.modules.permission.service.DepartmentModuleCache;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeService {
    public record ManagerOption(Long id, String employeeCode, String fullName) {}
    private final EmployeeRepository employees;
    private final OrganizationOptionRepository options;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final DepartmentModuleCache moduleCache;
    private final PasswordEncoder passwordEncoder;
    private final ProfileFileStore files;

    public EmployeeService(EmployeeRepository employees, OrganizationOptionRepository options,
            UserRepository users, JdbcTemplate jdbc, DepartmentModuleCache moduleCache, PasswordEncoder passwordEncoder,
            ProfileFileStore files) {
        this.employees = employees;
        this.options = options;
        this.users = users;
        this.jdbc = jdbc;
        this.moduleCache = moduleCache;
        this.passwordEncoder = passwordEncoder;
        this.files = files;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(String search, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw bad("Invalid pagination");
        String normalized = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        if (normalized != null && normalized.length() > 100) throw bad("Search is too long");
        Page<Employee> result = normalized == null
            ? employees.visible(PageRequest.of(page, size))
            : employees.visibleSearch(normalized, PageRequest.of(page, size));
        return result.map(e -> response(e, false));
    }

    @Transactional(readOnly = true)
    public List<ManagerOption> managerChoices(String search, Authentication actor) {
        if (!has(actor, "EMPLOYEE_VIEW") && !has(actor, "EMPLOYEE_CREATE"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        String normalized = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        if (normalized != null && normalized.length() > 100) throw bad("Search is too long");
        List<Employee> choices = normalized == null ? employees.managerChoices(PageRequest.of(0, 30))
            : employees.managerChoicesSearch(normalized, PageRequest.of(0, 30));
        return choices.stream().map(e -> new ManagerOption(e.getId(), e.getEmployeeCode(), e.getFullName())).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(long id, Authentication actor) {
        Employee e = visible(id);
        boolean canView = has(actor, "EMPLOYEE_VIEW");
        if (!canView && !owns(e, actor)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return response(e, true);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse own(Authentication actor) {
        User user = actorUser(actor);
        Employee e = employees.findByAccountId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No employee profile is linked"));
        return response(e, true);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest input, Authentication actor) {
        require(actor, "EMPLOYEE_CREATE");
        if (input == null) throw bad("Employee details are required");
        User editor = actorUser(actor);
        Employee e = new Employee();
        String code = required(input.employeeCode(), 40).toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9][A-Z0-9_-]*") || employees.existsByEmployeeCode(code)) throw bad("Employee ID is invalid or already used");
        e.setEmployeeCode(code);
        apply(e, input);
        String password = input.password();
        if (password == null || password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw bad("Password must be at least 8 characters and at most 72 bytes");
        if (users.findByUsernameIgnoreCase(e.getWorkEmail()).isPresent()) throw bad("Work email is already used as a login");
        e.setCreatedBy(editor);
        e.setUpdatedBy(editor);
        try {
            e.setAccount(users.saveAndFlush(new User(e.getWorkEmail(), passwordEncoder.encode(password), "ROLE_USER")));
            Employee saved = employees.saveAndFlush(e);
            if (saved.getAccount() != null) moduleCache.refreshAccountAfterCommit(saved.getAccount().getId());
            return response(saved, true); }
        catch (DataIntegrityViolationException ex) { throw bad("Employee ID or work email is already used"); }
    }

    @Transactional
    public EmployeeResponse update(long id, EmployeeRequest input, Authentication actor) {
        require(actor, "EMPLOYEE_UPDATE");
        if (input == null) throw bad("Employee details are required");
        Employee e = visible(id);
        if (input.employeeCode() != null && !input.employeeCode().trim().equalsIgnoreCase(e.getEmployeeCode()))
            throw bad("Employee ID cannot be changed");
        if (input.password() != null && !input.password().isEmpty()) throw bad("Password cannot be changed here");
        apply(e, input);
        if (e.getAccount() != null && !e.getAccount().getUsername().equals(e.getWorkEmail())) {
            users.findByUsernameIgnoreCase(e.getWorkEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(e.getAccount().getId())) throw bad("Work email is already used as a login");
            });
            e.getAccount().setUsername(e.getWorkEmail());
        }
        e.setUpdatedBy(actorUser(actor));
        try { Employee saved = employees.saveAndFlush(e);
            if (saved.getAccount() != null) moduleCache.refreshAccountAfterCommit(saved.getAccount().getId());
            return response(saved, true); }
        catch (DataIntegrityViolationException ex) { throw bad("Work email is already used"); }
    }

    @Transactional
    public void deactivate(long id, Authentication actor) {
        require(actor, "EMPLOYEE_DEACTIVATE");
        Employee e = visible(id);
        e.setActive(false);
        e.setUpdatedBy(actorUser(actor));
        // Preserve the employee and every historical record. Linked login is disabled too.
        if (e.getAccount() != null) e.getAccount().setActive(false);
        if (e.getAccount() != null) moduleCache.refreshAccountAfterCommit(e.getAccount().getId());
    }

    @Transactional
    public void reactivate(long id, Authentication actor) {
        require(actor, "EMPLOYEE_UPDATE");
        Employee e = visible(id);
        e.setActive(true);
        e.setUpdatedBy(actorUser(actor));
        if (e.getAccount() != null) moduleCache.refreshAccountAfterCommit(e.getAccount().getId());
        // Account activation is a separate security decision; do not restore login here.
    }

    @Transactional(readOnly = true)
    public long activeCount(Authentication actor) {
        require(actor, "EMPLOYEE_VIEW");
        return employees.countActiveVisible();
    }

    @Transactional
    public void savePicture(long id, MultipartFile file, Authentication actor) throws IOException {
        require(actor, "EMPLOYEE_UPDATE");
        Employee employee = visible(id);
        if (employee.getAccount() == null) throw bad("Employee has no linked user account");
        files.save(employee.getAccount().getId(), ProfileFileStore.Kind.PROFILE, file);
        employee.setUpdatedBy(actorUser(actor));
    }

    @Transactional(readOnly = true)
    public Picture picture(long id, Authentication actor) throws IOException {
        Employee e = visible(id);
        if (!has(actor, "EMPLOYEE_VIEW") && !owns(e, actor)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (e.getAccount() != null) {
            var stored = files.find(e.getAccount().getId(), ProfileFileStore.Kind.PROFILE);
            if (stored != null) return new Picture(stored.contentType(), new FileSystemResource(stored.path()));
        }
        return jdbc.query("SELECT content_type, image_data FROM employee_profile_images WHERE employee_id = ?",
            rs -> rs.next() ? new Picture(rs.getString(1), new ByteArrayResource(rs.getBytes(2))) : null, id);
    }

    public record Picture(String type, Resource resource) {}

    private void apply(Employee e, EmployeeRequest in) {
        if (in == null) throw bad("Employee details are required");
        e.setFullName(required(in.fullName(), 160));
        e.setAddress(optional(in.address(), 500));
        e.setPhone(optional(in.phone(), 30));
        String email = required(in.workEmail(), 255).toLowerCase(Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw bad("Invalid work email");
        e.setWorkEmail(email);
        if (in.dateOfJoining() == null || in.dateOfJoining().isAfter(LocalDate.now().plusYears(1))) throw bad("Invalid joining date");
        if (in.dateOfBirth() != null && !in.dateOfBirth().isBefore(LocalDate.now())) throw bad("Invalid birth date");
        e.setDateOfJoining(in.dateOfJoining());
        e.setDateOfBirth(in.dateOfBirth());
        e.setDesignation(option(in.designationId(), OrganizationOption.Kind.DESIGNATION, e.getDesignation()));
        e.setDepartment(option(in.departmentId(), OrganizationOption.Kind.DEPARTMENT, e.getDepartment()));
        e.setLevel(option(in.levelId(), OrganizationOption.Kind.LEVEL, e.getLevel()));
        if (in.reportingManagerId() != null) {
            Employee manager = visible(in.reportingManagerId());
            if (!manager.isActive() || manager == e) throw bad("Invalid manager");
            Employee cursor = manager;
            while (cursor != null) {
                if (cursor.getId() != null && cursor.getId().equals(e.getId())) throw bad("Reporting cycle");
                cursor = cursor.getReportingManager();
            }
            e.setReportingManager(manager);
        } else e.setReportingManager(null);
    }

    private OrganizationOption option(Long id, OrganizationOption.Kind kind, OrganizationOption current) {
        if (id == null) return null;
        return options.findById(id).filter(o -> o.getKind() == kind && (o.isActive() || current != null && id.equals(current.getId())))
            .orElseThrow(() -> bad("Invalid organization option"));
    }

    private Employee visible(long id) {
        return employees.visibleById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private EmployeeResponse response(Employee e, boolean privateFields) {
        return new EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getFullName(), e.getWorkEmail(),
            name(e.getDesignation()), name(e.getDepartment()), name(e.getLevel()),
            optionId(e.getDesignation()), optionId(e.getDepartment()), optionId(e.getLevel()),
            e.getReportingManager() == null ? null : e.getReportingManager().getId(),
            privateFields && e.getReportingManager() != null ? e.getReportingManager().getFullName() : null, e.isActive(),
            "/api/employees/" + e.getId() + "/picture", e.getProfileFocusX(), e.getProfileFocusY(), privateFields ? e.getAddress() : null,
            privateFields ? e.getPhone() : null, privateFields ? e.getDateOfBirth() : null,
            e.getDateOfJoining());
    }

    private String name(OrganizationOption option) { return option == null ? null : option.getName(); }
    private Long optionId(OrganizationOption option) { return option == null ? null : option.getId(); }
    private boolean owns(Employee e, Authentication actor) {
        return e.getAccount() != null && e.getAccount().getUsername().equals(actor.getName());
    }
    private User actorUser(Authentication actor) {
        return users.findByUsername(actor.getName()).filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
    private void require(Authentication actor, String permission) {
        if (!has(actor, permission)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private boolean has(Authentication actor, String permission) {
        return actor != null && actor.getAuthorities().stream().anyMatch(a -> permission.equals(a.getAuthority()));
    }
    private String required(String value, int max) {
        String clean = optional(value, max);
        if (clean == null) throw bad("Required employee field is missing");
        return clean;
    }
    private String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.length() > max) throw bad("Employee field is too long");
        return clean;
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
