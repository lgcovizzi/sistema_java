package com.example.sistemajava.dashboard;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public Map<String, String> userDash() {
        return Map.of("dashboard", "USER");
    }

    @GetMapping("/associado")
    @PreAuthorize("hasRole('ASSOCIADO')")
    public Map<String, String> associadoDash() {
        return Map.of("dashboard", "ASSOCIADO");
    }

    @GetMapping("/colaborador")
    @PreAuthorize("hasRole('COLABORADOR')")
    public Map<String, String> colaboradorDash() {
        return Map.of("dashboard", "COLABORADOR");
    }

    @GetMapping("/parceiro")
    @PreAuthorize("hasRole('PARCEIRO')")
    public Map<String, String> parceiroDash() {
        return Map.of("dashboard", "PARCEIRO");
    }

    @GetMapping("/fundador")
    @PreAuthorize("hasRole('FUNDADOR')")
    public Map<String, String> fundadorDash() {
        return Map.of("dashboard", "FUNDADOR");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminDash() {
        return Map.of("dashboard", "ADMIN");
    }
}


