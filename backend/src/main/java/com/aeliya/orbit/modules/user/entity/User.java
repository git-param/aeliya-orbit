// backend/src/main/java/com/aeliya/orbit/modules/user/entity/User.java
package com.aeliya.orbit.modules.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // e.g., "SUPER_ADMIN", "ADMIN", "USER"

    @Column(nullable = false)
    private boolean active = true;

    // Default constructor required by JPA
    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() 
    { 
        return id; 
    }
    public String getUsername() 
    { 
        return username; 
    }
    public void setUsername(String username) 
    { 
        this.username = username; 
    }
    public String getPassword() 
    { 
        return password; 
    }
    public void setPassword(String password) 
    { 
        this.password = password; 
    }
    public String getRole() 
    { 
        return role; 
    }
    public void setRole(String role) 
    { 
        this.role = role; 
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
