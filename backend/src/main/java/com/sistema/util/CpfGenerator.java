package com.sistema.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerador de CPFs válidos para testes.
 * Baseado no repositório gabriel-logan/Gerador-CPF-e-CNPJ-valido (Licença MIT).
 * Implementa o algoritmo oficial de geração de CPF brasileiro com melhorias.
 * 
 * Características:
 * - Métodos estáticos para facilidade de uso
 * - Compatibilidade com validadores existentes
 * - Geração segura usando SecureRandom
 * - Suporte a formatação e limpeza de CPF
 * - Validação integrada
 * 
 * ATENÇÃO: Esta classe é destinada APENAS para testes e desenvolvimento.
 * Não deve ser usada para gerar CPFs para uso real ou produção.
 */
public final class CpfGenerator {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private CpfGenerator() {
        // Classe utilitária - construtor privado
    }
    
    /**
     * Gera um CPF válido aleatório (método principal baseado no gabriel-logan).
     * 
     * @return CPF válido apenas com números (11 dígitos)
     */
    public static String generateCpf() {
        return generateValidCpf(false);
    }
    
    /**
     * Gera um CPF válido aleatório.
     * 
     * @return CPF válido no formato xxx.xxx.xxx-xx
     */
    public static String generateValidCpf() {
        return generateValidCpf(true);
    }
    
    /**
     * Gera um CPF válido aleatório.
     * 
     * @param formatted se true, retorna formatado (xxx.xxx.xxx-xx), senão apenas números
     * @return CPF válido
     */
    public static String generateValidCpf(boolean formatted) {
        List<Integer> cpfDigits = generatePartialCpf();
        cpfDigits = generateFirstDigit(cpfDigits);
        cpfDigits = generateSecondDigit(cpfDigits);
        
        StringBuilder cpf = new StringBuilder();
        for (int digit : cpfDigits) {
            cpf.append(digit);
        }
        
        String cpfString = cpf.toString();
        return formatted ? FormatUtils.formatCpf(cpfString) : cpfString;
    }
    
    /**
     * Gera múltiplos CPFs válidos.
     * 
     * @param count quantidade de CPFs a gerar
     * @param formatted se true, retorna formatados
     * @return lista de CPFs válidos
     */
    public static List<String> generateMultipleValidCpfs(int count, boolean formatted) {
        ValidationUtils.validatePositive(count, "count");
        ValidationUtils.validateRange(count, 1, 1000, "count"); // Limite para evitar uso excessivo
        
        List<String> cpfs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cpfs.add(generateValidCpf(formatted));
        }
        return cpfs;
    }
    
    /**
     * Gera múltiplos CPFs válidos formatados.
     * 
     * @param count quantidade de CPFs a gerar
     * @return lista de CPFs válidos formatados
     */
    public static List<String> generateMultipleValidCpfs(int count) {
        return generateMultipleValidCpfs(count, true);
    }
    
    /**
     * Gera um número aleatório de 0 a 9.
     * 
     * @return número aleatório
     */
    private static int generateRandomDigit() {
        return SECURE_RANDOM.nextInt(10);
    }
    
    /**
     * Gera os 9 primeiros dígitos do CPF de forma aleatória.
     * 
     * @return lista com os 9 primeiros dígitos
     */
    private static List<Integer> generatePartialCpf() {
        List<Integer> digits = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            digits.add(generateRandomDigit());
        }
        return digits;
    }
    
    /**
     * Calcula e adiciona o primeiro dígito verificador.
     * Algoritmo baseado no artigo:
     * - Cada dígito tem peso decrescente de 10 a 2
     * - Soma = (d1*10 + d2*9 + d3*8 + ... + d9*2)
     * - Resto = Soma % 11
     * - Se resto < 2, dígito = 0, senão dígito = 11 - resto
     * 
     * @param partialCpf lista com os 9 primeiros dígitos
     * @return lista com 10 dígitos (incluindo primeiro verificador)
     */
    private static List<Integer> generateFirstDigit(List<Integer> partialCpf) {
        List<Integer> cpfWithFirstDigit = new ArrayList<>(partialCpf);
        
        int sum = 0;
        int weight = 10;
        
        // Calcula a soma ponderada
        for (int digit : partialCpf) {
            sum += digit * weight;
            weight--;
        }
        
        // Calcula o primeiro dígito verificador
        int remainder = sum % 11;
        int firstDigit = remainder < 2 ? 0 : 11 - remainder;
        
        cpfWithFirstDigit.add(firstDigit);
        return cpfWithFirstDigit;
    }
    
    /**
     * Calcula e adiciona o segundo dígito verificador.
     * Algoritmo baseado no artigo:
     * - Cada dígito (incluindo o primeiro verificador) tem peso decrescente de 11 a 2
     * - Soma = (d1*11 + d2*10 + d3*9 + ... + d10*2)
     * - Resto = Soma % 11
     * - Se resto < 2, dígito = 0, senão dígito = 11 - resto
     * 
     * @param cpfWithFirstDigit lista com 10 dígitos (incluindo primeiro verificador)
     * @return lista com 11 dígitos (CPF completo)
     */
    private static List<Integer> generateSecondDigit(List<Integer> cpfWithFirstDigit) {
        List<Integer> completeCpf = new ArrayList<>(cpfWithFirstDigit);
        
        int sum = 0;
        int weight = 11;
        
        // Calcula a soma ponderada
        for (int digit : cpfWithFirstDigit) {
            sum += digit * weight;
            weight--;
        }
        
        // Calcula o segundo dígito verificador
        int remainder = sum % 11;
        int secondDigit = remainder < 2 ? 0 : 11 - remainder;
        
        completeCpf.add(secondDigit);
        return completeCpf;
    }
    
    /**
     * Gera um CPF válido com base em uma sequência específica dos 9 primeiros dígitos.
     * Útil para testes determinísticos.
     * 
     * @param firstNineDigits string com exatamente 9 dígitos
     * @param formatted se true, retorna formatado
     * @return CPF válido baseado na sequência fornecida
     * @throws IllegalArgumentException se a sequência não tiver exatamente 9 dígitos numéricos
     */
    public static String generateCpfFromSequence(String firstNineDigits, boolean formatted) {
        ValidationUtils.validateNotBlank(firstNineDigits, "firstNineDigits");
        
        if (!firstNineDigits.matches("\\d{9}")) {
            throw new IllegalArgumentException("A sequência deve conter exatamente 9 dígitos numéricos");
        }
        
        List<Integer> digits = new ArrayList<>();
        for (char c : firstNineDigits.toCharArray()) {
            digits.add(Character.getNumericValue(c));
        }
        
        digits = generateFirstDigit(digits);
        digits = generateSecondDigit(digits);
        
        StringBuilder cpf = new StringBuilder();
        for (int digit : digits) {
            cpf.append(digit);
        }
        
        String cpfString = cpf.toString();
        return formatted ? FormatUtils.formatCpf(cpfString) : cpfString;
    }
    
    /**
     * Gera um CPF válido com base em uma sequência específica dos 9 primeiros dígitos (formatado).
     * 
     * @param firstNineDigits string com exatamente 9 dígitos
     * @return CPF válido formatado baseado na sequência fornecida
     */
    public static String generateCpfFromSequence(String firstNineDigits) {
        return generateCpfFromSequence(firstNineDigits, true);
    }
    
    /**
     * Valida se um CPF gerado por esta classe é realmente válido.
     * Usa o validador existente do projeto para verificação.
     * 
     * @param cpf CPF a validar
     * @return true se o CPF for válido
     */
    public static boolean validateGeneratedCpf(String cpf) {
        return ValidationUtils.isValidCpf(cpf);
    }
    
    /**
     * Valida se um CPF é válido (método baseado no gabriel-logan).
     * Implementa o algoritmo oficial de validação de CPF brasileiro.
     * 
     * @param cpf CPF a validar (com ou sem formatação)
     * @return true se o CPF for válido
     */
    public static boolean isValidCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }
        
        // Remove formatação
        String cleanCpf = cleanCpf(cpf);
        
        // Verifica se tem 11 dígitos
        if (cleanCpf.length() != 11 || !cleanCpf.matches("\\d{11}")) {
            return false;
        }
        
        // Verifica se não são todos os dígitos iguais
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
     * Formata um CPF no padrão xxx.xxx.xxx-xx (método baseado no gabriel-logan).
     * 
     * @param cpf CPF apenas com números (11 dígitos)
     * @return CPF formatado ou string vazia se inválido
     */
    public static String formatCpf(String cpf) {
        if (cpf == null) {
            return "";
        }
        
        String cleanCpf = cleanCpf(cpf);
        
        if (cleanCpf.length() != 11 || !cleanCpf.matches("\\d{11}")) {
            return "";
        }
        
        return String.format("%s.%s.%s-%s",
            cleanCpf.substring(0, 3),
            cleanCpf.substring(3, 6),
            cleanCpf.substring(6, 9),
            cleanCpf.substring(9, 11)
        );
    }
    
    /**
     * Remove formatação do CPF, deixando apenas números (método baseado no gabriel-logan).
     * 
     * @param cpf CPF com ou sem formatação
     * @return CPF apenas com números
     */
    public static String cleanCpf(String cpf) {
        if (cpf == null) {
            return "";
        }
        
        return cpf.replaceAll("[^0-9]", "");
    }
    
    /**
     * Gera um exemplo de CPF válido para demonstração.
     * Sempre retorna o mesmo CPF baseado na sequência 123456789.
     * 
     * @return CPF de exemplo: 123.456.789-09
     */
    public static String generateExampleCpf() {
        return generateCpfFromSequence("123456789", true);
    }
    
    /**
     * Método utilitário para testes - gera e valida um CPF.
     * 
     * @return objeto com CPF gerado e resultado da validação
     */
    public static CpfGenerationResult generateAndValidate() {
        String cpf = generateValidCpf(true);
        String cpfUnformatted = generateValidCpf(false);
        boolean isValid = validateGeneratedCpf(cpf);
        
        return new CpfGenerationResult(cpf, cpfUnformatted, isValid);
    }
    
    /**
     * Classe para resultado de geração e validação de CPF.
     */
    public static class CpfGenerationResult {
        private final String formattedCpf;
        private final String unformattedCpf;
        private final boolean isValid;
        
        public CpfGenerationResult(String formattedCpf, String unformattedCpf, boolean isValid) {
            this.formattedCpf = formattedCpf;
            this.unformattedCpf = unformattedCpf;
            this.isValid = isValid;
        }
        
        public String getFormattedCpf() { return formattedCpf; }
        public String getUnformattedCpf() { return unformattedCpf; }
        public boolean isValid() { return isValid; }
        
        @Override
        public String toString() {
            return String.format("CPF: %s (sem formatação: %s) - Válido: %s", 
                               formattedCpf, unformattedCpf, isValid);
        }
    }
}