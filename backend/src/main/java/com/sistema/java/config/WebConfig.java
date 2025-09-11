package com.sistema.java.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração Web do Spring MVC
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityInterceptor securityInterceptor;

    /**
     * Registra interceptadores
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/resources/**",
                    "/javax.faces.resource/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/uploads/**",
                    "/favicon.ico",
                    "/error"
                );
    }

    /**
     * Configura handlers para recursos estáticos
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Recursos estáticos do JSF
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
        
        // Recursos CSS
        registry.addResourceHandler("/css/**")
                .addResourceLocations("/css/");
        
        // Recursos JavaScript
        registry.addResourceHandler("/js/**")
                .addResourceLocations("/js/");
        
        // Imagens
        registry.addResourceHandler("/images/**")
                .addResourceLocations("/images/");
        
        // Uploads (avatares e imagens de notícias)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
        
        // Favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("/favicon.ico");
    }
}