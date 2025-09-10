package com.example.sistemajava.auth;

import com.example.sistemajava.auth.dto.AuthDtos.*;
import com.example.sistemajava.security.JwtService;
import com.example.sistemajava.security.JwtBlacklistService;
import com.example.sistemajava.security.RefreshTokenService;
import com.example.sistemajava.user.User;
import com.example.sistemajava.user.UserRepository;
import com.example.sistemajava.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtBlacklistService jwtBlacklistService;

    public AuthController(UserService userService, UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService, JwtBlacklistService jwtBlacklistService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.email(), request.password(), request.cpf());
        return ResponseEntity.ok(Map.of("id", user.getId(), "email", user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtService.generateToken(userDetails.getUsername(), Map.of());
            String refresh = refreshTokenService.issue(userDetails.getUsername());
            return ResponseEntity.ok(Map.of("token", token, "refreshToken", refresh));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@Valid @RequestBody ActivateRequest request) {
        userService.activate(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@Valid @RequestBody EmailRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(userService::sendVerificationEmail);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<?> resetRequest(@Valid @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String cpf = body.get("cpf");
        if (email == null || cpf == null) return ResponseEntity.badRequest().build();
        userService.requestPasswordReset(email, cpf);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.token(), request.newPassword(), request.cpf());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recover-email")
    public ResponseEntity<?> recoverEmail(@Valid @RequestBody CpfRequest request) {
        userService.recoverEmailByCpf(request.cpf());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String refreshToken = body.get("refreshToken");
        if (email == null || refreshToken == null) return ResponseEntity.badRequest().build();
        if (!refreshTokenService.validate(email, refreshToken)) return ResponseEntity.status(401).build();
        String token = jwtService.generateToken(email, Map.of());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @RequestBody(required = false) Map<String, String> body) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtBlacklistService.blacklist(token);
        }
        if (body != null) {
            String email = body.get("email");
            String refreshToken = body.get("refreshToken");
            if (email != null && refreshToken != null) {
                refreshTokenService.revoke(email, refreshToken);
            }
        }
        return ResponseEntity.ok().build();
    }
}


