package com.sistema.config;

// import com.sistema.telemetry.interceptor.HttpTelemetryInterceptor;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração para recursos estáticos, mapeamentos web e interceptadores de telemetria.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // private final HttpTelemetryInterceptor httpTelemetryInterceptor;

    // @Autowired
    // public WebConfig(HttpTelemetryInterceptor httpTelemetryInterceptor) {
    //     this.httpTelemetryInterceptor = httpTelemetryInterceptor;
    // }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configuração para servir arquivos estáticos
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // Cache por 1 hora
        
        // Configuração específica para favicon.ico
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico")
                .setCachePeriod(86400); // Cache por 24 horas
        
        // Configuração para outros recursos comuns
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);
    }

    // @Override
    // public void addInterceptors(InterceptorRegistry registry) {
    //     // Registrar interceptador de telemetria para todas as requisições
    //     registry.addInterceptor(httpTelemetryInterceptor)
    //             .addPathPatterns("/**")
    //             .excludePathPatterns(
    //                 "/static/**",
    //                 "/css/**", 
    //                 "/js/**", 
    //                 "/images/**",
    //                 "/favicon.ico",
    //                 "/actuator/health",
    //                 "/actuator/prometheus"
    //             );
    // }
}