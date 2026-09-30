package com.company.ems.service;

import com.company.ems.dto.PerformanceDtos.*;
import com.company.ems.entity.*;
import com.company.ems.exception.ResourceNotFoundException;
import com.company.ems.repository.PerformanceReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeService employeeService;

    @Transactional
    public ReviewResponse create(ReviewRequest r) {
        Employee e = employeeService.getEntity(r.employeeId());
        PerformanceReview review = PerformanceReview.builder()
                .employee(e)
                .reviewDate(r.reviewDate() != null ? r.reviewDate() : LocalDate.now())
                .rating(r.rating()).goalsMetPercent(r.goalsMetPercent())
                .strengths(r.strengths()).improvements(r.improvements())
                .reviewerName(r.reviewerName()).build();
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> forEmployee(Long employeeId) {
        employeeService.getEntity(employeeId);
        return reviewRepository.findByEmployeeIdOrderByReviewDateDesc(employeeId)
                .stream().map(ReviewResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> all() {
        return reviewRepository.findAll().stream().map(ReviewResponse::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        PerformanceReview r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        reviewRepository.delete(r);
    }
}