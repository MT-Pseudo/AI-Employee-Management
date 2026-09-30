package com.company.ems.service;

import com.company.ems.entity.*;
import com.company.ems.exception.*;
import com.company.ems.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("securityUtils")
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new BusinessException("No authenticated user");
        return auth.getName();
    }

    public User currentUser() {
        return userRepository.findByUsernameIgnoreCase(currentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    public Long currentEmployeeId() {
        Long id = currentUser().getEmployeeId();
        if (id == null) throw new BusinessException("Account is not linked to an employee record");
        return id;
    }

    public boolean hasAnyRole(Role... roles) {
        Role cur = currentUser().getRole();
        for (Role r : roles) if (r == cur) return true;
        return false;
    }

    public void requireSelfOrPrivileged(Long employeeId) {
        if (hasAnyRole(Role.ADMIN, Role.HR, Role.MANAGER)) return;
        if (!currentEmployeeId().equals(employeeId))
            throw new BusinessException("You can only access your own records");
    }
}