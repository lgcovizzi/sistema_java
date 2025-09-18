package com.sistema.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para gerenciar mensagens toast
 * Integra com o sistema de sessão para persistir mensagens entre requisições
 */
@Service
public class ToastService {
    
    private static final String TOAST_MESSAGES_ATTRIBUTE = "toastMessages";
    
    /**
     * Adiciona mensagem de sucesso
     */
    public void success(String message) {
        addMessage(ToastMessage.success(message));
    }
    
    /**
     * Adiciona mensagem de sucesso com título customizado
     */
    public void success(String title, String message) {
        addMessage(ToastMessage.success(title, message));
    }
    
    /**
     * Adiciona mensagem de erro
     */
    public void error(String message) {
        addMessage(ToastMessage.error(message));
    }
    
    /**
     * Adiciona mensagem de erro com título customizado
     */
    public void error(String title, String message) {
        addMessage(ToastMessage.error(title, message));
    }
    
    /**
     * Adiciona mensagem de aviso
     */
    public void warning(String message) {
        addMessage(ToastMessage.warning(message));
    }
    
    /**
     * Adiciona mensagem de aviso com título customizado
     */
    public void warning(String title, String message) {
        addMessage(ToastMessage.warning(title, message));
    }
    
    /**
     * Adiciona mensagem de informação
     */
    public void info(String message) {
        addMessage(ToastMessage.info(message));
    }
    
    /**
     * Adiciona mensagem de informação com título customizado
     */
    public void info(String title, String message) {
        addMessage(ToastMessage.info(title, message));
    }
    
    /**
     * Adiciona mensagem customizada
     */
    public void addMessage(ToastMessage message) {
        HttpSession session = getCurrentSession();
        if (session != null) {
            List<ToastMessage> messages = getMessages(session);
            messages.add(message);
            session.setAttribute(TOAST_MESSAGES_ATTRIBUTE, messages);
        }
    }
    
    /**
     * Obtém todas as mensagens e as remove da sessão
     */
    public List<ToastMessage> getAndClearMessages() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            List<ToastMessage> messages = getMessages(session);
            session.removeAttribute(TOAST_MESSAGES_ATTRIBUTE);
            return new ArrayList<>(messages);
        }
        return new ArrayList<>();
    }
    
    /**
     * Obtém mensagens sem removê-las da sessão
     */
    public List<ToastMessage> getMessages() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            return new ArrayList<>(getMessages(session));
        }
        return new ArrayList<>();
    }
    
    /**
     * Limpa todas as mensagens da sessão
     */
    public void clearMessages() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.removeAttribute(TOAST_MESSAGES_ATTRIBUTE);
        }
    }
    
    /**
     * Verifica se há mensagens na sessão
     */
    public boolean hasMessages() {
        return !getMessages().isEmpty();
    }
    
    /**
     * Conta o número de mensagens na sessão
     */
    public int getMessageCount() {
        return getMessages().size();
    }
    
    /**
     * Adiciona mensagem baseada em exceção
     */
    public void fromException(Exception exception) {
        if (exception instanceof com.sistema.exception.BusinessException) {
            com.sistema.exception.BusinessException businessEx = 
                (com.sistema.exception.BusinessException) exception;
            
            String type = determineTypeFromException(businessEx);
            ToastMessage message = new ToastMessage(type, getDefaultTitle(type), 
                                                  businessEx.getMessage());
            addMessage(message);
        } else {
            error("Erro interno do sistema. Tente novamente.");
        }
    }
    
    /**
     * Determina o tipo de toast baseado na exceção
     */
    private String determineTypeFromException(com.sistema.exception.BusinessException exception) {
        String className = exception.getClass().getSimpleName();
        
        if (className.contains("Validation")) {
            return ToastMessage.TYPE_WARNING;
        } else if (className.contains("Authentication") || className.contains("Authorization")) {
            return ToastMessage.TYPE_ERROR;
        } else if (className.contains("NotFound")) {
            return ToastMessage.TYPE_INFO;
        } else if (className.contains("RateLimit")) {
            return ToastMessage.TYPE_WARNING;
        }
        
        return ToastMessage.TYPE_ERROR;
    }
    
    /**
     * Obtém título padrão para o tipo
     */
    private String getDefaultTitle(String type) {
        switch (type) {
            case ToastMessage.TYPE_SUCCESS:
                return "Sucesso";
            case ToastMessage.TYPE_ERROR:
                return "Erro";
            case ToastMessage.TYPE_WARNING:
                return "Atenção";
            case ToastMessage.TYPE_INFO:
                return "Informação";
            default:
                return "Notificação";
        }
    }
    
    /**
     * Obtém sessão HTTP atual
     */
    private HttpSession getCurrentSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) 
            RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession(true);
    }
    
    /**
     * Obtém lista de mensagens da sessão
     */
    @SuppressWarnings("unchecked")
    private List<ToastMessage> getMessages(HttpSession session) {
        List<ToastMessage> messages = (List<ToastMessage>) 
            session.getAttribute(TOAST_MESSAGES_ATTRIBUTE);
        
        if (messages == null) {
            messages = new ArrayList<>();
            session.setAttribute(TOAST_MESSAGES_ATTRIBUTE, messages);
        }
        
        return messages;
    }
    
    /**
     * Classe interna para representar uma mensagem toast
     */
    public static class ToastMessage {
        public static final String TYPE_SUCCESS = "success";
        public static final String TYPE_ERROR = "error";
        public static final String TYPE_WARNING = "warning";
        public static final String TYPE_INFO = "info";
        
        private String type;
        private String title;
        private String message;
        private long timestamp;
        
        public ToastMessage(String type, String title, String message) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Factory methods
        public static ToastMessage success(String message) {
            return new ToastMessage(TYPE_SUCCESS, "Sucesso", message);
        }
        
        public static ToastMessage success(String title, String message) {
            return new ToastMessage(TYPE_SUCCESS, title, message);
        }
        
        public static ToastMessage error(String message) {
            return new ToastMessage(TYPE_ERROR, "Erro", message);
        }
        
        public static ToastMessage error(String title, String message) {
            return new ToastMessage(TYPE_ERROR, title, message);
        }
        
        public static ToastMessage warning(String message) {
            return new ToastMessage(TYPE_WARNING, "Atenção", message);
        }
        
        public static ToastMessage warning(String title, String message) {
            return new ToastMessage(TYPE_WARNING, title, message);
        }
        
        public static ToastMessage info(String message) {
            return new ToastMessage(TYPE_INFO, "Informação", message);
        }
        
        public static ToastMessage info(String title, String message) {
            return new ToastMessage(TYPE_INFO, title, message);
        }
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("ToastMessage{type='%s', title='%s', message='%s'}", 
                               type, title, message);
        }
    }
}