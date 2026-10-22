package com.aeliya.orbit.modules.employee.service;

import com.aeliya.orbit.modules.employee.entity.OrganizationOption;
import com.aeliya.orbit.modules.employee.repository.OrganizationOptionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationOptionServiceTest {
    private final OrganizationOptionRepository repository = mock(OrganizationOptionRepository.class);
    private final OrganizationOptionService service = new OrganizationOptionService(repository,
        mock(org.springframework.jdbc.core.JdbcTemplate.class), mock(com.aeliya.orbit.modules.permission.service.DepartmentModuleCache.class));

    private Authentication actor(String... authorities) {
        return new UsernamePasswordAuthenticationToken("operator", null,
            java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }

    @Test void onlyAdminOrSuperAdminWithGrantCanManage() {
        var user = actor("ROLE_USER", "DEPARTMENT_MANAGE");
        var adminWithoutGrant = actor("ROLE_ADMIN");
        assertThrows(ResponseStatusException.class, () -> service.list(OrganizationOption.Kind.DEPARTMENT, user));
        assertThrows(ResponseStatusException.class, () -> service.create(new OrganizationOptionService.OptionInput(OrganizationOption.Kind.LEVEL, "L1"), adminWithoutGrant));
        verifyNoInteractions(repository);
    }

    @Test void archivePreservesRowAndRenameRejectsDuplicates() {
        var admin = actor("ROLE_ADMIN", "DEPARTMENT_MANAGE");
        var option = new OrganizationOption(OrganizationOption.Kind.DESIGNATION, "Engineer");
        ReflectionTestUtils.setField(option, "id", 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(option));
        when(repository.save(option)).thenReturn(option);
        var archived = service.status(7L, new OrganizationOptionService.StatusInput(false), admin);
        assertFalse(archived.active());
        verify(repository).save(option);
        when(repository.existsByKindAndNameIgnoreCaseAndIdNot(OrganizationOption.Kind.DESIGNATION, "Manager", 7L)).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.rename(7L, new OrganizationOptionService.RenameInput("Manager"), admin));
        assertEquals("Engineer", option.getName());
    }

    @Test void listedOptionsIncludeArchivedForConfiguration() {
        var admin = actor("ROLE_SUPER_ADMIN", "DEPARTMENT_MANAGE");
        var option = new OrganizationOption(OrganizationOption.Kind.LEVEL, "L2");
        option.setActive(false);
        ReflectionTestUtils.setField(option, "id", 4L);
        when(repository.findByKindOrderByName(OrganizationOption.Kind.LEVEL)).thenReturn(List.of(option));
        assertFalse(service.list(OrganizationOption.Kind.LEVEL, admin).getFirst().active());
    }

    public static void main(String[] args) {
        var test = new OrganizationOptionServiceTest();
        test.onlyAdminOrSuperAdminWithGrantCanManage();
        test.archivePreservesRowAndRenameRejectsDuplicates();
        test.listedOptionsIncludeArchivedForConfiguration();
    }
}
