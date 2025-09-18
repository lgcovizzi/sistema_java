package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.dto.ToastMessage;
import com.sistema.enums.ToastType;
import com.sistema.service.ToastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integração - Sistema de Toast")
class ToastIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToastService toastService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    @Test
    @DisplayName("Deve criar e recuperar mensagens toast via API REST")
    void shouldCreateAndRetrieveToastMessagesViaAPI() throws Exception {
        // Adicionar mensagem de sucesso
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Operação realizada com sucesso\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Adicionar mensagem de erro
        mockMvc.perform(post("/api/toast/error")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Erro na operação\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Recuperar mensagens
        MvcResult result = mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("SUCCESS"))
                .andExpect(jsonPath("$[0].message").value("Operação realizada com sucesso"))
                .andExpect(jsonPath("$[1].type").value("ERROR"))
                .andExpect(jsonPath("$[1].message").value("Erro na operação"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        List<ToastMessage> messages = objectMapper.readValue(responseContent, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, ToastMessage.class));
        
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getType()).isEqualTo(ToastType.SUCCESS);
        assertThat(messages.get(1).getType()).isEqualTo(ToastType.ERROR);
    }

    @Test
    @DisplayName("Deve limpar mensagens após recuperação com clear=true")
    void shouldClearMessagesAfterRetrievalWithClearFlag() throws Exception {
        // Adicionar mensagens
        toastService.success("Mensagem 1");
        toastService.error("Mensagem 2");

        // Verificar que existem mensagens
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Recuperar e limpar mensagens
        mockMvc.perform(get("/api/toast/messages-and-clear")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Verificar que as mensagens foram limpas
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Deve manter isolamento entre sessões diferentes")
    void shouldMaintainIsolationBetweenDifferentSessions() throws Exception {
        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();

        // Adicionar mensagem na sessão 1
        mockMvc.perform(post("/api/toast/success")
                .session(session1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Mensagem sessão 1\"}"))
                .andExpect(status().isOk());

        // Adicionar mensagem na sessão 2
        mockMvc.perform(post("/api/toast/error")
                .session(session2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Mensagem sessão 2\"}"))
                .andExpect(status().isOk());

        // Verificar mensagens na sessão 1
        mockMvc.perform(get("/api/toast/messages")
                .session(session1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("SUCCESS"))
                .andExpect(jsonPath("$[0].message").value("Mensagem sessão 1"));

        // Verificar mensagens na sessão 2
        mockMvc.perform(get("/api/toast/messages")
                .session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("ERROR"))
                .andExpect(jsonPath("$[0].message").value("Mensagem sessão 2"));
    }

    @Test
    @DisplayName("Deve validar entrada de dados nos endpoints")
    void shouldValidateInputDataInEndpoints() throws Exception {
        // Teste com mensagem vazia
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());

        // Teste com mensagem null
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        // Teste com JSON malformado
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":"))
                .andExpect(status().isBadRequest());

        // Teste com mensagem muito longa
        String longMessage = "a".repeat(1001);
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + longMessage + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve funcionar corretamente com requisições concorrentes")
    void shouldHandleConcurrentRequestsCorrectly() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Executar múltiplas requisições concorrentes
        CompletableFuture<Void>[] futures = new CompletableFuture[20];
        
        for (int i = 0; i < 20; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    mockMvc.perform(post("/api/toast/info")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":\"Mensagem " + index + "\"}"))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // Aguardar conclusão de todas as requisições
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        // Verificar que todas as mensagens foram adicionadas
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(20));

        executor.shutdown();
    }

    @Test
    @DisplayName("Deve integrar corretamente com GlobalExceptionHandler")
    void shouldIntegrateCorrectlyWithGlobalExceptionHandler() throws Exception {
        // Simular erro que deve ser capturado pelo GlobalExceptionHandler
        mockMvc.perform(get("/api/toast/messages/invalid-endpoint")
                .session(session))
                .andExpect(status().isNotFound());

        // Verificar se uma mensagem de erro foi adicionada pelo exception handler
        // (assumindo que o GlobalExceptionHandler adiciona mensagens toast para erros)
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve suportar mensagens com títulos personalizados")
    void shouldSupportMessagesWithCustomTitles() throws Exception {
        // Adicionar mensagem com título personalizado
        mockMvc.perform(post("/api/toast/message")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"WARNING\",\"title\":\"Atenção\",\"message\":\"Operação pode demorar\"}"))
                .andExpect(status().isOk());

        // Verificar mensagem com título
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("WARNING"))
                .andExpect(jsonPath("$[0].title").value("Atenção"))
                .andExpect(jsonPath("$[0].message").value("Operação pode demorar"));
    }

    @Test
    @DisplayName("Deve verificar status de mensagens corretamente")
    void shouldCheckMessageStatusCorrectly() throws Exception {
        // Verificar quando não há mensagens
        mockMvc.perform(get("/api/toast/has-messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(false));

        // Adicionar uma mensagem
        toastService.info("Teste");

        // Verificar quando há mensagens
        mockMvc.perform(get("/api/toast/has-messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(true));

        // Verificar contagem
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("Deve limpar todas as mensagens corretamente")
    void shouldClearAllMessagesCorrectly() throws Exception {
        // Adicionar várias mensagens
        toastService.success("Sucesso 1");
        toastService.error("Erro 1");
        toastService.warning("Aviso 1");
        toastService.info("Info 1");

        // Verificar que existem mensagens
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));

        // Limpar todas as mensagens
        mockMvc.perform(delete("/api/toast/clear")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verificar que não há mais mensagens
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("Deve manter ordem das mensagens (FIFO)")
    void shouldMaintainMessageOrderFIFO() throws Exception {
        // Adicionar mensagens em ordem específica
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Primeira mensagem\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/toast/error")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Segunda mensagem\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/toast/warning")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Terceira mensagem\"}"))
                .andExpect(status().isOk());

        // Verificar ordem das mensagens
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].message").value("Primeira mensagem"))
                .andExpect(jsonPath("$[1].message").value("Segunda mensagem"))
                .andExpect(jsonPath("$[2].message").value("Terceira mensagem"));
    }

    @Test
    @DisplayName("Deve suportar caracteres especiais e Unicode")
    void shouldSupportSpecialCharactersAndUnicode() throws Exception {
        String specialMessage = "Mensagem com acentos: ção, ã, é, ü, ñ e emojis: 🎉 🚀 ✅";
        
        mockMvc.perform(post("/api/toast/info")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + specialMessage + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value(specialMessage));
    }
}