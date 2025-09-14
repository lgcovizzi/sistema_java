package com.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        // Adicionar dados dinâmicos ao modelo se necessário
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        return "index"; // Retorna o template index.html
    }
    
    @GetMapping("/api-simple")
    public String apiSimple() {
        return "redirect:/";
    }
}