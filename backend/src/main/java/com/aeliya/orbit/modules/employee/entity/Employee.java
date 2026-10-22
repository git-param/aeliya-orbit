package com.aeliya.orbit.modules.employee.entity;

import com.aeliya.orbit.modules.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_code", nullable = false, length = 40)
    private String employeeCode;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id")
    private User account;
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;
    @Column(length = 500)
    private String address;
    @Column(length = 30)
    private String phone;
    @Column(name = "work_email", nullable = false)
    private String workEmail;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "designation_id")
    private OrganizationOption designation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id")
    private OrganizationOption department;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "level_id")
    private OrganizationOption level;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reporting_manager_id")
    private Employee reportingManager;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;
    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String value) { employeeCode = value; }
    public User getAccount() { return account; }
    public void setAccount(User value) { account = value; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getWorkEmail() { return workEmail; }
    public void setWorkEmail(String value) { workEmail = value; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate value) { dateOfBirth = value; }
    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public void setDateOfJoining(LocalDate value) { dateOfJoining = value; }
    public OrganizationOption getDesignation() { return designation; }
    public void setDesignation(OrganizationOption value) { designation = value; }
    public OrganizationOption getDepartment() { return department; }
    public void setDepartment(OrganizationOption value) { department = value; }
    public OrganizationOption getLevel() { return level; }
    public void setLevel(OrganizationOption value) { level = value; }
    public Employee getReportingManager() { return reportingManager; }
    public void setReportingManager(Employee value) { reportingManager = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setCreatedBy(User value) { createdBy = value; }
    public void setUpdatedBy(User value) { updatedBy = value; }
}
