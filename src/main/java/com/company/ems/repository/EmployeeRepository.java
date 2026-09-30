package com.company.ems.repository;

import com.company.ems.entity.Employee;
import com.company.ems.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Employee> findByDepartmentIgnoreCase(String department);
    List<Employee> findByStatus(EmployeeStatus status);
    long countByStatus(EmployeeStatus status);

    @Query("""
            SELECT e FROM Employee e
            WHERE LOWER(CONCAT(e.firstName,' ',e.lastName)) LIKE LOWER(CONCAT('%',:q,'%'))
               OR LOWER(e.email) LIKE LOWER(CONCAT('%',:q,'%'))
               OR LOWER(COALESCE(e.department,'')) LIKE LOWER(CONCAT('%',:q,'%'))
            """)
    Page<Employee> search(@Param("q") String query, Pageable pageable);

    @Query("SELECT DISTINCT e.department FROM Employee e WHERE e.department IS NOT NULL ORDER BY e.department")
    List<String> findDistinctDepartments();
}