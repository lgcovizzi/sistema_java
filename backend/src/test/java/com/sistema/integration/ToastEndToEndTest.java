package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.dto.ToastMessage;
import com.sistema.enums.ToastType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes End-to-End - Sistema de Toast")
class ToastEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    @Test
    @DisplayName("Cenário completo: Adicionar, visualizar e limpar mensagens")
    void completeScenario_AddViewAndClearMessages() throws Exception {
        // 1. Verificar estado inicial (sem mensagens)
        mockMvc.perform(get("/api/toast/has-messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(false));

        // 2. Adicionar mensagem de sucesso
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Usuário cadastrado com sucesso!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Verificar que agora há mensagens
        mockMvc.perform(get("/api/toast/has-messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(true));

        // 4. Adicionar mensagem de aviso
        mockMvc.perform(post("/api/toast/warning")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Verifique seus dados antes de continuar\"}"))
                .andExpect(status().isOk());

        // 5. Verificar contagem total
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        // 6. Recuperar todas as mensagens
        MvcResult result = mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        List<ToastMessage> messages = objectMapper.readValue(responseContent, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, ToastMessage.class));
        
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getType()).isEqualTo(ToastType.SUCCESS);
        assertThat(messages.get(0).getMessage()).isEqualTo("Usuário cadastrado com sucesso!");
        assertThat(messages.get(1).getType()).isEqualTo(ToastType.WARNING);
        assertThat(messages.get(1).getMessage()).isEqualTo("Verifique seus dados antes de continuar");

        // 7. Limpar todas as mensagens
        mockMvc.perform(delete("/api/toast/clear")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 8. Verificar que não há mais mensagens
        mockMvc.perform(get("/api/toast/has-messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(false));
    }

    @Test
    @DisplayName("Cenário de formulário: Validação e feedback")
    void formScenario_ValidationAndFeedback() throws Exception {
        // Simular envio de formulário com dados inválidos
        mockMvc.perform(post("/api/toast/error")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Email inválido\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/toast/error")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Senha deve ter pelo menos 8 caracteres\"}"))
                .andExpect(status().isOk());

        // Verificar mensagens de erro
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("ERROR"))
                .andExpect(jsonPath("$[1].type").value("ERROR"));

        // Simular correção e sucesso
        mockMvc.perform(delete("/api/toast/clear")
                .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Formulário enviado com sucesso!\"}"))
                .andExpect(status().isOk());

        // Verificar mensagem de sucesso
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("SUCCESS"));
    }

    @Test
    @DisplayName("Cenário de operação assíncrona: Loading e resultado")
    void asyncOperationScenario_LoadingAndResult() throws Exception {
        // 1. Mostrar mensagem de loading
        mockMvc.perform(post("/api/toast/info")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Processando dados...\"}"))
                .andExpect(status().isOk());

        // 2. Verificar mensagem de loading
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("INFO"))
                .andExpect(jsonPath("$[0].message").value("Processando dados..."));

        // 3. Simular conclusão da operação - limpar loading
        mockMvc.perform(delete("/api/toast/clear")
                .session(session))
                .andExpect(status().isOk());

        // 4. Mostrar resultado da operação
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Dados processados com sucesso!\"}"))
                .andExpect(status().isOk());

        // 5. Verificar resultado final
        mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("SUCCESS"))
                .andExpect(jsonPath("$[0].message").value("Dados processados com sucesso!"));
    }

    @Test
    @DisplayName("Cenário de múltiplas abas: Isolamento de sessões")
    void multipleTabsScenario_SessionIsolation() throws Exception {
        MockHttpSession tab1 = new MockHttpSession();
        MockHttpSession tab2 = new MockHttpSession();

        // Aba 1: Operação de login
        mockMvc.perform(post("/api/toast/success")
                .session(tab1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Login realizado com sucesso!\"}"))
                .andExpect(status().isOk());

        // Aba 2: Operação de cadastro
        mockMvc.perform(post("/api/toast/info")
                .session(tab2)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Preencha todos os campos obrigatórios\"}"))
                .andExpect(status().isOk());

        // Verificar isolamento - Aba 1
        mockMvc.perform(get("/api/toast/messages")
                .session(tab1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("SUCCESS"))
                .andExpect(jsonPath("$[0].message").value("Login realizado com sucesso!"));

        // Verificar isolamento - Aba 2
        mockMvc.perform(get("/api/toast/messages")
                .session(tab2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("INFO"))
                .andExpect(jsonPath("$[0].message").value("Preencha todos os campos obrigatórios"));

        // Limpar apenas aba 1
        mockMvc.perform(delete("/api/toast/clear")
                .session(tab1))
                .andExpect(status().isOk());

        // Verificar que aba 1 foi limpa mas aba 2 mantém mensagens
        mockMvc.perform(get("/api/toast/has-messages")
                .session(tab1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(false));

        mockMvc.perform(get("/api/toast/has-messages")
                .session(tab2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(true));
    }

    @Test
    @DisplayName("Cenário de recuperação com limpeza automática")
    void retrieveAndClearScenario() throws Exception {
        // Adicionar várias mensagens
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Operação 1 concluída\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/toast/warning")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Atenção: Verificar configurações\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/toast/error")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Erro na operação 2\"}"))
                .andExpect(status().isOk());

        // Verificar que existem 3 mensagens
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        // Recuperar e limpar em uma única operação
        MvcResult result = mockMvc.perform(get("/api/toast/messages-and-clear")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        // Verificar conteúdo das mensagens recuperadas
        String responseContent = result.getResponse().getContentAsString();
        List<ToastMessage> messages = objectMapper.readValue(responseContent, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, ToastMessage.class));
        
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getType()).isEqualTo(ToastType.SUCCESS);
        assertThat(messages.get(1).getType()).isEqualTo(ToastType.WARNING);
        assertThat(messages.get(2).getType()).isEqualTo(ToastType.ERROR);

        // Verificar que as mensagens foram automaticamente limpas
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(get("/api/toast/has-messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMessages").value(false));
    }

    @Test
    @DisplayName("Cenário de mensagens com títulos personalizados")
    void customTitlesScenario() throws Exception {
        // Adicionar mensagem com título personalizado
        mockMvc.perform(post("/api/toast/message")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"SUCCESS\",\"title\":\"Parabéns!\",\"message\":\"Você completou o tutorial\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/toast/message")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ERROR\",\"title\":\"Ops!\",\"message\":\"Algo deu errado\"}"))
                .andExpect(status().isOk());

        // Verificar mensagens com títulos
        MvcResult result = mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        List<ToastMessage> messages = objectMapper.readValue(responseContent, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, ToastMessage.class));
        
        assertThat(messages.get(0).getTitle()).isEqualTo("Parabéns!");
        assertThat(messages.get(0).getMessage()).isEqualTo("Você completou o tutorial");
        assertThat(messages.get(1).getTitle()).isEqualTo("Ops!");
        assertThat(messages.get(1).getMessage()).isEqualTo("Algo deu errado");
    }

    @Test
    @DisplayName("Cenário de tratamento de erros de validação")
    void validationErrorsScenario() throws Exception {
        // Tentar adicionar mensagem vazia
        mockMvc.perform(post("/api/toast/success")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());

        // Tentar adicionar mensagem muito longa
        String longMessage = "a".repeat(1001);
        mockMvc.perform(post("/api/toast/error")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + longMessage + "\"}"))
                .andExpect(status().isBadRequest());

        // Tentar enviar JSON malformado
        mockMvc.perform(post("/api/toast/info")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":"))
                .andExpect(status().isBadRequest());

        // Verificar que nenhuma mensagem foi adicionada devido aos erros
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("Cenário de performance com muitas mensagens")
    void performanceScenario_ManyMessages() throws Exception {
        // Adicionar muitas mensagens rapidamente
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post("/api/toast/info")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"Mensagem " + i + "\"}"))
                    .andExpect(status().isOk());
        }

        // Verificar contagem
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(100));

        // Recuperar todas as mensagens (teste de performance)
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(get("/api/toast/messages")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100))
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verificar que a operação foi rápida (menos de 1 segundo)
        assertThat(duration).isLessThan(1000);

        // Limpar todas as mensagens
        mockMvc.perform(delete("/api/toast/clear")
                .session(session))
                .andExpect(status().isOk());

        // Verificar limpeza
        mockMvc.perform(get("/api/toast/count")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }
}