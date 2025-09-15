package com.sistema.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Testes unitários para HomeController
 * Seguindo práticas de TDD com padrão Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HomeController Tests")
class HomeControllerTest {

    @Mock
    private Model model;

    @InjectMocks
    private HomeController homeController;

    @Test
    @DisplayName("Deve retornar template index quando página inicial é acessada")
    void shouldReturnIndexTemplateWhenHomePageIsAccessed() {
        // When
        String viewName = homeController.home(model);

        // Then
        assertThat(viewName).isEqualTo("index");
        verify(model).addAttribute("appName", "Sistema Java");
        verify(model).addAttribute("version", "1.0.0");
    }

    @Test
    @DisplayName("Deve redirecionar para home quando api-simple é acessado")
    void shouldRedirectToHomeWhenApiSimpleIsAccessed() {
        // When
        String redirect = homeController.apiSimple();

        // Then
        assertThat(redirect).isEqualTo("redirect:/");
    }
}