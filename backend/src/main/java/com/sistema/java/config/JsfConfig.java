package com.sistema.java.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;

import jakarta.faces.webapp.FacesServlet;
import jakarta.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do JSF para integração com Spring Boot
 * 
 * @author Sistema Java
 * @version 1.0
 */
// Temporariamente desabilitado para executar apenas como API REST
// @Configuration
public class JsfConfig implements ServletContextAware {

    private ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        
        // Configurações do JSF
        servletContext.setInitParameter("javax.faces.PROJECT_STAGE", "Development");
        servletContext.setInitParameter("javax.faces.FACELETS_REFRESH_PERIOD", "0");
        servletContext.setInitParameter("javax.faces.FACELETS_ENCODING", "UTF-8");
        servletContext.setInitParameter("javax.faces.VALIDATE_EMPTY_FIELDS", "true");
        servletContext.setInitParameter("javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL", "true");
        
        // Configurações do PrimeFaces
        servletContext.setInitParameter("primefaces.THEME", "bootstrap");
        servletContext.setInitParameter("primefaces.UPLOADER", "auto");
        servletContext.setInitParameter("primefaces.CLIENT_SIDE_VALIDATION", "true");
        servletContext.setInitParameter("primefaces.FONT_AWESOME", "true");
        servletContext.setInitParameter("primefaces.MOVE_SCRIPTS_TO_BOTTOM", "true");
    }

    /**
     * Registra o FacesServlet no Spring Boot
     */
    // @Bean
    public ServletRegistrationBean<FacesServlet> facesServletRegistration() {
        ServletRegistrationBean<FacesServlet> registration = new ServletRegistrationBean<>(
            new FacesServlet(), "*.xhtml");
        registration.setLoadOnStartup(1);
        registration.setName("FacesServlet");
        
        Map<String, String> initParameters = new HashMap<>();
        initParameters.put("javax.faces.FACELETS_SUFFIX", ".xhtml");
        initParameters.put("javax.faces.DEFAULT_SUFFIX", ".xhtml");
        registration.setInitParameters(initParameters);
        
        return registration;
    }
}