package com.example.sistemajava.root;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class RootController {
    @GetMapping("/")
    public String index(Model model) {
        // fake news in-memory (em prod, viriam de um serviço/repos)
        List<Map<String, String>> news = List.of(
                Map.of("title", "Lançamento do Sistema", "url", "/auth/register", "summary", "Crie sua conta e explore o sistema."),
                Map.of("title", "Acesse seu Dashboard", "url", "/auth/login", "summary", "Faça login para acessar seu painel."),
                Map.of("title", "MailHog em dev", "url", "http://localhost:8025", "summary", "Verifique seus emails de ativação e reset.")
        );
        model.addAttribute("news", news);
        return "index";
    }
}


