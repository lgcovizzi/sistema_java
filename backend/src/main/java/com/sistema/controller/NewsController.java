package com.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/news")
public class NewsController {

    @GetMapping
    public String newsPage(Model model) {
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        return "news";
    }
}