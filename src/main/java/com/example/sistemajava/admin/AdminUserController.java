package com.example.sistemajava.admin;

import com.example.sistemajava.user.Role;
import com.example.sistemajava.user.User;
import com.example.sistemajava.user.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<User> list(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable UUID id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record UpdateRoleRequest(@NotBlank String role) {}

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable UUID id, @RequestBody UpdateRoleRequest req) {
        return userRepository.findById(id)
                .map(u -> {
                    u.setRole(Role.valueOf(req.role()));
                    userRepository.save(u);
                    return ResponseEntity.ok(Map.of("status", "ok"));
                }).orElse(ResponseEntity.notFound().build());
    }

    public record UpdateEnabledRequest(boolean enabled) {}

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<?> setEnabled(@PathVariable UUID id, @RequestBody UpdateEnabledRequest req) {
        return userRepository.findById(id)
                .map(u -> {
                    u.setEnabled(req.enabled());
                    userRepository.save(u);
                    return ResponseEntity.ok(Map.of("status", "ok"));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}


