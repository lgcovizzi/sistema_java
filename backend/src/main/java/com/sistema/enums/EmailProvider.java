package com.sistema.enums;

/**
 * Enum que define os provedores de email SMTP suportados pelo sistema.
 * Cada provedor possui configurações pré-definidas para facilitar a configuração.
 */
public enum EmailProvider {
    
    MAILTRAP("Mailtrap", "Mailtrap Development", "smtp.mailtrap.io", 587, true, true),
    GMAIL("Gmail", "Google Gmail", "smtp.gmail.com", 587, true, true),
    OUTLOOK("Outlook", "Microsoft Outlook", "smtp-mail.outlook.com", 587, true, true),
    YAHOO("Yahoo", "Yahoo Mail", "smtp.mail.yahoo.com", 587, true, true),
    CUSTOM("Custom", "Configuração Personalizada", "", 587, true, true);
    
    private final String name;
    private final String description;
    private final String defaultHost;
    private final int defaultPort;
    private final boolean defaultTlsEnabled;
    private final boolean defaultAuthRequired;
    
    EmailProvider(String name, String description, String defaultHost, int defaultPort, 
                  boolean defaultTlsEnabled, boolean defaultAuthRequired) {
        this.name = name;
        this.description = description;
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
        this.defaultTlsEnabled = defaultTlsEnabled;
        this.defaultAuthRequired = defaultAuthRequired;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getDefaultHost() {
        return defaultHost;
    }
    
    public int getDefaultPort() {
        return defaultPort;
    }
    
    public boolean isDefaultTlsEnabled() {
        return defaultTlsEnabled;
    }
    
    public boolean isDefaultAuthRequired() {
        return defaultAuthRequired;
    }
    
    /**
     * Converte uma string para o enum EmailProvider correspondente.
     * @param name Nome do provedor
     * @return EmailProvider correspondente
     * @throws IllegalArgumentException se o provedor não for encontrado
     */
    public static EmailProvider fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do provedor não pode ser nulo ou vazio");
        }
        
        for (EmailProvider provider : values()) {
            if (provider.name.equalsIgnoreCase(name.trim())) {
                return provider;
            }
        }
        
        throw new IllegalArgumentException("Provedor de email não encontrado: " + name);
    }
    
    /**
     * Verifica se o provedor é personalizado (CUSTOM).
     * @return true se for CUSTOM, false caso contrário
     */
    public boolean isCustom() {
        return this == CUSTOM;
    }
    
    /**
     * Verifica se o provedor é o Gmail.
     * @return true se for GMAIL, false caso contrário
     */
    public boolean isGmail() {
        return this == GMAIL;
    }
    
    /**
     * Verifica se o provedor é o Mailtrap.
     * @return true se for MAILTRAP, false caso contrário
     */
    public boolean isMailtrap() {
        return this == MAILTRAP;
    }
    
    /**
     * Retorna o nome de exibição do provedor.
     * @return nome de exibição
     */
    public String getDisplayName() {
        return this.name;
    }
    
    /**
     * Retorna as configurações padrão do provedor.
     * @return mapa com configurações padrão
     */
    public java.util.Map<String, Object> getDefaultConfiguration() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("name", this.name);
        config.put("description", this.description);
        config.put("host", this.defaultHost);
        config.put("port", this.defaultPort);
        config.put("tlsEnabled", this.defaultTlsEnabled);
        config.put("authRequired", this.defaultAuthRequired);
        return config;
    }
}