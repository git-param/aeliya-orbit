// backend/src/main/java/com/aeliya/orbit/scripts/DatabaseSeeder.java
package com.aeliya.orbit.scripts;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedSuperAdmin();
    }

    private void seedSuperAdmin() {
        String superAdminUsername = "param";
        
        // Check if the user already exists to prevent duplicate entries on restarts
        if (userRepository.findByUsername(superAdminUsername).isEmpty()) {
            System.out.println("Seeding Super Admin user...");
            
            // Hash the password before saving to the database
            String hashedPassword = passwordEncoder.encode("Param@4112"); 
            
            User superAdmin = new User(superAdminUsername, hashedPassword, "ROLE_SUPER_ADMIN");
            userRepository.save(superAdmin);
            
            System.out.println("Super Admin seeded successfully! Username: param | Password: Param@4112!");
        } else {
            System.out.println("Super Admin already exists. Skipping database seed.");
        }
    }
}