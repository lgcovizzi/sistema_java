package com.example.sistemajava.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserRepository userRepository;
    private final com.example.sistemajava.user.avatar.AvatarQueue avatarQueue;

    public UserController(UserRepository userRepository, com.example.sistemajava.user.avatar.AvatarQueue avatarQueue) {
        this.userRepository = userRepository;
        this.avatarQueue = avatarQueue;
    }

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return Map.of(
                "email", user.getEmail(),
                "cpf", user.getCpf(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "birthDate", user.getBirthDate(),
                "avatarUrl", user.getAvatarUrl(),
                "role", user.getRole().name()
        );
    }

    public record UpdateProfileRequest(String firstName, String lastName,
                                       @Past LocalDate birthDate,
                                       String avatarUrl) {}

    @PutMapping
    public Map<String, Object> update(@AuthenticationPrincipal UserDetails principal,
                                      @Valid @RequestBody UpdateProfileRequest req) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setBirthDate(req.birthDate());
        user.setAvatarUrl(req.avatarUrl());
        userRepository.save(user);
        return Map.of("status", "ok");
    }

    @PostMapping("/avatar")
    @ResponseStatus(code = org.springframework.http.HttpStatus.ACCEPTED)
    public Map<String, Object> uploadAvatar(@AuthenticationPrincipal UserDetails principal,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "x", required = false) Integer x,
                                            @RequestParam(value = "y", required = false) Integer y,
                                            @RequestParam(value = "w", required = false) Integer w,
                                            @RequestParam(value = "h", required = false) Integer h) throws Exception {
        if (file.getSize() <= 0 || file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Arquivo deve ter até 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Apenas JPEG/PNG são permitidos");
        }
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        java.nio.file.Path tmpDir = java.nio.file.Path.of("tmp-uploads");
        java.nio.file.Files.createDirectories(tmpDir);
        String tmpName = "avatar-" + user.getId() + "-" + java.util.UUID.randomUUID();
        java.nio.file.Path tmpPath = tmpDir.resolve(tmpName);
        file.transferTo(tmpPath.toFile());
        avatarQueue.process(new com.example.sistemajava.user.avatar.AvatarQueue.Task(user.getId().toString(), tmpName, x, y, w, h));
        return Map.of("status", "processing");
    }
}


