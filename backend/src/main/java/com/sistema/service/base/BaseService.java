package com.sistema.service.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Classe base abstrata para todos os serviços do sistema.
 * Fornece funcionalidades comuns como logging, validação e tratamento de erros.
 */
public abstract class BaseService {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * Executa uma operação com tratamento de erro padronizado.
     * 
     * @param operation operação a ser executada
     * @param errorMessage mensagem de erro personalizada
     * @param <T> tipo de retorno
     * @return resultado da operação
     * @throws RuntimeException se ocorrer erro
     */
    protected <T> T executeWithErrorHandling(Supplier<T> operation, String errorMessage) {
        try {
            T result = operation.get();
            logger.debug("Operação executada com sucesso: {}", errorMessage);
            return result;
        } catch (Exception e) {
            logger.error("{}: {}", errorMessage, e.getMessage(), e);
            throw new RuntimeException(errorMessage + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Executa uma operação void com tratamento de erro padronizado.
     * 
     * @param operation operação a ser executada
     * @param errorMessage mensagem de erro personalizada
     * @throws RuntimeException se ocorrer erro
     */
    protected void executeWithErrorHandling(Runnable operation, String errorMessage) {
        try {
            operation.run();
            logger.debug("Operação executada com sucesso: {}", errorMessage);
        } catch (Exception e) {
            logger.error("{}: {}", errorMessage, e.getMessage(), e);
            throw new RuntimeException(errorMessage + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Valida se um parâmetro não é nulo ou vazio.
     * 
     * @param value valor a ser validado
     * @param paramName nome do parâmetro para mensagem de erro
     * @throws IllegalArgumentException se valor for inválido
     */
    protected void validateNotEmpty(String value, String paramName) {
        if (!StringUtils.hasText(value)) {
            String message = String.format("Parâmetro '%s' não pode ser nulo ou vazio", paramName);
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Valida se um objeto não é nulo.
     * 
     * @param object objeto a ser validado
     * @param paramName nome do parâmetro para mensagem de erro
     * @throws IllegalArgumentException se objeto for nulo
     */
    protected void validateNotNull(Object object, String paramName) {
        if (object == null) {
            String message = String.format("Parâmetro '%s' não pode ser nulo", paramName);
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Valida se um ID é válido (maior que 0).
     * 
     * @param id ID a ser validado
     * @param paramName nome do parâmetro para mensagem de erro
     * @throws IllegalArgumentException se ID for inválido
     */
    protected void validateId(Long id, String paramName) {
        validateNotNull(id, paramName);
        if (id <= 0) {
            String message = String.format("ID '%s' deve ser maior que zero", paramName);
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * Loga uma operação de busca.
     * 
     * @param operation descrição da operação
     * @param parameter parâmetro de busca
     * @param result resultado da busca
     */
    protected void logSearchOperation(String operation, Object parameter, Optional<?> result) {
        if (result.isPresent()) {
            logger.debug("{} encontrado para parâmetro: {}", operation, parameter);
        } else {
            logger.debug("{} não encontrado para parâmetro: {}", operation, parameter);
        }
    }
    
    /**
     * Loga uma operação de criação.
     * 
     * @param entityType tipo da entidade
     * @param entityId ID da entidade criada
     */
    protected void logCreateOperation(String entityType, Object entityId) {
        logger.info("{} criado com sucesso. ID: {}", entityType, entityId);
    }
    
    /**
     * Loga uma operação de atualização.
     * 
     * @param entityType tipo da entidade
     * @param entityId ID da entidade atualizada
     */
    protected void logUpdateOperation(String entityType, Object entityId) {
        logger.info("{} atualizado com sucesso. ID: {}", entityType, entityId);
    }
    
    /**
     * Loga uma operação de exclusão.
     * 
     * @param entityType tipo da entidade
     * @param entityId ID da entidade excluída
     */
    protected void logDeleteOperation(String entityType, Object entityId) {
        logger.info("{} excluído com sucesso. ID: {}", entityType, entityId);
    }
    
    /**
     * Obtém timestamp atual para auditoria.
     * 
     * @return timestamp atual
     */
    protected LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }
    
    /**
     * Formata uma mensagem de erro padronizada.
     * 
     * @param operation operação que falhou
     * @param details detalhes do erro
     * @return mensagem formatada
     */
    protected String formatErrorMessage(String operation, String details) {
        return String.format("Erro ao %s: %s", operation, details);
    }
    
    /**
     * Loga uma mensagem de erro.
     * 
     * @param message mensagem de erro
     */
    protected void logError(String message) {
        logger.error(message);
    }
    
    /**
     * Loga uma mensagem de erro com exceção.
     * 
     * @param message mensagem de erro
     * @param exception exceção associada
     */
    protected void logError(String message, Exception exception) {
        logger.error(message, exception);
    }
    
    /**
     * Loga uma mensagem de informação.
     * 
     * @param message mensagem de informação
     */
    protected void logInfo(String message) {
        logger.info(message);
    }
    
    /**
     * Loga uma mensagem de aviso.
     * 
     * @param message mensagem de aviso
     */
    protected void logWarn(String message) {
        logger.warn(message);
    }
    
    /**
     * Loga uma mensagem de debug.
     * 
     * @param message mensagem de debug
     */
    protected void logDebug(String message) {
        logger.debug(message);
    }
}