package com.example.sistemajava.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AuthDtos {
    public record RegisterRequest(@Email @NotBlank String email, @NotBlank String password, @NotBlank @Pattern(regexp = "\\d{11}") String cpf) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record TokenResponse(String token) {}
    public record EmailRequest(@Email @NotBlank String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword, @NotBlank @Pattern(regexp = "\\d{11}") String cpf) {}
    public record ActivateRequest(@NotBlank String token) {}
    public record CpfRequest(@NotBlank @Pattern(regexp = "\\d{11}") String cpf) {}
}


