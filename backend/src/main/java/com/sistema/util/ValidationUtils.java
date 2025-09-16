package com.sistema.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utilitários para validação de dados.
 * Fornece métodos estáticos para validações comuns em toda a aplicação.
 */
public final class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._-]{3,20}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
    );
    
    private ValidationUtils() {
        // Classe utilitária - construtor privado
    }
    
    /**
     * Valida se um objeto não é nulo.
     * 
     * @param value valor a validar
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se valor é nulo
     */
    public static void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " não pode ser nulo");
        }
    }
    
    /**
     * Valida se uma string não é nula nem vazia.
     * 
     * @param value valor a validar
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se valor é nulo ou vazio
     */
    public static void validateNotEmpty(String value, String fieldName) {
        validateNotNull(value, fieldName);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " não pode estar vazio");
        }
    }
    
    /**
     * Valida se uma coleção não é nula nem vazia.
     * 
     * @param collection coleção a validar
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se coleção é nula ou vazia
     */
    public static void validateNotEmpty(Collection<?> collection, String fieldName) {
        validateNotNull(collection, fieldName);
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " não pode estar vazio");
        }
    }
    
    /**
     * Valida se um mapa não é nulo nem vazio.
     * 
     * @param map mapa a validar
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se mapa é nulo ou vazio
     */
    public static void validateNotEmpty(Map<?, ?> map, String fieldName) {
        validateNotNull(map, fieldName);
        if (map.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " não pode estar vazio");
        }
    }
    
    /**
     * Valida se um número é positivo.
     * 
     * @param value valor a validar
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se valor não é positivo
     */
    public static void validatePositive(Number value, String fieldName) {
        validateNotNull(value, fieldName);
        if (value.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " deve ser positivo");
        }
    }
    
    /**
     * Valida se um número não é negativo.
     * 
     * @param value valor a validar
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se valor é negativo
     */
    public static void validateNotNegative(Number value, String fieldName) {
        validateNotNull(value, fieldName);
        if (value.doubleValue() < 0) {
            throw new IllegalArgumentException(fieldName + " não pode ser negativo");
        }
    }
    
    /**
     * Valida se um valor está dentro de um intervalo.
     * 
     * @param value valor a validar
     * @param min valor mínimo (inclusivo)
     * @param max valor máximo (inclusivo)
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se valor fora do intervalo
     */
    public static void validateRange(Number value, Number min, Number max, String fieldName) {
        validateNotNull(value, fieldName);
        validateNotNull(min, "min");
        validateNotNull(max, "max");
        
        double val = value.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        
        if (val < minVal || val > maxVal) {
            throw new IllegalArgumentException(
                String.format("%s deve estar entre %s e %s", fieldName, min, max)
            );
        }
    }
    
    /**
     * Valida se uma string tem comprimento dentro do intervalo especificado.
     * 
     * @param value string a validar
     * @param minLength comprimento mínimo
     * @param maxLength comprimento máximo
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se comprimento inválido
     */
    public static void validateLength(String value, int minLength, int maxLength, String fieldName) {
        validateNotNull(value, fieldName);
        validateRange(minLength, 0, Integer.MAX_VALUE, "minLength");
        validateRange(maxLength, minLength, Integer.MAX_VALUE, "maxLength");
        
        int length = value.length();
        if (length < minLength || length > maxLength) {
            throw new IllegalArgumentException(
                String.format("%s deve ter entre %d e %d caracteres", fieldName, minLength, maxLength)
            );
        }
    }
    
    /**
     * Valida formato de email.
     * 
     * @param email email a validar
     * @throws IllegalArgumentException se formato inválido
     */
    public static void validateEmail(String email) {
        validateNotEmpty(email, "email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Formato de email inválido");
        }
    }
    
    /**
     * Valida formato de username.
     * 
     * @param username username a validar
     * @throws IllegalArgumentException se formato inválido
     */
    public static void validateUsername(String username) {
        validateNotEmpty(username, "username");
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                "Username deve ter 3-20 caracteres e conter apenas letras, números, pontos, hífens e underscores"
            );
        }
    }
    
    /**
     * Valida formato de telefone.
     * 
     * @param phone telefone a validar
     * @throws IllegalArgumentException se formato inválido
     */
    public static void validatePhone(String phone) {
        validateNotEmpty(phone, "phone");
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Formato de telefone inválido");
        }
    }
    
    /**
     * Valida força da senha.
     * 
     * @param password senha a validar
     * @throws IllegalArgumentException se senha não atende critérios
     */
    public static void validatePassword(String password) {
        validateNotEmpty(password, "password");
        
        if (password.length() < 8) {
            throw new IllegalArgumentException("Senha deve ter pelo menos 8 caracteres");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                "Senha deve conter pelo menos: 1 letra minúscula, 1 maiúscula, 1 número e 1 caractere especial"
            );
        }
    }
    
    /**
     * Valida se uma string corresponde a um padrão regex.
     * 
     * @param value valor a validar
     * @param pattern padrão regex
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se não corresponde ao padrão
     */
    public static void validatePattern(String value, Pattern pattern, String fieldName) {
        validateNotEmpty(value, fieldName);
        validateNotNull(pattern, "pattern");
        
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " não atende ao formato esperado");
        }
    }
    
    /**
     * Valida se uma string corresponde a um padrão regex.
     * 
     * @param value valor a validar
     * @param regex padrão regex como string
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se não corresponde ao padrão
     */
    public static void validatePattern(String value, String regex, String fieldName) {
        validatePattern(value, Pattern.compile(regex), fieldName);
    }
    
    /**
     * Valida se um valor está em uma lista de valores permitidos.
     * 
     * @param value valor a validar
     * @param allowedValues valores permitidos
     * @param fieldName nome do campo para mensagem de erro
     * @throws IllegalArgumentException se valor não está na lista
     */
    public static void validateAllowedValues(Object value, Collection<?> allowedValues, String fieldName) {
        validateNotNull(value, fieldName);
        validateNotEmpty(allowedValues, "allowedValues");
        
        if (!allowedValues.contains(value)) {
            throw new IllegalArgumentException(
                String.format("%s deve ser um dos valores: %s", fieldName, allowedValues)
            );
        }
    }
    
    /**
     * Verifica se uma string é um email válido.
     * 
     * @param email email a verificar
     * @return true se é válido
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Verifica se uma string é um username válido.
     * 
     * @param username username a verificar
     * @return true se é válido
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    /**
     * Verifica se uma string é um telefone válido.
     * 
     * @param phone telefone a verificar
     * @return true se é válido
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Verifica se uma senha atende aos critérios de força.
     * 
     * @param password senha a verificar
     * @return true se é válida
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 && PASSWORD_PATTERN.matcher(password).matches();
    }
}