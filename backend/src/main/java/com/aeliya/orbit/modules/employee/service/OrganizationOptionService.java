package com.aeliya.orbit.modules.employee.service;

import com.aeliya.orbit.modules.employee.entity.OrganizationOption;
import com.aeliya.orbit.modules.employee.repository.OrganizationOptionRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationOptionService {
    private final OrganizationOptionRepository options;

    public OrganizationOptionService(OrganizationOptionRepository options) { this.options = options; }

    public record OptionView(Long id, String kind, String name, boolean active) {
        static OptionView from(OrganizationOption option) {
            return new OptionView(option.getId(), option.getKind().name(), option.getName(), option.isActive());
        }
    }
    public record OptionInput(OrganizationOption.Kind kind, String name) {}
    public record RenameInput(String name) {}
    public record StatusInput(boolean active) {}

    @Transactional(readOnly = true)
    public List<OptionView> list(OrganizationOption.Kind kind, Authentication actor) {
        requireManager(actor);
        if (kind == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option kind is required");
        return options.findByKindOrderByName(kind).stream().map(OptionView::from).toList();
    }

    @Transactional
    public OptionView create(OptionInput input, Authentication actor) {
        requireManager(actor);
        if (input == null || input.kind() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option kind is required");
        String name = name(input.name());
        ensureAvailable(input.kind(), name, null);
        try { return OptionView.from(options.saveAndFlush(new OrganizationOption(input.kind(), name))); }
        catch (DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Option already exists"); }
    }

    @Transactional
    public OptionView rename(long id, RenameInput input, Authentication actor) {
        requireManager(actor);
        OrganizationOption option = find(id);
        String name = name(input == null ? null : input.name());
        ensureAvailable(option.getKind(), name, id);
        option.setName(name);
        try { return OptionView.from(options.saveAndFlush(option)); }
        catch (DataIntegrityViolationException ex) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Option already exists"); }
    }

    @Transactional
    public OptionView status(long id, StatusInput input, Authentication actor) {
        requireManager(actor);
        if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        OrganizationOption option = find(id);
        option.setActive(input.active());
        return OptionView.from(options.save(option));
    }

    private OrganizationOption find(long id) {
        return options.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private String name(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 120 characters");
        return value.trim();
    }
    private void ensureAvailable(OrganizationOption.Kind kind, String name, Long except) {
        boolean taken = except == null ? options.existsByKindAndNameIgnoreCase(kind, name)
            : options.existsByKindAndNameIgnoreCaseAndIdNot(kind, name, except);
        if (taken) throw new ResponseStatusException(HttpStatus.CONFLICT, "Option already exists");
    }
    private void requireManager(Authentication actor) {
        boolean role = actor != null && actor.getAuthorities().stream().anyMatch(a ->
            "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        boolean grant = actor != null && actor.getAuthorities().stream().anyMatch(a -> "DEPARTMENT_MANAGE".equals(a.getAuthority()));
        if (!role || !grant) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
