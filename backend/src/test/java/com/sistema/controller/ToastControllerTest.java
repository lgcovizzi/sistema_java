package com.sistema.controller;

import com.sistema.service.ToastService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToastController.class)
class ToastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToastService toastService;

    @Autowired
    private ObjectMapper objectMapper;

    private List<ToastService.ToastMessage> mockMessages;

    @BeforeEach
    void setUp() {
        mockMessages = Arrays.asList(
                ToastService.ToastMessage.success("Sucesso", "Operação realizada com sucesso"),
                ToastService.ToastMessage.error("Erro", "Erro na operação"),
                ToastService.ToastMessage.warning("Aviso", "Atenção necessária")
        );
    }

    @Test
    void testGetMessages_WithMessages() throws Exception {
        when(toastService.getMessages()).thenReturn(mockMessages);

        mockMvc.perform(get("/api/toast/messages"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].type").value("success"))
                .andExpect(jsonPath("$.data[0].title").value("Sucesso"))
                .andExpect(jsonPath("$.data[0].message").value("Operação realizada com sucesso"))
                .andExpect(jsonPath("$.data[1].type").value("error"))
                .andExpect(jsonPath("$.data[2].type").value("warning"));

        verify(toastService).getMessages();
    }

    @Test
    void testGetMessages_WithoutMessages() throws Exception {
        when(toastService.getMessages()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/toast/messages"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(toastService).getMessages();
    }

    @Test
    void testGetAndClearMessages() throws Exception {
        when(toastService.getAndClearMessages()).thenReturn(mockMessages);

        mockMvc.perform(get("/api/toast/messages/clear"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));

        verify(toastService).getAndClearMessages();
    }

    @Test
    void testAddSuccessMessage_ValidRequest() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título de Sucesso");
        request.setMessage("Mensagem de sucesso");

        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mensagem de sucesso adicionada"));

        verify(toastService).success("Título de Sucesso", "Mensagem de sucesso");
    }

    @Test
    void testAddSuccessMessage_InvalidRequest_BlankTitle() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("");
        request.setMessage("Mensagem de sucesso");

        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(toastService, never()).success(anyString(), anyString());
    }

    @Test
    void testAddSuccessMessage_InvalidRequest_BlankMessage() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título");
        request.setMessage("");

        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(toastService, never()).success(anyString(), anyString());
    }

    @Test
    void testAddErrorMessage_ValidRequest() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título de Erro");
        request.setMessage("Mensagem de erro");

        mockMvc.perform(post("/api/toast/error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mensagem de erro adicionada"));

        verify(toastService).error("Título de Erro", "Mensagem de erro");
    }

    @Test
    void testAddWarningMessage_ValidRequest() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título de Aviso");
        request.setMessage("Mensagem de aviso");

        mockMvc.perform(post("/api/toast/warning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mensagem de aviso adicionada"));

        verify(toastService).warning("Título de Aviso", "Mensagem de aviso");
    }

    @Test
    void testAddInfoMessage_ValidRequest() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título de Info");
        request.setMessage("Mensagem de informação");

        mockMvc.perform(post("/api/toast/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mensagem de informação adicionada"));

        verify(toastService).info("Título de Info", "Mensagem de informação");
    }

    @Test
    void testAddMessage_ValidRequest() throws Exception {
        ToastController.MessageRequest request = new ToastController.MessageRequest();
        request.setMessage("Mensagem simples");

        mockMvc.perform(post("/api/toast/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mensagem adicionada"));

        verify(toastService).info("Mensagem simples");
    }

    @Test
    void testAddMessage_InvalidRequest_BlankMessage() throws Exception {
        ToastController.MessageRequest request = new ToastController.MessageRequest();
        request.setMessage("");

        mockMvc.perform(post("/api/toast/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(toastService, never()).info(anyString());
    }

    @Test
    void testClearMessages() throws Exception {
        mockMvc.perform(delete("/api/toast/clear"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mensagens limpas"));

        verify(toastService).clearMessages();
    }

    @Test
    void testHasMessages_WithMessages() throws Exception {
        when(toastService.hasMessages()).thenReturn(true);

        mockMvc.perform(get("/api/toast/has-messages"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        verify(toastService).hasMessages();
    }

    @Test
    void testHasMessages_WithoutMessages() throws Exception {
        when(toastService.hasMessages()).thenReturn(false);

        mockMvc.perform(get("/api/toast/has-messages"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));

        verify(toastService).hasMessages();
    }

    @Test
    void testGetMessageCount() throws Exception {
        when(toastService.getMessageCount()).thenReturn(5);

        mockMvc.perform(get("/api/toast/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(5));

        verify(toastService).getMessageCount();
    }

    @Test
    void testGetMessageCount_Zero() throws Exception {
        when(toastService.getMessageCount()).thenReturn(0);

        mockMvc.perform(get("/api/toast/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(0));

        verify(toastService).getMessageCount();
    }

    @Test
    void testInvalidHttpMethod() throws Exception {
        mockMvc.perform(put("/api/toast/messages"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testInvalidEndpoint() throws Exception {
        mockMvc.perform(get("/api/toast/invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testContentTypeValidation() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título");
        request.setMessage("Mensagem");

        // Teste com content-type inválido
        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        verify(toastService, never()).success(anyString(), anyString());
    }

    @Test
    void testMalformedJson() throws Exception {
        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(toastService, never()).success(anyString(), anyString());
    }

    @Test
    void testEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(toastService, never()).success(anyString(), anyString());
    }

    @Test
    void testNullRequestBody() throws Exception {
        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(toastService, never()).success(anyString(), anyString());
    }

    @Test
    void testToastRequestValidation_InvalidType() throws Exception {
        // Teste com tipo inválido (simulando validação @Pattern)
        String invalidRequest = """
                {
                    "title": "Título",
                    "message": "Mensagem",
                    "type": "invalid_type"
                }
                """;

        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(toastService, never()).success(anyString(), anyString());
    }

    @Test
    void testConcurrentRequests() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título Concorrente");
        request.setMessage("Mensagem concorrente");

        // Simula múltiplas requisições simultâneas
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/toast/success")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        verify(toastService, times(5)).success("Título Concorrente", "Mensagem concorrente");
    }

    @Test
    void testServiceException() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título");
        request.setMessage("Mensagem");

        doThrow(new RuntimeException("Erro no serviço")).when(toastService)
                .success(anyString(), anyString());

        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(toastService).success("Título", "Mensagem");
    }

    @Test
    void testLargePayload() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("A".repeat(1000)); // Título muito longo
        request.setMessage("B".repeat(5000)); // Mensagem muito longa

        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(toastService).success(request.getTitle(), request.getMessage());
    }

    @Test
    void testSpecialCharacters() throws Exception {
        ToastController.ToastRequest request = new ToastController.ToastRequest();
        request.setTitle("Título com çãrácteres especiais! @#$%");
        request.setMessage("Mensagem com émojis 🎉🚀 e símbolos ©®™");

        mockMvc.perform(post("/api/toast/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(toastService).success(request.getTitle(), request.getMessage());
    }
}