package com.sistema.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        // Demo authentication logic
        if ("demo".equals(username) && "demo123".equals(password)) {
            response.put("success", true);
            response.put("message", "Login realizado com sucesso!");
            response.put("redirectUrl", "/dashboard");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Credenciais inválidas. Tente novamente.");
            return ResponseEntity.status(401).body(response);
        }
    }
    
    @PostMapping("/api/login")
    public ResponseEntity<Map<String, Object>> apiLogin(@RequestBody Map<String, String> credentials) {
        // Delegate to the main login method
        return login(credentials);
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("username", "demo");
        return "dashboard";
    }
    
    @GetMapping("/admin/email-config")
    public String emailConfig(Model model) {
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("username", "admin");
        return "admin-email-config";
    }
    
    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}