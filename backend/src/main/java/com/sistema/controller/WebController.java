package com.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/web")
    public String index(Model model) {
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("status", "Online");
        return "index";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("status", "Online");
        return "index";
    }
}