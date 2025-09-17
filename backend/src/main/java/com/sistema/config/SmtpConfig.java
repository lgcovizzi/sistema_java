package com.sistema.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuração SMTP separada para desacoplamento do serviço de email.
 * Permite configuração flexível de diferentes provedores SMTP.
 */
@Configuration
public class SmtpConfig {

    @Value("${spring.mail.host:localhost}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean sslEnable;

    @Value("${spring.mail.properties.mail.transport.protocol:smtp}")
    private String protocol;

    @Value("${spring.mail.default-encoding:UTF-8}")
    private String defaultEncoding;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:5000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:5000}")
    private int timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:5000}")
    private int writeTimeout;

    /**
     * Configura o JavaMailSender com as propriedades SMTP.
     * 
     * @return JavaMailSender configurado
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Configurações básicas
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding(defaultEncoding);
        
        // Propriedades SMTP
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.ssl.enable", sslEnable);
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", writeTimeout);
        
        // Configurações adicionais para diferentes provedores
        if (host.contains("gmail")) {
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        } else if (host.contains("outlook") || host.contains("hotmail")) {
            props.put("mail.smtp.ssl.trust", "smtp-mail.outlook.com");
        } else if (host.contains("mailtrap")) {
            props.put("mail.smtp.ssl.trust", host);
        }
        
        return mailSender;
    }

    /**
     * Configuração SMTP para diferentes ambientes.
     * 
     * @return SmtpConfiguration com as configurações atuais
     */
    @Bean
    public SmtpConfiguration smtpConfiguration() {
        return SmtpConfiguration.builder()
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .auth(auth)
                .starttlsEnable(starttlsEnable)
                .sslEnable(sslEnable)
                .protocol(protocol)
                .defaultEncoding(defaultEncoding)
                .connectionTimeout(connectionTimeout)
                .timeout(timeout)
                .writeTimeout(writeTimeout)
                .build();
    }

    /**
     * Classe de configuração SMTP imutável.
     */
    public static class SmtpConfiguration {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final boolean auth;
        private final boolean starttlsEnable;
        private final boolean sslEnable;
        private final String protocol;
        private final String defaultEncoding;
        private final int connectionTimeout;
        private final int timeout;
        private final int writeTimeout;

        private SmtpConfiguration(Builder builder) {
            this.host = builder.host;
            this.port = builder.port;
            this.username = builder.username;
            this.password = builder.password;
            this.auth = builder.auth;
            this.starttlsEnable = builder.starttlsEnable;
            this.sslEnable = builder.sslEnable;
            this.protocol = builder.protocol;
            this.defaultEncoding = builder.defaultEncoding;
            this.connectionTimeout = builder.connectionTimeout;
            this.timeout = builder.timeout;
            this.writeTimeout = builder.writeTimeout;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public boolean isAuth() { return auth; }
        public boolean isStarttlsEnable() { return starttlsEnable; }
        public boolean isSslEnable() { return sslEnable; }
        public String getProtocol() { return protocol; }
        public String getDefaultEncoding() { return defaultEncoding; }
        public int getConnectionTimeout() { return connectionTimeout; }
        public int getTimeout() { return timeout; }
        public int getWriteTimeout() { return writeTimeout; }

        public static class Builder {
            private String host;
            private int port;
            private String username;
            private String password;
            private boolean auth;
            private boolean starttlsEnable;
            private boolean sslEnable;
            private String protocol;
            private String defaultEncoding;
            private int connectionTimeout;
            private int timeout;
            private int writeTimeout;

            public Builder host(String host) { this.host = host; return this; }
            public Builder port(int port) { this.port = port; return this; }
            public Builder username(String username) { this.username = username; return this; }
            public Builder password(String password) { this.password = password; return this; }
            public Builder auth(boolean auth) { this.auth = auth; return this; }
            public Builder starttlsEnable(boolean starttlsEnable) { this.starttlsEnable = starttlsEnable; return this; }
            public Builder sslEnable(boolean sslEnable) { this.sslEnable = sslEnable; return this; }
            public Builder protocol(String protocol) { this.protocol = protocol; return this; }
            public Builder defaultEncoding(String defaultEncoding) { this.defaultEncoding = defaultEncoding; return this; }
            public Builder connectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; return this; }
            public Builder timeout(int timeout) { this.timeout = timeout; return this; }
            public Builder writeTimeout(int writeTimeout) { this.writeTimeout = writeTimeout; return this; }

            public SmtpConfiguration build() {
                return new SmtpConfiguration(this);
            }
        }
    }
}