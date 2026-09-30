package com.company.ems.dto;

import com.company.ems.entity.Role;
import jakarta.validation.constraints.*;
public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, String tokenType, long expiresInSeconds,
                                String username, Role role, Long employeeId) {}
    public record RegisterRequest(
            @NotBlank @Size(min=4,max=60) String username,
            @NotBlank @Size(min=6,max=100) String password,
            @NotNull Role role, Long employeeId) {}
    public record UserResponse(Long id, String username, Role role, Long employeeId, boolean enabled) {}
}