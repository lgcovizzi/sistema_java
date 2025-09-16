package com.sistema.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Configuração do Redis embarcado para desenvolvimento local.
 * Esta configuração é ativada apenas quando o profile 'local' está ativo.
 */
@Configuration
@Profile("local")
public class EmbeddedRedisConfig {

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    /**
     * Inicia o servidor Redis embarcado após a construção do bean.
     */
    @PostConstruct
    public void startRedis() throws IOException {
        try {
            redisServer = new RedisServer(redisPort);
            redisServer.start();
            System.out.println("Redis embarcado iniciado na porta: " + redisPort);
        } catch (Exception e) {
            System.err.println("Erro ao iniciar Redis embarcado: " + e.getMessage());
            // Se não conseguir iniciar na porta padrão, tenta uma porta alternativa
            try {
                redisPort = 6380;
                redisServer = new RedisServer(redisPort);
                redisServer.start();
                System.out.println("Redis embarcado iniciado na porta alternativa: " + redisPort);
            } catch (Exception ex) {
                System.err.println("Não foi possível iniciar Redis embarcado: " + ex.getMessage());
            }
        }
    }

    /**
     * Para o servidor Redis embarcado antes da destruição do bean.
     */
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            System.out.println("Redis embarcado parado");
        }
    }
}