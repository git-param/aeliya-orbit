package com.aeliya.orbit.modules.employee.service;

import com.aeliya.orbit.modules.employee.entity.Employee;
import com.aeliya.orbit.modules.employee.repository.EmployeeRepository;
import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileStorageTest {
    @TempDir Path temp;

    @Test void storesLargePortraitUnderUserIdWithoutExtension() throws Exception {
        var store = new LocalProfileFileStore(temp.toString());
        byte[] image = new byte[3_000_000];
        image[0] = (byte) 0xff; image[1] = (byte) 0xd8; image[2] = (byte) 0xff;
        store.save(42L, ProfileFileStore.Kind.PROFILE, new MockMultipartFile("file", "portrait.jpg", "image/jpeg", image));
        assertTrue(Files.isRegularFile(temp.resolve("42/profile")));
        assertEquals(image.length, Files.size(temp.resolve("42/profile")));
        assertEquals("image/jpeg", store.find(42L, ProfileFileStore.Kind.PROFILE).contentType());
    }

    @Test void acceptsPdfDocumentAndRejectsDisguisedFiles() throws Exception {
        var store = new LocalProfileFileStore(temp.toString());
        store.save(42L, ProfileFileStore.Kind.AADHAAR,
            new MockMultipartFile("file", "document.pdf", "application/pdf", "%PDF-1.7\n".getBytes()));
        assertTrue(Files.exists(temp.resolve("42/aadhar")));
        assertThrows(ResponseStatusException.class, () -> store.save(42L, ProfileFileStore.Kind.PAN,
            new MockMultipartFile("file", "fake.png", "image/png", "<html></html>".getBytes())));
        assertFalse(Files.exists(temp.resolve("42/pan")));
    }

    @Test void removingPortraitLeavesIdentityDocumentsInPlace() throws Exception {
        var store = new LocalProfileFileStore(temp.toString());
        byte[] image = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
        store.save(42L, ProfileFileStore.Kind.PROFILE,
            new MockMultipartFile("file", "portrait.jpg", "image/jpeg", image));
        store.save(42L, ProfileFileStore.Kind.AADHAAR,
            new MockMultipartFile("file", "card.pdf", "application/pdf", "%PDF-1.7\n".getBytes()));
        store.delete(42L, ProfileFileStore.Kind.PROFILE);
        assertNull(store.find(42L, ProfileFileStore.Kind.PROFILE));
        assertNotNull(store.find(42L, ProfileFileStore.Kind.AADHAAR));
    }

    @Test void removingOwnPortraitAlsoClearsLegacyImage() throws Exception {
        var employees = mock(EmployeeRepository.class);
        var users = mock(UserRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        var files = mock(ProfileFileStore.class);
        var profile = new SelfProfileService(employees, users, jdbc, files);
        var actor = new UsernamePasswordAuthenticationToken("person@example.com", null);
        var user = new User("person@example.com", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(user, "id", 42L);
        var employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", 7L);
        employee.setAccount(user);
        when(users.findByUsername("person@example.com")).thenReturn(Optional.of(user));
        when(employees.findByAccountId(42L)).thenReturn(Optional.of(employee));

        profile.deletePicture(actor);

        verify(files).delete(42L, ProfileFileStore.Kind.PROFILE);
        verify(jdbc).update("DELETE FROM employee_profile_images WHERE employee_id = ?", 7L);
    }

    @Test void documentLookupUsesAuthenticatedUsersFolder() throws Exception {
        var employees = mock(EmployeeRepository.class);
        var users = mock(UserRepository.class);
        var files = mock(ProfileFileStore.class);
        var profile = new SelfProfileService(employees, users, mock(JdbcTemplate.class), files);
        var actor = new UsernamePasswordAuthenticationToken("person@example.com", null);
        var user = new User("person@example.com", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(user, "id", 42L);
        var employee = new Employee();
        employee.setAccount(user);
        when(users.findByUsername("person@example.com")).thenReturn(Optional.of(user));
        when(employees.findByAccountId(42L)).thenReturn(Optional.of(employee));

        profile.file(ProfileFileStore.Kind.PAN, actor);
        verify(files).find(42L, ProfileFileStore.Kind.PAN);
    }

    public static void main(String[] args) throws Exception {
        ProfileStorageTest test = new ProfileStorageTest();
        test.temp = Files.createTempDirectory("profile-storage-test-");
        test.storesLargePortraitUnderUserIdWithoutExtension();
        test.acceptsPdfDocumentAndRejectsDisguisedFiles();
        test.removingPortraitLeavesIdentityDocumentsInPlace();
        test.removingOwnPortraitAlsoClearsLegacyImage();
        test.documentLookupUsesAuthenticatedUsersFolder();
    }
}
