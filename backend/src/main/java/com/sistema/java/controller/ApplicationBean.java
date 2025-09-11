package com.sistema.java.controller;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bean de aplicação para configurações globais do JSF
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Named("applicationBean")
@ApplicationScoped
@Component
public class ApplicationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String applicationName;
    private String version;
    private String buildDate;
    private String environment;
    
    @PostConstruct
    public void init() {
        this.applicationName = "Sistema Java";
        this.version = "1.0.0";
        this.buildDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        this.environment = "Development";
    }
    
    /**
     * Retorna o nome da aplicação
     */
    public String getApplicationName() {
        return applicationName;
    }
    
    /**
     * Retorna a versão da aplicação
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Retorna a data de build
     */
    public String getBuildDate() {
        return buildDate;
    }
    
    /**
     * Retorna o ambiente atual
     */
    public String getEnvironment() {
        return environment;
    }
    
    /**
     * Retorna informações completas da aplicação
     */
    public String getFullApplicationInfo() {
        return String.format("%s v%s - Build: %s (%s)", 
            applicationName, version, buildDate, environment);
    }
    
    /**
     * Retorna o ano atual para copyright
     */
    public int getCurrentYear() {
        return LocalDateTime.now().getYear();
    }
    
    /**
     * Retorna a data/hora atual formatada
     */
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}