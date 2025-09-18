package com.sistema.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeController Tests")
class HomeControllerTest {

    @Mock
    private Model model;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
    }

    @Test
    @DisplayName("Should return home page with model attributes")
    void home_ShouldReturnHomePageWithModelAttributes() {
        // When
        String result = homeController.home(model);

        // Then
        assertThat(result).isEqualTo("index");
        verify(model).addAttribute("appName", "Sistema Java");
        verify(model).addAttribute("version", "1.0.0");
    }

    @Test
    @DisplayName("Should handle GET request to root path")
    void home_ShouldHandleGetRequestToRootPath() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("appName", "Sistema Java"))
                .andExpect(model().attribute("version", "1.0.0"));
    }

    @Test
    @DisplayName("Should redirect api-simple to root")
    void apiSimple_ShouldRedirectToRoot() {
        // When
        String result = homeController.redirectToHome();

        // Then
        assertThat(result).isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should handle GET request to api-simple path")
    void apiSimple_ShouldHandleGetRequestToApiSimplePath() throws Exception {
        // When & Then
        mockMvc.perform(get("/api-simple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("Should verify model interactions")
    void home_ShouldVerifyModelInteractions() {
        // When
        homeController.home(model);

        // Then
        verify(model, times(1)).addAttribute("appName", "Sistema Java");
        verify(model, times(1)).addAttribute("version", "1.0.0");
        verifyNoMoreInteractions(model);
    }

    @Test
    @DisplayName("Should handle multiple requests to home")
    void home_ShouldHandleMultipleRequests() throws Exception {
        // When & Then - First request
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        // When & Then - Second request
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("Should handle multiple requests to api-simple")
    void apiSimple_ShouldHandleMultipleRequests() throws Exception {
        // When & Then - First request
        mockMvc.perform(get("/api-simple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // When & Then - Second request
        mockMvc.perform(get("/api-simple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("Should verify correct HTTP methods are supported")
    void shouldVerifyCorrectHttpMethodsAreSupported() throws Exception {
        // Test GET method for root path
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        // Test GET method for api-simple path
        mockMvc.perform(get("/api-simple"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should return consistent view name")
    void home_ShouldReturnConsistentViewName() {
        // Given
        Model mockModel1 = mock(Model.class);
        Model mockModel2 = mock(Model.class);

        // When
        String result1 = homeController.home(mockModel1);
        String result2 = homeController.home(mockModel2);

        // Then
        assertThat(result1).isEqualTo("index");
        assertThat(result2).isEqualTo("index");
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("Should return consistent redirect URL")
    void apiSimple_ShouldReturnConsistentRedirectUrl() {
        // When
        String result1 = homeController.redirectToHome();
        String result2 = homeController.redirectToHome();

        // Then
        assertThat(result1).isEqualTo("redirect:/");
        assertThat(result2).isEqualTo("redirect:/");
        assertThat(result1).isEqualTo(result2);
    }
}