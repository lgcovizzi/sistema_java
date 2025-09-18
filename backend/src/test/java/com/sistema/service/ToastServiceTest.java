package com.sistema.service;

import com.sistema.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToastServiceTest {

    @Mock
    private HttpSession httpSession;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @InjectMocks
    private ToastService toastService;

    private List<ToastService.ToastMessage> mockMessages;

    @BeforeEach
    void setUp() {
        mockMessages = new ArrayList<>();
        
        // Mock da sessão HTTP
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);
        when(httpSession.getAttribute("toastMessages")).thenReturn(mockMessages);
    }

    @Test
    void testSuccess_WithMessage() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            toastService.success("Operação realizada com sucesso");

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            
            ToastService.ToastMessage message = mockMessages.get(0);
            assertEquals(ToastService.ToastMessage.TYPE_SUCCESS, message.getType());
            assertEquals("Sucesso", message.getTitle());
            assertEquals("Operação realizada com sucesso", message.getMessage());
            assertTrue(message.getTimestamp() > 0);
        }
    }

    @Test
    void testSuccess_WithTitleAndMessage() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            toastService.success("Título Personalizado", "Mensagem personalizada");

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            
            ToastService.ToastMessage message = mockMessages.get(0);
            assertEquals(ToastService.ToastMessage.TYPE_SUCCESS, message.getType());
            assertEquals("Título Personalizado", message.getTitle());
            assertEquals("Mensagem personalizada", message.getMessage());
        }
    }

    @Test
    void testError_WithMessage() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            toastService.error("Erro na operação");

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            
            ToastService.ToastMessage message = mockMessages.get(0);
            assertEquals(ToastService.ToastMessage.TYPE_ERROR, message.getType());
            assertEquals("Erro", message.getTitle());
            assertEquals("Erro na operação", message.getMessage());
        }
    }

    @Test
    void testWarning_WithMessage() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            toastService.warning("Atenção necessária");

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            
            ToastService.ToastMessage message = mockMessages.get(0);
            assertEquals(ToastService.ToastMessage.TYPE_WARNING, message.getType());
            assertEquals("Aviso", message.getTitle());
            assertEquals("Atenção necessária", message.getMessage());
        }
    }

    @Test
    void testInfo_WithMessage() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            toastService.info("Informação importante");

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            
            ToastService.ToastMessage message = mockMessages.get(0);
            assertEquals(ToastService.ToastMessage.TYPE_INFO, message.getType());
            assertEquals("Informação", message.getTitle());
            assertEquals("Informação importante", message.getMessage());
        }
    }

    @Test
    void testAddMessage_DirectMessage() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            ToastService.ToastMessage customMessage = new ToastService.ToastMessage(
                    ToastService.ToastMessage.TYPE_SUCCESS, 
                    "Título Custom", 
                    "Mensagem custom"
            );

            toastService.addMessage(customMessage);

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            assertEquals(customMessage, mockMessages.get(0));
        }
    }

    @Test
    void testGetAndClearMessages() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            // Adiciona algumas mensagens
            mockMessages.add(ToastService.ToastMessage.success("Mensagem 1"));
            mockMessages.add(ToastService.ToastMessage.error("Mensagem 2"));

            List<ToastService.ToastMessage> result = toastService.getAndClearMessages();

            assertEquals(2, result.size());
            assertEquals("Mensagem 1", result.get(0).getMessage());
            assertEquals("Mensagem 2", result.get(1).getMessage());
            
            // Verifica se as mensagens foram limpas
            verify(httpSession).setAttribute("toastMessages", new ArrayList<>());
        }
    }

    @Test
    void testGetMessages() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            mockMessages.add(ToastService.ToastMessage.info("Mensagem teste"));

            List<ToastService.ToastMessage> result = toastService.getMessages();

            assertEquals(1, result.size());
            assertEquals("Mensagem teste", result.get(0).getMessage());
            
            // Verifica que as mensagens NÃO foram limpas
            assertEquals(1, mockMessages.size());
        }
    }

    @Test
    void testClearMessages() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            mockMessages.add(ToastService.ToastMessage.success("Mensagem"));

            toastService.clearMessages();

            verify(httpSession).setAttribute("toastMessages", new ArrayList<>());
        }
    }

    @Test
    void testHasMessages_WithMessages() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            mockMessages.add(ToastService.ToastMessage.success("Mensagem"));

            assertTrue(toastService.hasMessages());
        }
    }

    @Test
    void testHasMessages_WithoutMessages() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            assertFalse(toastService.hasMessages());
        }
    }

    @Test
    void testGetMessageCount() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            mockMessages.add(ToastService.ToastMessage.success("Mensagem 1"));
            mockMessages.add(ToastService.ToastMessage.error("Mensagem 2"));
            mockMessages.add(ToastService.ToastMessage.warning("Mensagem 3"));

            assertEquals(3, toastService.getMessageCount());
        }
    }

    @Test
    void testFromException_BusinessException() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            BusinessException exception = new BusinessException("Erro de negócio");

            toastService.fromException(exception);

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            
            ToastService.ToastMessage message = mockMessages.get(0);
            assertEquals(ToastService.ToastMessage.TYPE_ERROR, message.getType());
            assertEquals("Erro", message.getTitle());
            assertEquals("Erro de negócio", message.getMessage());
        }
    }

    @Test
    void testFromException_GenericException() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);

            RuntimeException exception = new RuntimeException("Erro genérico");

            toastService.fromException(exception);

            verify(httpSession).setAttribute(eq("toastMessages"), any(List.class));
            assertEquals(1, mockMessages.size());
            
            ToastService.ToastMessage message = mockMessages.get(0);
            assertEquals(ToastService.ToastMessage.TYPE_ERROR, message.getType());
            assertEquals("Erro", message.getTitle());
            assertEquals("Erro genérico", message.getMessage());
        }
    }

    @Test
    void testSessionCreation_WhenSessionIsNull() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);
            
            when(httpSession.getAttribute("toastMessages")).thenReturn(null);

            toastService.success("Teste");

            verify(httpSession).setAttribute(eq("toastMessages"), any(ArrayList.class));
        }
    }

    @Test
    void testToastMessage_FactoryMethods() {
        ToastService.ToastMessage successMessage = ToastService.ToastMessage.success("Sucesso");
        assertEquals(ToastService.ToastMessage.TYPE_SUCCESS, successMessage.getType());
        assertEquals("Sucesso", successMessage.getTitle());
        assertEquals("Sucesso", successMessage.getMessage());

        ToastService.ToastMessage errorMessage = ToastService.ToastMessage.error("Título", "Erro");
        assertEquals(ToastService.ToastMessage.TYPE_ERROR, errorMessage.getType());
        assertEquals("Título", errorMessage.getTitle());
        assertEquals("Erro", errorMessage.getMessage());

        ToastService.ToastMessage warningMessage = ToastService.ToastMessage.warning("Aviso");
        assertEquals(ToastService.ToastMessage.TYPE_WARNING, warningMessage.getType());
        assertEquals("Aviso", warningMessage.getTitle());
        assertEquals("Aviso", warningMessage.getMessage());

        ToastService.ToastMessage infoMessage = ToastService.ToastMessage.info("Info");
        assertEquals(ToastService.ToastMessage.TYPE_INFO, infoMessage.getType());
        assertEquals("Informação", infoMessage.getTitle());
        assertEquals("Info", infoMessage.getMessage());
    }

    @Test
    void testToastMessage_SettersAndGetters() {
        ToastService.ToastMessage message = new ToastService.ToastMessage(
                ToastService.ToastMessage.TYPE_SUCCESS, 
                "Título", 
                "Mensagem"
        );

        assertEquals(ToastService.ToastMessage.TYPE_SUCCESS, message.getType());
        assertEquals("Título", message.getTitle());
        assertEquals("Mensagem", message.getMessage());
        assertTrue(message.getTimestamp() > 0);

        message.setType(ToastService.ToastMessage.TYPE_ERROR);
        message.setTitle("Novo Título");
        message.setMessage("Nova Mensagem");
        long newTimestamp = System.currentTimeMillis();
        message.setTimestamp(newTimestamp);

        assertEquals(ToastService.ToastMessage.TYPE_ERROR, message.getType());
        assertEquals("Novo Título", message.getTitle());
        assertEquals("Nova Mensagem", message.getMessage());
        assertEquals(newTimestamp, message.getTimestamp());
    }

    @Test
    void testToastMessage_ToString() {
        ToastService.ToastMessage message = new ToastService.ToastMessage(
                ToastService.ToastMessage.TYPE_SUCCESS, 
                "Título", 
                "Mensagem"
        );

        String toString = message.toString();
        assertTrue(toString.contains("success"));
        assertTrue(toString.contains("Título"));
        assertTrue(toString.contains("Mensagem"));
        assertTrue(toString.contains(String.valueOf(message.getTimestamp())));
    }

    @Test
    void testExceptionHandling_WhenSessionUnavailable() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::currentRequestAttributes)
                    .thenThrow(new IllegalStateException("No request context"));

            // Deve não lançar exceção, apenas logar o erro
            assertDoesNotThrow(() -> toastService.success("Teste"));
        }
    }
}