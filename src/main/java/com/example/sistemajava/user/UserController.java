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

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
    public Map<String, Object> uploadAvatar(@AuthenticationPrincipal UserDetails principal,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "x", required = false) Integer x,
                                            @RequestParam(value = "y", required = false) Integer y,
                                            @RequestParam(value = "w", required = false) Integer w,
                                            @RequestParam(value = "h", required = false) Integer h) throws Exception {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        String url = com.example.sistemajava.user.avatar.ImageService.processAndStore(user.getId().toString(), file, x, y, w, h);
        user.setAvatarUrl(url);
        userRepository.save(user);
        return Map.of("avatarUrl", url);
    }
}


