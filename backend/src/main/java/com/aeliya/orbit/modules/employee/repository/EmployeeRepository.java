package com.aeliya.orbit.modules.employee.repository;

import com.aeliya.orbit.modules.employee.entity.Employee;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @EntityGraph(attributePaths = {"account", "designation", "department", "level"})
    @Query("select e from Employee e left join e.account a where a is null or a.role <> 'ROLE_SUPER_ADMIN'")
    Page<Employee> visible(Pageable pageable);
    @EntityGraph(attributePaths = {"account", "designation", "department", "level"})
    @Query("select e from Employee e left join e.account a where (a is null or a.role <> 'ROLE_SUPER_ADMIN') and " +
           "(lower(e.fullName) like concat('%', :search, '%') or lower(e.employeeCode) like concat('%', :search, '%'))")
    Page<Employee> visibleSearch(@Param("search") String search, Pageable pageable);
    @EntityGraph(attributePaths = {"account", "designation", "department", "level", "reportingManager"})
    @Query("select e from Employee e left join e.account a where e.id = :id and (a is null or a.role <> 'ROLE_SUPER_ADMIN')")
    Optional<Employee> visibleById(@Param("id") Long id);
    @EntityGraph(attributePaths = {"designation", "department", "level"})
    Optional<Employee> findByAccountId(Long accountId);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByWorkEmail(String workEmail);
    boolean existsByAccountId(Long accountId);
    @Query("select count(e) from Employee e left join e.account a where e.active = true and (a is null or a.role <> 'ROLE_SUPER_ADMIN')")
    long countActiveVisible();
}
