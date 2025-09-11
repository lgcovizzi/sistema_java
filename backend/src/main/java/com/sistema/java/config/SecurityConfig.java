package com.sistema.java.config;

import com.sistema.java.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de segurança do Spring Security
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Login e Registro - project_rules.md
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                         JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    /**
     * Configuração da cadeia de filtros de segurança
     * Referência: Controle de Acesso - project_rules.md
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                // Endpoints públicos - acessíveis para CONVIDADO
                .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()
                .requestMatchers("/api/noticias/publicas").permitAll()
                .requestMatchers("/api/email/mailhog-info").permitAll()
                
                // Recursos estáticos
                .requestMatchers("/resources/**", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/javax.faces.resource/**").permitAll()
                
                // Páginas JSF públicas
                .requestMatchers("/", "/index.xhtml", "/pages/publico/**").permitAll()
                
                // Health check e actuator
                .requestMatchers("/actuator/health").permitAll()
                
                // Endpoints administrativos - apenas ADMINISTRADOR
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/email/teste", "/api/email/status", "/api/email/estatisticas", "/api/email/enviar-simples").hasRole("ADMINISTRADOR")
                .requestMatchers("/pages/admin/**").hasRole("ADMINISTRADOR")
                
                // Endpoints de fundador - FUNDADOR e ADMINISTRADOR
                .requestMatchers("/api/fundador/**").hasAnyRole("FUNDADOR", "ADMINISTRADOR")
                .requestMatchers("/pages/fundador/**").hasAnyRole("FUNDADOR", "ADMINISTRADOR")
                
                // Endpoints de colaborador - COLABORADOR, FUNDADOR e ADMINISTRADOR
                .requestMatchers("/api/colaborador/**").hasAnyRole("COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                .requestMatchers("/pages/colaborador/**").hasAnyRole("COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                
                // Endpoints de parceiro - PARCEIRO e superiores
                .requestMatchers("/api/parceiro/**").hasAnyRole("PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                .requestMatchers("/pages/parceiro/**").hasAnyRole("PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                
                // Endpoints de associado - ASSOCIADO e superiores
                .requestMatchers("/api/associado/**").hasAnyRole("ASSOCIADO", "PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                .requestMatchers("/pages/associado/**").hasAnyRole("ASSOCIADO", "PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                
                // Endpoints de autenticação que requerem token válido
                .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/validar-token").hasAnyRole("USUARIO", "ASSOCIADO", "PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                
                // Dashboard e perfil - todos os usuários autenticados (exceto CONVIDADO)
                .requestMatchers("/api/dashboard/**", "/api/perfil/**").hasAnyRole("USUARIO", "ASSOCIADO", "PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                .requestMatchers("/pages/dashboard/**", "/pages/perfil/**").hasAnyRole("USUARIO", "ASSOCIADO", "PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                
                // Avatar upload - todos os usuários autenticados
                .requestMatchers("/api/avatar/**").hasAnyRole("USUARIO", "ASSOCIADO", "PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                
                // Comentários - todos os usuários autenticados
                .requestMatchers("/api/comentarios/**").hasAnyRole("USUARIO", "ASSOCIADO", "PARCEIRO", "COLABORADOR", "FUNDADOR", "ADMINISTRADOR")
                
                // Qualquer outra requisição requer autenticação
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Adicionar filtro JWT antes do filtro de autenticação padrão
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuração CORS para desenvolvimento
     * Referência: Configurações de Ambiente - project_rules.md
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Encoder de senha usando BCrypt
     * Referência: Login e Registro - project_rules.md
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Provider de autenticação DAO
     * Referência: Controle de Acesso - project_rules.md
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Gerenciador de autenticação
     * Referência: Controle de Acesso - project_rules.md
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}