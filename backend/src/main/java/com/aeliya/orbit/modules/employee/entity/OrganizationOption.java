package com.aeliya.orbit.modules.employee.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_options")
public class OrganizationOption {
    public enum Kind { DEPARTMENT, DESIGNATION, LEVEL }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Kind kind;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false)
    private boolean active = true;
    protected OrganizationOption() {}
    public OrganizationOption(Kind kind, String name) { this.kind = kind; this.name = name; }
    public Long getId() { return id; }
    public Kind getKind() { return kind; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
