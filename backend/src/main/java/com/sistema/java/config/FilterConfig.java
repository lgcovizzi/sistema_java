package com.sistema.java.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de filtros da aplicação
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Configuration
public class FilterConfig {

    @Autowired
    private SessionFilter sessionFilter;

    /**
     * Registra o filtro de sessão
     */
    @Bean
    public FilterRegistrationBean<SessionFilter> sessionFilterRegistration() {
        FilterRegistrationBean<SessionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(sessionFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("sessionFilter");
        return registration;
    }
}