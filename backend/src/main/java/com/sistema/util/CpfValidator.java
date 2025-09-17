package com.sistema.util;

import java.util.regex.Pattern;

/**
 * Utilitário para validação de CPF brasileiro.
 * Baseado no repositório gabriel-logan/Gerador-CPF-e-CNPJ-valido (Licença MIT).
 * Implementa o algoritmo oficial de validação do CPF.
 * 
 * Compatível com CpfGenerator para uso em testes e validação.
 */
public class CpfValidator {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");

    /**
     * Valida se o CPF é válido.
     * 
     * @param cpf o CPF a ser validado
     * @return true se o CPF for válido
     */
    public static boolean isValid(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }

        // Remove formatação
        String cleanCpf = cpf.replaceAll("[^0-9]", "");

        // Verifica se tem 11 dígitos
        if (cleanCpf.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais (CPF inválido)
        if (cleanCpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        // Calcula o primeiro dígito verificador
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cleanCpf.charAt(i)) * (10 - i);
        }
        int firstDigit = 11 - (sum % 11);
        if (firstDigit >= 10) {
            firstDigit = 0;
        }

        // Verifica o primeiro dígito
        if (Character.getNumericValue(cleanCpf.charAt(9)) != firstDigit) {
            return false;
        }

        // Calcula o segundo dígito verificador
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cleanCpf.charAt(i)) * (11 - i);
        }
        int secondDigit = 11 - (sum % 11);
        if (secondDigit >= 10) {
            secondDigit = 0;
        }

        // Verifica o segundo dígito
        return Character.getNumericValue(cleanCpf.charAt(10)) == secondDigit;
    }

    /**
     * Formata o CPF com pontos e hífen.
     * 
     * @param cpf o CPF a ser formatado
     * @return CPF formatado (xxx.xxx.xxx-xx)
     */
    public static String format(String cpf) {
        if (cpf == null) {
            return null;
        }

        String cleanCpf = cpf.replaceAll("[^0-9]", "");
        
        if (cleanCpf.length() != 11) {
            return cpf; // Retorna original se não tiver 11 dígitos
        }

        return cleanCpf.substring(0, 3) + "." +
               cleanCpf.substring(3, 6) + "." +
               cleanCpf.substring(6, 9) + "-" +
               cleanCpf.substring(9, 11);
    }

    /**
     * Remove a formatação do CPF, deixando apenas números.
     * 
     * @param cpf o CPF formatado
     * @return CPF apenas com números
     */
    public static String clean(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^0-9]", "");
    }

    /**
     * Mascara o CPF para exibição, mostrando apenas os primeiros 3 e últimos 2 dígitos.
     * 
     * @param cpf o CPF a ser mascarado
     * @return CPF mascarado (xxx.***.**-xx)
     */
    public static String mask(String cpf) {
        if (cpf == null) {
            return null;
        }

        String cleanCpf = clean(cpf);
        
        if (cleanCpf.length() != 11) {
            return "***.***.***-**";
        }

        return cleanCpf.substring(0, 3) + ".***.**-" + cleanCpf.substring(9, 11);
    }
}