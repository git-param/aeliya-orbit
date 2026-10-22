package com.aeliya.orbit.modules.employee.service;

import com.aeliya.orbit.modules.employee.entity.Employee;
import com.aeliya.orbit.modules.employee.repository.EmployeeRepository;
import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SelfProfileService {
    public record SecondaryPhone(long id, String phone) {}
    public record Profile(String fullName, String employeeCode, String workEmail, String address, String primaryPhone,
        int focusX, int focusY, String pictureUrl, List<SecondaryPhone> secondaryPhones,
        boolean aadhaarUploaded, boolean panUploaded, boolean pictureUploaded) {}
    public record Update(String address, int focusX, int focusY) {}
    public record PhoneInput(String phone) {}

    private final EmployeeRepository employees;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final ProfileFileStore files;

    public SelfProfileService(EmployeeRepository employees, UserRepository users, JdbcTemplate jdbc, ProfileFileStore files) {
        this.employees = employees;
        this.users = users;
        this.jdbc = jdbc;
        this.files = files;
    }

    @Transactional(readOnly = true)
    public Profile get(Authentication actor) throws IOException {
        Employee employee = own(actor);
        long userId = employee.getAccount().getId();
        List<SecondaryPhone> phones = jdbc.query("SELECT id, phone FROM employee_secondary_phones WHERE employee_id = ? ORDER BY id",
            (rs, row) -> new SecondaryPhone(rs.getLong(1), rs.getString(2)), employee.getId());
        boolean pictureUploaded = files.find(userId, ProfileFileStore.Kind.PROFILE) != null ||
            Boolean.TRUE.equals(jdbc.query("SELECT EXISTS (SELECT 1 FROM employee_profile_images WHERE employee_id = ?)",
                rs -> rs.next() && rs.getBoolean(1), employee.getId()));
        return new Profile(employee.getFullName(), employee.getEmployeeCode(), employee.getWorkEmail(), employee.getAddress(),
            employee.getPhone(), employee.getProfileFocusX(), employee.getProfileFocusY(),
            "/api/employees/" + employee.getId() + "/picture", phones,
            files.find(userId, ProfileFileStore.Kind.AADHAAR) != null,
            files.find(userId, ProfileFileStore.Kind.PAN) != null, pictureUploaded);
    }

    @Transactional
    public Profile update(Update input, Authentication actor) throws IOException {
        if (input == null || input.focusX() < 0 || input.focusX() > 100 || input.focusY() < 0 || input.focusY() > 100)
            throw bad("Invalid profile framing");
        Employee employee = own(actor);
        String address = input.address() == null ? null : input.address().trim();
        if (address != null && address.length() > 500) throw bad("Address is too long");
        employee.setAddress(address == null || address.isEmpty() ? null : address);
        employee.setProfileFocusX(input.focusX());
        employee.setProfileFocusY(input.focusY());
        employee.setUpdatedBy(employee.getAccount());
        employees.saveAndFlush(employee);
        return get(actor);
    }

    @Transactional
    public List<SecondaryPhone> addPhone(PhoneInput input, Authentication actor) {
        Employee employee = own(actor);
        String phone = input == null || input.phone() == null ? "" : input.phone().trim();
        if (!phone.matches("^[+0-9][0-9 ()-]{6,29}$")) throw bad("Invalid mobile number");
        if (phone.equals(employee.getPhone())) throw bad("This is already the primary mobile number");
        try { jdbc.update("INSERT INTO employee_secondary_phones(employee_id, phone) VALUES (?, ?)", employee.getId(), phone); }
        catch (DataIntegrityViolationException ex) { throw bad("This mobile number is already listed"); }
        return phones(employee.getId());
    }

    @Transactional
    public List<SecondaryPhone> deletePhone(long phoneId, Authentication actor) {
        Employee employee = own(actor);
        int removed = jdbc.update("DELETE FROM employee_secondary_phones WHERE id = ? AND employee_id = ?", phoneId, employee.getId());
        if (removed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return phones(employee.getId());
    }

    @Transactional(readOnly = true)
    public ProfileFileStore.StoredFile file(ProfileFileStore.Kind kind, Authentication actor) throws IOException {
        Employee employee = own(actor);
        return files.find(employee.getAccount().getId(), kind);
    }

    public void upload(ProfileFileStore.Kind kind, MultipartFile upload, Authentication actor) throws IOException {
        Employee employee = own(actor);
        files.save(employee.getAccount().getId(), kind, upload);
    }

    @Transactional
    public void deletePicture(Authentication actor) throws IOException {
        Employee employee = own(actor);
        files.delete(employee.getAccount().getId(), ProfileFileStore.Kind.PROFILE);
        jdbc.update("DELETE FROM employee_profile_images WHERE employee_id = ?", employee.getId());
    }

    private List<SecondaryPhone> phones(long employeeId) {
        return jdbc.query("SELECT id, phone FROM employee_secondary_phones WHERE employee_id = ? ORDER BY id",
            (rs, row) -> new SecondaryPhone(rs.getLong(1), rs.getString(2)), employeeId);
    }

    private Employee own(Authentication actor) {
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User user = users.findByUsername(actor.getName()).filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return employees.findByAccountId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No employee profile is linked"));
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
