package com.sistema.entity;

import com.sistema.enums.EmailProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o enum EmailProvider.
 * Verifica valores, métodos e comportamentos do enum.
 */
@DisplayName("Testes do EmailProvider")
class EmailProviderTest {

    @Test
    @DisplayName("Deve ter todos os provedores esperados")
    void shouldHaveAllExpectedProviders() {
        // Given & When
        EmailProvider[] providers = EmailProvider.values();

        // Then
        assertEquals(2, providers.length, "Deve ter exatamente 2 provedores");
        assertTrue(containsProvider(providers, EmailProvider.MAILTRAP), "Deve conter MAILTRAP");
        assertTrue(containsProvider(providers, EmailProvider.GMAIL), "Deve conter GMAIL");
    }

    @Test
    @DisplayName("Deve retornar nome correto para MAILTRAP")
    void shouldReturnCorrectNameForMailtrap() {
        // Given
        EmailProvider provider = EmailProvider.MAILTRAP;

        // When
        String name = provider.getName();

        // Then
        assertEquals("Mailtrap", name, "Nome do MAILTRAP deve ser 'Mailtrap'");
    }

    @Test
    @DisplayName("Deve retornar nome correto para GMAIL")
    void shouldReturnCorrectNameForGmail() {
        // Given
        EmailProvider provider = EmailProvider.GMAIL;

        // When
        String name = provider.getName();

        // Then
        assertEquals("Gmail", name, "Nome do GMAIL deve ser 'Gmail'");
    }

    @Test
    @DisplayName("Deve retornar descrição correta para MAILTRAP")
    void shouldReturnCorrectDescriptionForMailtrap() {
        // Given
        EmailProvider provider = EmailProvider.MAILTRAP;

        // When
        String description = provider.getDescription();

        // Then
        assertEquals("Serviço de email para desenvolvimento e testes", description,
                "Descrição do MAILTRAP deve estar correta");
    }

    @Test
    @DisplayName("Deve retornar descrição correta para GMAIL")
    void shouldReturnCorrectDescriptionForGmail() {
        // Given
        EmailProvider provider = EmailProvider.GMAIL;

        // When
        String description = provider.getDescription();

        // Then
        assertEquals("Serviço de email do Google para produção", description,
                "Descrição do GMAIL deve estar correta");
    }

    @Test
    @DisplayName("Deve retornar host padrão correto para MAILTRAP")
    void shouldReturnCorrectDefaultHostForMailtrap() {
        // Given
        EmailProvider provider = EmailProvider.MAILTRAP;

        // When
        String host = provider.getDefaultHost();

        // Then
        assertEquals("sandbox.smtp.mailtrap.io", host,
                "Host padrão do MAILTRAP deve ser 'sandbox.smtp.mailtrap.io'");
    }

    @Test
    @DisplayName("Deve retornar host padrão correto para GMAIL")
    void shouldReturnCorrectDefaultHostForGmail() {
        // Given
        EmailProvider provider = EmailProvider.GMAIL;

        // When
        String host = provider.getDefaultHost();

        // Then
        assertEquals("smtp.gmail.com", host,
                "Host padrão do GMAIL deve ser 'smtp.gmail.com'");
    }

    @Test
    @DisplayName("Deve retornar porta padrão correta para MAILTRAP")
    void shouldReturnCorrectDefaultPortForMailtrap() {
        // Given
        EmailProvider provider = EmailProvider.MAILTRAP;

        // When
        int port = provider.getDefaultPort();

        // Then
        assertEquals(2525, port, "Porta padrão do MAILTRAP deve ser 2525");
    }

    @Test
    @DisplayName("Deve retornar porta padrão correta para GMAIL")
    void shouldReturnCorrectDefaultPortForGmail() {
        // Given
        EmailProvider provider = EmailProvider.GMAIL;

        // When
        int port = provider.getDefaultPort();

        // Then
        assertEquals(587, port, "Porta padrão do GMAIL deve ser 587");
    }

    @Test
    @DisplayName("MAILTRAP deve usar TLS")
    void shouldMailtrapUseTls() {
        // Given
        EmailProvider provider = EmailProvider.MAILTRAP;

        // When
        boolean usesTls = provider.isUseTls();

        // Then
        assertTrue(usesTls, "MAILTRAP deve usar TLS");
    }

    @Test
    @DisplayName("GMAIL deve usar TLS")
    void shouldGmailUseTls() {
        // Given
        EmailProvider provider = EmailProvider.GMAIL;

        // When
        boolean usesTls = provider.isUseTls();

        // Then
        assertTrue(usesTls, "GMAIL deve usar TLS");
    }

    @Test
    @DisplayName("MAILTRAP deve requerer autenticação")
    void shouldMailtrapRequireAuth() {
        // Given
        EmailProvider provider = EmailProvider.MAILTRAP;

        // When
        boolean requiresAuth = provider.isRequiresAuth();

        // Then
        assertTrue(requiresAuth, "MAILTRAP deve requerer autenticação");
    }

    @Test
    @DisplayName("GMAIL deve requerer autenticação")
    void shouldGmailRequireAuth() {
        // Given
        EmailProvider provider = EmailProvider.GMAIL;

        // When
        boolean requiresAuth = provider.isRequiresAuth();

        // Then
        assertTrue(requiresAuth, "GMAIL deve requerer autenticação");
    }

    @Test
    @DisplayName("Deve encontrar provedor por nome (case insensitive)")
    void shouldFindProviderByNameCaseInsensitive() {
        // Given & When & Then
        assertEquals(EmailProvider.MAILTRAP, EmailProvider.fromName("mailtrap"));
        assertEquals(EmailProvider.MAILTRAP, EmailProvider.fromName("MAILTRAP"));
        assertEquals(EmailProvider.MAILTRAP, EmailProvider.fromName("Mailtrap"));
        
        assertEquals(EmailProvider.GMAIL, EmailProvider.fromName("gmail"));
        assertEquals(EmailProvider.GMAIL, EmailProvider.fromName("GMAIL"));
        assertEquals(EmailProvider.GMAIL, EmailProvider.fromName("Gmail"));
    }

    @Test
    @DisplayName("Deve lançar exceção para nome de provedor inválido")
    void shouldThrowExceptionForInvalidProviderName() {
        // Given
        String invalidName = "INVALID_PROVIDER";

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EmailProvider.fromName(invalidName),
                "Deve lançar IllegalArgumentException para nome inválido"
        );

        assertTrue(exception.getMessage().contains("Provedor de email não encontrado"),
                "Mensagem de erro deve conter 'Provedor de email não encontrado'");
        assertTrue(exception.getMessage().contains(invalidName),
                "Mensagem de erro deve conter o nome inválido");
    }

    @Test
    @DisplayName("Deve lançar exceção para nome nulo")
    void shouldThrowExceptionForNullName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EmailProvider.fromName(null),
                "Deve lançar IllegalArgumentException para nome nulo"
        );

        assertTrue(exception.getMessage().contains("Nome do provedor não pode ser nulo ou vazio"),
                "Mensagem de erro deve indicar que o nome não pode ser nulo");
    }

    @Test
    @DisplayName("Deve lançar exceção para nome vazio")
    void shouldThrowExceptionForEmptyName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EmailProvider.fromName(""),
                "Deve lançar IllegalArgumentException para nome vazio"
        );

        assertTrue(exception.getMessage().contains("Nome do provedor não pode ser nulo ou vazio"),
                "Mensagem de erro deve indicar que o nome não pode ser vazio");
    }

    @Test
    @DisplayName("Deve lançar exceção para nome com apenas espaços")
    void shouldThrowExceptionForBlankName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EmailProvider.fromName("   "),
                "Deve lançar IllegalArgumentException para nome com apenas espaços"
        );

        assertTrue(exception.getMessage().contains("Nome do provedor não pode ser nulo ou vazio"),
                "Mensagem de erro deve indicar que o nome não pode ser vazio");
    }

    @ParameterizedTest
    @EnumSource(EmailProvider.class)
    @DisplayName("Todos os provedores devem ter nome não nulo")
    void shouldAllProvidersHaveNonNullName(EmailProvider provider) {
        // When
        String name = provider.getName();

        // Then
        assertNotNull(name, "Nome do provedor não deve ser nulo");
        assertFalse(name.trim().isEmpty(), "Nome do provedor não deve ser vazio");
    }

    @ParameterizedTest
    @EnumSource(EmailProvider.class)
    @DisplayName("Todos os provedores devem ter descrição não nula")
    void shouldAllProvidersHaveNonNullDescription(EmailProvider provider) {
        // When
        String description = provider.getDescription();

        // Then
        assertNotNull(description, "Descrição do provedor não deve ser nula");
        assertFalse(description.trim().isEmpty(), "Descrição do provedor não deve ser vazia");
    }

    @ParameterizedTest
    @EnumSource(EmailProvider.class)
    @DisplayName("Todos os provedores devem ter host padrão não nulo")
    void shouldAllProvidersHaveNonNullDefaultHost(EmailProvider provider) {
        // When
        String host = provider.getDefaultHost();

        // Then
        assertNotNull(host, "Host padrão do provedor não deve ser nulo");
        assertFalse(host.trim().isEmpty(), "Host padrão do provedor não deve ser vazio");
    }

    @ParameterizedTest
    @EnumSource(EmailProvider.class)
    @DisplayName("Todos os provedores devem ter porta padrão válida")
    void shouldAllProvidersHaveValidDefaultPort(EmailProvider provider) {
        // When
        int port = provider.getDefaultPort();

        // Then
        assertTrue(port > 0, "Porta padrão deve ser maior que 0");
        assertTrue(port <= 65535, "Porta padrão deve ser menor ou igual a 65535");
    }

    @Test
    @DisplayName("Deve retornar string correta no toString")
    void shouldReturnCorrectStringInToString() {
        // Given & When & Then
        assertEquals("MAILTRAP", EmailProvider.MAILTRAP.toString());
        assertEquals("GMAIL", EmailProvider.GMAIL.toString());
    }

    @Test
    @DisplayName("Deve ser possível usar em switch statement")
    void shouldBeUsableInSwitchStatement() {
        // Given
        EmailProvider provider = EmailProvider.MAILTRAP;
        String result;

        // When
        switch (provider) {
            case MAILTRAP:
                result = "Desenvolvimento";
                break;
            case GMAIL:
                result = "Produção";
                break;
            default:
                result = "Desconhecido";
        }

        // Then
        assertEquals("Desenvolvimento", result, "Switch deve funcionar corretamente");
    }

    @Test
    @DisplayName("Deve manter ordem consistente dos valores")
    void shouldMaintainConsistentOrderOfValues() {
        // Given & When
        EmailProvider[] providers = EmailProvider.values();

        // Then
        assertEquals(EmailProvider.MAILTRAP, providers[0], "MAILTRAP deve ser o primeiro");
        assertEquals(EmailProvider.GMAIL, providers[1], "GMAIL deve ser o segundo");
    }

    @Test
    @DisplayName("Deve ser possível converter de e para string")
    void shouldBeConvertibleToAndFromString() {
        // Given
        EmailProvider original = EmailProvider.GMAIL;

        // When
        String stringValue = original.name();
        EmailProvider converted = EmailProvider.valueOf(stringValue);

        // Then
        assertEquals(original, converted, "Conversão de/para string deve manter o valor");
    }

    /**
     * Método auxiliar para verificar se um array contém um provedor específico.
     */
    private boolean containsProvider(EmailProvider[] providers, EmailProvider target) {
        for (EmailProvider provider : providers) {
            if (provider == target) {
                return true;
            }
        }
        return false;
    }
}