package com.company.ems.service;

import com.company.ems.dto.AuthDtos.*;
import com.company.ems.entity.User;
import com.company.ems.exception.*;
import com.company.ems.repository.*;
import com.company.ems.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs()/1000,
                user.getUsername(), user.getRole(), user.getEmployeeId());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username()))
            throw new BusinessException("Username already taken: " + request.username());
        if (request.employeeId() != null && !employeeRepository.existsById(request.employeeId()))
            throw new ResourceNotFoundException("Employee not found: " + request.employeeId());
        if (request.employeeId() != null && userRepository.findByEmployeeId(request.employeeId()).isPresent())
            throw new BusinessException("Employee already has a login");

        User u = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .employeeId(request.employeeId())
                .enabled(true).build();
        return toResponse(userRepository.save(u));
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole(), u.getEmployeeId(), u.isEnabled());
    }
}