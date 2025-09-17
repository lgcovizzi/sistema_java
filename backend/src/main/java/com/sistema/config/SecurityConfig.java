package com.sistema.config;

import com.sistema.security.JwtAuthenticationFilter;
import com.sistema.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
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
 * Configuração de segurança do Spring Security com autenticação JWT.
 * Define políticas de acesso, filtros de autenticação e configurações CORS.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    @Lazy
    private AuthService authService;

    /**
     * Configura a cadeia de filtros de segurança.
     *
     * @param http Configuração de segurança HTTP
     * @return SecurityFilterChain configurado
     * @throws Exception em caso de erro na configuração
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desabilita CSRF para APIs REST
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configura CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configura autorização de requisições
            .authorizeHttpRequests(authz -> authz
                // Endpoints públicos - não requerem autenticação
                .requestMatchers(
                    "/",
                    "/api-simple",
                    "/api/health",
                    "/api/info",
                    "/api/redis-test",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/validate-token",
                    "/api/auth/verify-email",
                    "/api/auth/resend-verification",
                    "/api/email/**",
                    "/actuator/**",
                    "/error",
                    "/login",
                    "/api/login",
                    "/logout",
                    "/dashboard",
                    "/news",
                    "/h2-console/**"
                ).permitAll()
                
                // Endpoints administrativos - requerem role ADMIN
                .requestMatchers(
                    "/api/auth/users",
                    "/api/auth/users/*/status",
                    "/api/auth/stats"
                ).hasRole("ADMIN")
                
                // Endpoints de usuário autenticado
                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/change-password",
                    "/api/auth/refresh",
                    "/api/auth/logout"
                ).authenticated()
                
                // Todas as outras requisições requerem autenticação
                .anyRequest().authenticated()
            )
            
            // Configura política de sessão como stateless (JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configura provider de autenticação
            .authenticationProvider(authenticationProvider())
            
            // Adiciona filtro JWT antes do filtro de autenticação padrão
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configuração de headers para H2 console
            .headers(headers -> headers
                .frameOptions().sameOrigin() // Permite frames da mesma origem para H2 console
            );

        return http.build();
    }

    /**
     * Configura o provider de autenticação DAO.
     *
     * @return AuthenticationProvider configurado
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configura o serviço de detalhes do usuário.
     *
     * @return UserDetailsService
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return authService;
    }

    /**
     * Configura o encoder de senhas.
     *
     * @return PasswordEncoder BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configura o gerenciador de autenticação.
     *
     * @param config Configuração de autenticação
     * @return AuthenticationManager
     * @throws Exception em caso de erro na configuração
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configura CORS para permitir requisições de diferentes origens.
     *
     * @return CorsConfigurationSource configurado
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permite origens específicas (configurar conforme necessário)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*"
        ));
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Cabeçalhos permitidos
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Cabeçalhos expostos
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition"
        ));
        
        // Permite credenciais
        configuration.setAllowCredentials(true);
        
        // Tempo de cache para requisições preflight
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Configuração adicional para desenvolvimento.
     * Remove em produção ou configure adequadamente.
     */
    public static class DevSecurityConfig {
        
        /**
         * Configuração mais permissiva para desenvolvimento.
         * ATENÇÃO: Não usar em produção!
         */
        public static void configureForDevelopment(HttpSecurity http) throws Exception {
            http.headers(headers -> headers
                .frameOptions().sameOrigin() // Permite iframes da mesma origem
                .contentTypeOptions().disable() // Desabilita proteção de tipo de conteúdo
            );
        }
    }

    /**
     * Configuração para produção com segurança reforçada.
     */
    public static class ProductionSecurityConfig {
        
        /**
         * Configuração de segurança para ambiente de produção.
         */
        public static void configureForProduction(HttpSecurity http) throws Exception {
            http.headers(headers -> headers
                .frameOptions().deny() // Nega todos os iframes
                .contentTypeOptions().and() // Habilita proteção de tipo de conteúdo
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000) // HSTS por 1 ano
                    .includeSubDomains(true)
                )
            );
        }
    }
}