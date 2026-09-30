package com.company.ems.repository;

import com.company.ems.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {
    List<PerformanceReview> findByEmployeeIdOrderByReviewDateDesc(Long employeeId);
    @Query("SELECT AVG(p.rating) FROM PerformanceReview p WHERE p.employee.id = :employeeId")
    Double averageRating(@Param("employeeId") Long employeeId);
    void deleteByEmployeeId(Long employeeId);
}