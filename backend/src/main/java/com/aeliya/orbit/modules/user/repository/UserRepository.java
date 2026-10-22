// backend/src/main/java/com/aeliya/orbit/modules/user/repository/UserRepository.java
package com.aeliya.orbit.modules.user.repository;

import com.aeliya.orbit.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByRole(String role);
}
