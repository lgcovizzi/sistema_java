package com.sistema.config;

/*
 * CLASSE COMENTADA: EmbeddedRedisConfig
 * 
 * Esta classe foi comentada porque depende da biblioteca redis.embedded
 * que foi removida do projeto para evitar conflitos de porta.
 * 
 * O projeto agora usa Redis externo rodando na porta 6379.
 * Para desenvolvimento local, inicie o Redis manualmente:
 * - redis-server --daemonize yes
 * 
 * Caso queira reativar o Redis embarcado no futuro, descomente esta classe
 * e adicione a dependência no pom.xml:
 * 
 * <dependency>
 *     <groupId>it.ozimov</groupId>
 *     <artifactId>embedded-redis</artifactId>
 *     <version>0.7.3</version>
 * </dependency>
 */

/*
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Configuration
@Profile("local")
public class EmbeddedRedisConfig {

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        try {
            redisServer = new RedisServer(redisPort);
            redisServer.start();
            System.out.println("Redis embarcado iniciado na porta: " + redisPort);
        } catch (Exception e) {
            System.err.println("Erro ao iniciar Redis embarcado: " + e.getMessage());
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

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            System.out.println("Redis embarcado parado");
        }
    }
}
*/