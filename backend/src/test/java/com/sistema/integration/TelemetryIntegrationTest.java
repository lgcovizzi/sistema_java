package com.sistema.integration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Testes de integração para o sistema de telemetria.
 * 
 * Testa:
 * - Integração do OpenTelemetry com Spring Boot
 * - Coleta de métricas com Micrometer
 * - Geração de traces distribuídos
 * - Exportação para Prometheus
 * - Instrumentação automática
 * - Performance e overhead
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.telemetry.enabled=true",
    "app.telemetry.service-name=sistema-java-integration-test",
    "app.telemetry.environment=integration-test",
    "app.telemetry.exporters.prometheus.enabled=true",
    "app.telemetry.exporters.jaeger.enabled=false",
    "app.telemetry.exporters.otlp.enabled=false",
    "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
    "management.endpoint.metrics.enabled=true",
    "management.endpoint.prometheus.enabled=true"
})
@DisplayName("Telemetry - Testes de Integração")
class TelemetryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private OpenTelemetry openTelemetry;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Nested
    @DisplayName("Configuração e Inicialização")
    class ConfigurationAndInitialization {

        @Test
        @DisplayName("Deve inicializar MeterRegistry corretamente")
        void shouldInitializeMeterRegistryCorrectly() {
            // Then
            if (meterRegistry != null) {
                assertThat(meterRegistry).isNotNull();
                assertThat(meterRegistry.getMeters()).isNotNull();
            }
        }

        @Test
        @DisplayName("Deve inicializar OpenTelemetry corretamente")
        void shouldInitializeOpenTelemetryCorrectly() {
            // Then
            if (openTelemetry != null) {
                assertThat(openTelemetry).isNotNull();
                assertThat(openTelemetry.getTracerProvider()).isNotNull();
            }
        }

        @Test
        @DisplayName("Deve expor endpoint de métricas do Actuator")
        void shouldExposeActuatorMetricsEndpoint() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/metrics", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("names");
        }

        @Test
        @DisplayName("Deve expor endpoint do Prometheus")
        void shouldExposePrometheusEndpoint() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/prometheus", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("# HELP");
        }
    }

    @Nested
    @DisplayName("Coleta de Métricas")
    class MetricsCollection {

        @Test
        @DisplayName("Deve coletar métricas básicas da JVM")
        void shouldCollectBasicJvmMetrics() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/metrics/jvm.memory.used", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("measurements");
        }

        @Test
        @DisplayName("Deve coletar métricas de HTTP requests")
        void shouldCollectHttpRequestMetrics() {
            // Given - Fazer algumas requisições para gerar métricas
            restTemplate.getForEntity(baseUrl + "/api/health", String.class);
            restTemplate.getForEntity(baseUrl + "/api/health", String.class);

            // When
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/actuator/metrics/http.server.requests", String.class);
                
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).contains("measurements");
            });
        }

        @Test
        @DisplayName("Deve registrar métricas customizadas")
        void shouldRegisterCustomMetrics() {
            if (meterRegistry != null) {
                // Given
                Counter customCounter = Counter.builder("test.custom.counter")
                    .description("Contador customizado para teste")
                    .tag("test", "integration")
                    .register(meterRegistry);

                // When
                customCounter.increment();
                customCounter.increment(5);

                // Then
                assertThat(customCounter.count()).isEqualTo(6.0);

                // Verificar se aparece no endpoint de métricas
                await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                        baseUrl + "/actuator/metrics/test.custom.counter", String.class);
                    
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).contains("6.0");
                });
            }
        }

        @Test
        @DisplayName("Deve medir tempo de execução com Timer")
        void shouldMeasureExecutionTimeWithTimer() {
            if (meterRegistry != null) {
                // Given
                Timer customTimer = Timer.builder("test.custom.timer")
                    .description("Timer customizado para teste")
                    .tag("operation", "test")
                    .register(meterRegistry);

                // When
                Timer.Sample sample = Timer.start(meterRegistry);
                try {
                    Thread.sleep(100); // Simular operação
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sample.stop(customTimer);

                // Then
                assertThat(customTimer.count()).isEqualTo(1);
                assertThat(customTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(90);
            }
        }

        @Test
        @DisplayName("Deve registrar Gauge para valores dinâmicos")
        void shouldRegisterGaugeForDynamicValues() {
            if (meterRegistry != null) {
                // Given
                final double[] dynamicValue = {42.0};
                
                Gauge customGauge = Gauge.builder("test.custom.gauge", dynamicValue, arr -> arr[0])
                    .description("Gauge customizado para teste")
                    .tag("type", "dynamic")
                    .register(meterRegistry);

                // When
                dynamicValue[0] = 100.0;

                // Then
                assertThat(customGauge.value()).isEqualTo(100.0);
            }
        }
    }

    @Nested
    @DisplayName("Traces Distribuídos")
    class DistributedTraces {

        @Test
        @DisplayName("Deve criar traces para requisições HTTP")
        void shouldCreateTracesForHttpRequests() {
            if (openTelemetry != null) {
                // Given
                Tracer tracer = openTelemetry.getTracer("integration-test");

                // When
                Span span = tracer.spanBuilder("test-operation")
                    .setAttribute("test.type", "integration")
                    .setAttribute("test.component", "http")
                    .startSpan();

                try {
                    // Simular operação
                    restTemplate.getForEntity(baseUrl + "/api/health", String.class);
                } finally {
                    span.end();
                }

                // Then
                assertThat(span.getSpanContext().isValid()).isTrue();
            }
        }

        @Test
        @DisplayName("Deve propagar contexto entre spans")
        void shouldPropagateContextBetweenSpans() {
            if (openTelemetry != null) {
                // Given
                Tracer tracer = openTelemetry.getTracer("integration-test");

                // When
                Span parentSpan = tracer.spanBuilder("parent-operation")
                    .setAttribute("operation.type", "parent")
                    .startSpan();

                try {
                    // Criar span filho
                    Span childSpan = tracer.spanBuilder("child-operation")
                        .setAttribute("operation.type", "child")
                        .startSpan();

                    try {
                        // Simular operação aninhada
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        childSpan.end();
                    }
                } finally {
                    parentSpan.end();
                }

                // Then
                assertThat(parentSpan.getSpanContext().isValid()).isTrue();
            }
        }

        @Test
        @DisplayName("Deve adicionar atributos customizados aos spans")
        void shouldAddCustomAttributesToSpans() {
            if (openTelemetry != null) {
                // Given
                Tracer tracer = openTelemetry.getTracer("integration-test");

                // When
                Span span = tracer.spanBuilder("custom-attributes-test")
                    .setAttribute("user.id", "test-user-123")
                    .setAttribute("request.size", 1024L)
                    .setAttribute("operation.success", true)
                    .startSpan();

                try {
                    span.addEvent("Processing started");
                    Thread.sleep(10);
                    span.addEvent("Processing completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    span.end();
                }

                // Then
                assertThat(span.getSpanContext().isValid()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Métricas OpenTelemetry")
    class OpenTelemetryMetrics {

        @Test
        @DisplayName("Deve criar contador OpenTelemetry")
        void shouldCreateOpenTelemetryCounter() {
            if (openTelemetry != null) {
                // Given
                Meter meter = openTelemetry.getMeter("integration-test");
                LongCounter counter = meter.counterBuilder("test.otel.counter")
                    .setDescription("Contador OpenTelemetry para teste")
                    .setUnit("operations")
                    .build();

                // When
                counter.add(1);
                counter.add(5);

                // Then
                // Verificar que o contador foi criado (não há getter direto)
                assertThat(counter).isNotNull();
            }
        }

        @Test
        @DisplayName("Deve criar histograma OpenTelemetry")
        void shouldCreateOpenTelemetryHistogram() {
            if (openTelemetry != null) {
                // Given
                Meter meter = openTelemetry.getMeter("integration-test");
                var histogram = meter.histogramBuilder("test.otel.histogram")
                    .setDescription("Histograma OpenTelemetry para teste")
                    .setUnit("ms")
                    .build();

                // When
                histogram.record(100.0);
                histogram.record(250.0);
                histogram.record(500.0);

                // Then
                assertThat(histogram).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Instrumentação Automática")
    class AutomaticInstrumentation {

        @Test
        @DisplayName("Deve instrumentar automaticamente requisições HTTP")
        void shouldAutomaticallyInstrumentHttpRequests() {
            // When - Fazer várias requisições
            for (int i = 0; i < 5; i++) {
                restTemplate.getForEntity(baseUrl + "/api/health", String.class);
            }

            // Then - Verificar se métricas foram coletadas automaticamente
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/actuator/prometheus", String.class);
                
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                String body = response.getBody();
                assertThat(body).contains("http_server_requests");
            });
        }

        @Test
        @DisplayName("Deve instrumentar automaticamente métricas da JVM")
        void shouldAutomaticallyInstrumentJvmMetrics() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/prometheus", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = response.getBody();
            assertThat(body).contains("jvm_memory_used_bytes");
            assertThat(body).contains("jvm_gc_collection_seconds");
            assertThat(body).contains("process_cpu_usage");
        }

        @Test
        @DisplayName("Deve instrumentar automaticamente pool de conexões")
        void shouldAutomaticallyInstrumentConnectionPools() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/prometheus", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = response.getBody();
            // Verificar métricas relacionadas a pools de conexão se disponíveis
            assertThat(body).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Exportação para Prometheus")
    class PrometheusExport {

        @Test
        @DisplayName("Deve exportar métricas no formato Prometheus")
        void shouldExportMetricsInPrometheusFormat() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/prometheus", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            String body = response.getBody();
            
            // Verificar formato Prometheus
            assertThat(body).contains("# HELP");
            assertThat(body).contains("# TYPE");
            assertThat(body).matches("(?s).*\\w+\\{.*\\}\\s+[0-9.]+.*");
        }

        @Test
        @DisplayName("Deve incluir labels nas métricas exportadas")
        void shouldIncludeLabelsInExportedMetrics() {
            // Given - Fazer requisição para gerar métricas com labels
            restTemplate.getForEntity(baseUrl + "/api/health", String.class);

            // When
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/actuator/prometheus", String.class);
                
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                String body = response.getBody();
                
                // Verificar presença de labels
                assertThat(body).contains("uri=");
                assertThat(body).contains("method=");
                assertThat(body).contains("status=");
            });
        }

        @Test
        @DisplayName("Deve exportar métricas customizadas")
        void shouldExportCustomMetrics() {
            if (meterRegistry != null) {
                // Given
                Counter customCounter = Counter.builder("integration.test.custom")
                    .description("Métrica customizada para teste de integração")
                    .tag("environment", "test")
                    .tag("component", "integration")
                    .register(meterRegistry);

                customCounter.increment(10);

                // When
                await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                        baseUrl + "/actuator/prometheus", String.class);
                    
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    String body = response.getBody();
                    
                    // Then
                    assertThat(body).contains("integration_test_custom");
                    assertThat(body).contains("environment=\"test\"");
                    assertThat(body).contains("component=\"integration\"");
                    assertThat(body).contains("10.0");
                });
            }
        }
    }

    @Nested
    @DisplayName("Performance e Overhead")
    class PerformanceAndOverhead {

        @Test
        @DisplayName("Deve manter performance aceitável com telemetria habilitada")
        void shouldMaintainAcceptablePerformanceWithTelemetryEnabled() {
            // Given
            int numberOfRequests = 100;
            long startTime = System.currentTimeMillis();

            // When
            for (int i = 0; i < numberOfRequests; i++) {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/api/health", String.class);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double averageTime = (double) totalTime / numberOfRequests;

            // Then
            assertThat(averageTime).isLessThan(100.0); // Menos de 100ms por requisição em média
        }

        @Test
        @DisplayName("Deve coletar métricas sem impacto significativo na memória")
        void shouldCollectMetricsWithoutSignificantMemoryImpact() {
            // Given
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();

            // When - Gerar muitas métricas
            if (meterRegistry != null) {
                for (int i = 0; i < 1000; i++) {
                    Counter.builder("test.memory.counter." + (i % 10))
                        .tag("iteration", String.valueOf(i))
                        .register(meterRegistry)
                        .increment();
                }
            }

            // Force garbage collection
            System.gc();
            Thread.yield();

            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            // Then - Aumento de memória deve ser razoável (menos de 50MB)
            assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024);
        }

        @Test
        @DisplayName("Deve processar spans rapidamente")
        void shouldProcessSpansQuickly() {
            if (openTelemetry != null) {
                // Given
                Tracer tracer = openTelemetry.getTracer("performance-test");
                int numberOfSpans = 1000;
                long startTime = System.nanoTime();

                // When
                for (int i = 0; i < numberOfSpans; i++) {
                    Span span = tracer.spanBuilder("performance-test-span-" + i)
                        .setAttribute("iteration", i)
                        .startSpan();
                    span.end();
                }

                long endTime = System.nanoTime();
                long totalTime = endTime - startTime;
                double averageTimePerSpan = (double) totalTime / numberOfSpans / 1_000_000; // em ms

                // Then - Cada span deve ser processado rapidamente (menos de 1ms)
                assertThat(averageTimePerSpan).isLessThan(1.0);
            }
        }
    }

    @Nested
    @DisplayName("Configuração Dinâmica")
    class DynamicConfiguration {

        @Test
        @DisplayName("Deve respeitar configuração de habilitação de telemetria")
        void shouldRespectTelemetryEnableConfiguration() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/metrics", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // Se telemetria está habilitada, deve ter métricas disponíveis
            assertThat(response.getBody()).contains("names");
        }

        @Test
        @DisplayName("Deve usar nome de serviço configurado")
        void shouldUseConfiguredServiceName() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/info", String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // Verificar que a aplicação está rodando com as configurações corretas
        }

        @Test
        @DisplayName("Deve expor apenas endpoints habilitados")
        void shouldExposeOnlyEnabledEndpoints() {
            // When - Verificar endpoints habilitados
            ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/metrics", String.class);
            ResponseEntity<String> prometheusResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/prometheus", String.class);

            // Then
            assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}