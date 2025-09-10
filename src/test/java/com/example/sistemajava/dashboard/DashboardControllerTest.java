package com.example.sistemajava.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.example.sistemajava.security.JwtAuthenticationFilter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import(TestSecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = {"USER"})
    void user_can_access_user_dashboard() throws Exception {
        mvc.perform(get("/dashboard/user")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void user_cannot_access_admin_dashboard() throws Exception {
        mvc.perform(get("/dashboard/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void admin_can_access_admin_dashboard() throws Exception {
        mvc.perform(get("/dashboard/admin")).andExpect(status().isOk());
    }
}


