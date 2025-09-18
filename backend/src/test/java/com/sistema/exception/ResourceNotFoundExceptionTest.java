package com.sistema.exception;

import com.sistema.util.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResourceNotFoundException Tests")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Deve criar ResourceNotFoundException com mensagem e código de erro")
    void testCreateResourceNotFoundExceptionWithMessageAndErrorCode() {
        // Given
        String message = "Usuário não encontrado";
        String errorCode = ErrorCodes.USER_NOT_FOUND;

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve criar ResourceNotFoundException com mensagem, código e parâmetros")
    void testCreateResourceNotFoundExceptionWithParameters() {
        // Given
        String message = "Recurso {0} com ID {1} não encontrado";
        String errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
        Object[] parameters = {"Usuario", 123L};

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertArrayEquals(parameters, exception.getParameters());
    }

    @Test
    @DisplayName("Deve criar ResourceNotFoundException com mensagem, código, parâmetros e causa")
    void testCreateResourceNotFoundExceptionWithCause() {
        // Given
        String message = "Erro ao buscar recurso";
        String errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
        Object[] parameters = {"Produto", "ABC123"};
        Throwable cause = new RuntimeException("Database connection failed");

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode, parameters, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertArrayEquals(parameters, exception.getParameters());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Deve criar ResourceNotFoundException apenas com mensagem")
    void testCreateResourceNotFoundExceptionWithMessageOnly() {
        // Given
        String message = "Recurso não encontrado";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve criar ResourceNotFoundException com mensagem e causa")
    void testCreateResourceNotFoundExceptionWithMessageAndCause() {
        // Given
        String message = "Falha ao localizar recurso";
        Throwable cause = new IllegalArgumentException("Invalid search criteria");

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getErrorCode());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve ser uma BusinessException")
    void testResourceNotFoundExceptionIsBusinessException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Teste");

        // Then
        assertInstanceOf(BusinessException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Deve lidar com diferentes tipos de recursos não encontrados")
    void testResourceNotFoundExceptionWithDifferentResourceTypes() {
        // Test usuário não encontrado
        ResourceNotFoundException userException = new ResourceNotFoundException(
            "Usuário não encontrado", ErrorCodes.USER_NOT_FOUND);
        assertEquals(ErrorCodes.USER_NOT_FOUND, userException.getErrorCode());

        // Test recurso genérico não encontrado
        ResourceNotFoundException resourceException = new ResourceNotFoundException(
            "Recurso não encontrado", ErrorCodes.RESOURCE_NOT_FOUND);
        assertEquals(ErrorCodes.RESOURCE_NOT_FOUND, resourceException.getErrorCode());

        // Test página não encontrada
        ResourceNotFoundException pageException = new ResourceNotFoundException(
            "Página não encontrada", ErrorCodes.RESOURCE_NOT_FOUND);
        assertEquals(ErrorCodes.RESOURCE_NOT_FOUND, pageException.getErrorCode());
    }

    @Test
    @DisplayName("Deve manter informações de contexto do recurso")
    void testResourceNotFoundExceptionWithResourceContext() {
        // Given
        String message = "Recurso {0} com identificador {1} não encontrado na tabela {2}";
        String errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
        Object[] parameters = {"Usuario", "user@example.com", "users"};

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertArrayEquals(parameters, exception.getParameters());
        assertEquals("Usuario", exception.getParameters()[0]); // Tipo do recurso
        assertEquals("user@example.com", exception.getParameters()[1]); // Identificador
        assertEquals("users", exception.getParameters()[2]); // Tabela/Coleção
    }

    @Test
    @DisplayName("Deve suportar busca por múltiplos critérios")
    void testResourceNotFoundExceptionWithMultipleCriteria() {
        // Given
        String message = "Nenhum {0} encontrado com os critérios: {1}={2}, {3}={4}";
        String errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
        Object[] parameters = {"Usuario", "email", "test@example.com", "status", "ATIVO"};

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(5, exception.getParameters().length);
        assertEquals("Usuario", exception.getParameters()[0]);
        assertEquals("email", exception.getParameters()[1]);
        assertEquals("test@example.com", exception.getParameters()[2]);
        assertEquals("status", exception.getParameters()[3]);
        assertEquals("ATIVO", exception.getParameters()[4]);
    }

    @Test
    @DisplayName("Deve lidar com IDs de diferentes tipos")
    void testResourceNotFoundExceptionWithDifferentIdTypes() {
        // Test com Long ID
        ResourceNotFoundException longIdException = new ResourceNotFoundException(
            "Produto com ID {0} não encontrado", ErrorCodes.RESOURCE_NOT_FOUND, new Object[]{123L});
        assertEquals(123L, longIdException.getParameters()[0]);

        // Test com String ID
        ResourceNotFoundException stringIdException = new ResourceNotFoundException(
            "Categoria com código {0} não encontrada", ErrorCodes.RESOURCE_NOT_FOUND, new Object[]{"CAT001"});
        assertEquals("CAT001", stringIdException.getParameters()[0]);

        // Test com UUID
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        ResourceNotFoundException uuidException = new ResourceNotFoundException(
            "Sessão com UUID {0} não encontrada", ErrorCodes.RESOURCE_NOT_FOUND, new Object[]{uuid});
        assertEquals(uuid, uuidException.getParameters()[0]);
    }

    @Test
    @DisplayName("Deve preservar stack trace para debugging")
    void testResourceNotFoundExceptionStackTrace() {
        // Given/When
        ResourceNotFoundException exception = new ResourceNotFoundException(
            "Recurso não encontrado", ErrorCodes.RESOURCE_NOT_FOUND);

        // Then
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
        assertEquals("testResourceNotFoundExceptionStackTrace", 
                    exception.getStackTrace()[0].getMethodName());
    }

    @Test
    @DisplayName("Deve suportar informações de paginação")
    void testResourceNotFoundExceptionWithPagination() {
        // Given
        String message = "Nenhum resultado encontrado na página {0} de {1} (total: {2} registros)";
        String errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
        Object[] parameters = {5, 10, 42}; // página 5 de 10, total 42 registros

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(5, exception.getParameters()[0]); // Página atual
        assertEquals(10, exception.getParameters()[1]); // Total de páginas
        assertEquals(42, exception.getParameters()[2]); // Total de registros
    }

    @Test
    @DisplayName("Deve lidar com recursos relacionados não encontrados")
    void testResourceNotFoundExceptionWithRelatedResources() {
        // Given
        String message = "Recurso {0} não encontrado para o {1} com ID {2}";
        String errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
        Object[] parameters = {"Endereço", "Usuario", 123L};

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals("Endereço", exception.getParameters()[0]); // Recurso filho
        assertEquals("Usuario", exception.getParameters()[1]); // Recurso pai
        assertEquals(123L, exception.getParameters()[2]); // ID do pai
    }

    @Test
    @DisplayName("Deve suportar filtros de busca complexos")
    void testResourceNotFoundExceptionWithComplexFilters() {
        // Given
        String message = "Nenhum resultado encontrado com os filtros aplicados";
        String errorCode = ErrorCodes.RESOURCE_NOT_FOUND;
        Object[] parameters = {
            "status=ATIVO", 
            "dataInicio>=2024-01-01", 
            "categoria IN [A,B,C]",
            "valor BETWEEN 100 AND 500"
        };

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(4, exception.getParameters().length);
        assertTrue(exception.getParameters()[0].toString().contains("status=ATIVO"));
        assertTrue(exception.getParameters()[1].toString().contains("dataInicio>=2024-01-01"));
    }
}