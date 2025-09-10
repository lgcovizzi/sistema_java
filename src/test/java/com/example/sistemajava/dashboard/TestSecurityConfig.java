package com.example.sistemajava.dashboard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/dashboard/user").hasRole("USER")
                        .requestMatchers("/dashboard/associado").hasRole("ASSOCIADO")
                        .requestMatchers("/dashboard/colaborador").hasRole("COLABORADOR")
                        .requestMatchers("/dashboard/parceiro").hasRole("PARCEIRO")
                        .requestMatchers("/dashboard/fundador").hasRole("FUNDADOR")
                        .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}


