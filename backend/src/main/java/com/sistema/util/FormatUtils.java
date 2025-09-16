package com.sistema.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilitários para formatação de dados.
 * Fornece métodos estáticos para formatação comum em toda a aplicação.
 */
public final class FormatUtils {
    
    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    private static final DecimalFormat CURRENCY_FORMATTER = 
        new DecimalFormat("R$ #,##0.00");
    
    private static final DecimalFormat PERCENTAGE_FORMATTER = 
        new DecimalFormat("#,##0.00%");
    
    private static final NumberFormat NUMBER_FORMATTER = 
        NumberFormat.getNumberInstance(new Locale("pt", "BR"));
    
    private FormatUtils() {
        // Classe utilitária - construtor privado
    }
    
    /**
     * Formata uma data/hora usando o formato padrão brasileiro.
     * 
     * @param dateTime data/hora a formatar
     * @return string formatada ou null se entrada for null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DEFAULT_DATE_TIME_FORMATTER) : null;
    }
    
    /**
     * Formata uma data usando o formato padrão brasileiro.
     * 
     * @param dateTime data/hora a formatar
     * @return string formatada ou null se entrada for null
     */
    public static String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : null;
    }
    
    /**
     * Formata uma data usando o formato padrão brasileiro.
     * 
     * @param date data a formatar
     * @return string formatada ou null se entrada for null
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }
    
    /**
     * Formata uma data usando um formato customizado.
     * 
     * @param date data a formatar
     * @param pattern padrão de formatação
     * @return string formatada ou null se entrada for null
     */
    public static String formatDate(LocalDate date, String pattern) {
        if (date == null || pattern == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Formata uma data e hora usando um formato customizado.
     * 
     * @param dateTime data e hora a formatar
     * @param pattern padrão de formatação
     * @return string formatada ou null se entrada for null
     */
    public static String formatDate(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Formata uma hora usando o formato padrão brasileiro.
     * 
     * @param dateTime data/hora a formatar
     * @return string formatada ou null se entrada for null
     */
    public static String formatTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMATTER) : null;
    }
    
    /**
     * Formata uma data/hora usando o formato ISO.
     * 
     * @param dateTime data/hora a formatar
     * @return string formatada ou null se entrada for null
     */
    public static String formatIsoDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(ISO_DATE_TIME_FORMATTER) : null;
    }
    
    /**
     * Formata uma data/hora usando um formato customizado.
     * 
     * @param dateTime data/hora a formatar
     * @param pattern padrão de formatação
     * @return string formatada ou null se entrada for null
     */
    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Formata um valor monetário em reais.
     * 
     * @param value valor a formatar
     * @return string formatada ou null se entrada for null
     */
    public static String formatCurrency(Number value) {
        return value != null ? CURRENCY_FORMATTER.format(value.doubleValue()) : null;
    }
    
    /**
     * Formata um valor monetário em reais com símbolo customizado.
     * 
     * @param value valor a formatar
     * @param currencySymbol símbolo da moeda (ignorado, mantém R$)
     * @return string formatada ou null se entrada for null
     */
    public static String formatCurrency(Number value, String currencySymbol) {
        return formatCurrency(value);
    }
    
    /**
     * Formata um valor como percentual.
     * 
     * @param value valor a formatar (0.5 = 50%)
     * @return string formatada ou null se entrada for null
     */
    public static String formatPercentage(Number value) {
        return value != null ? PERCENTAGE_FORMATTER.format(value.doubleValue()) : null;
    }
    
    /**
     * Formata um número usando o formato brasileiro.
     * 
     * @param value número a formatar
     * @return string formatada ou null se entrada for null
     */
    public static String formatNumber(Number value) {
        return value != null ? NUMBER_FORMATTER.format(value) : null;
    }
    
    /**
     * Formata um número com quantidade específica de casas decimais.
     * 
     * @param value número a formatar
     * @param decimalPlaces quantidade de casas decimais
     * @return string formatada ou null se entrada for null
     */
    public static String formatNumber(Number value, int decimalPlaces) {
        if (value == null) {
            return null;
        }
        
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append(".");
            for (int i = 0; i < decimalPlaces; i++) {
                pattern.append("0");
            }
        }
        
        DecimalFormat formatter = new DecimalFormat(pattern.toString());
        return formatter.format(value.doubleValue());
    }
    
    /**
     * Formata um CPF (apenas números para formato xxx.xxx.xxx-xx).
     * 
     * @param cpf CPF a formatar
     * @return CPF formatado ou null se entrada for null/inválida
     */
    public static String formatCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        
        String cleanCpf = cpf.replaceAll("\\D", "");
        if (cleanCpf.length() != 11) {
            return cpf; // Retorna original se não tem 11 dígitos
        }
        
        return String.format("%s.%s.%s-%s",
            cleanCpf.substring(0, 3),
            cleanCpf.substring(3, 6),
            cleanCpf.substring(6, 9),
            cleanCpf.substring(9, 11)
        );
    }
    
    /**
     * Formata um CNPJ (apenas números para formato xx.xxx.xxx/xxxx-xx).
     * 
     * @param cnpj CNPJ a formatar
     * @return CNPJ formatado ou null se entrada for null/inválida
     */
    public static String formatCnpj(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        
        String cleanCnpj = cnpj.replaceAll("\\D", "");
        if (cleanCnpj.length() != 14) {
            return cnpj; // Retorna original se não tem 14 dígitos
        }
        
        return String.format("%s.%s.%s/%s-%s",
            cleanCnpj.substring(0, 2),
            cleanCnpj.substring(2, 5),
            cleanCnpj.substring(5, 8),
            cleanCnpj.substring(8, 12),
            cleanCnpj.substring(12, 14)
        );
    }
    
    /**
     * Formata um telefone brasileiro.
     * 
     * @param phone telefone a formatar
     * @return telefone formatado ou null se entrada for null
     */
    public static String formatPhone(String phone) {
        if (phone == null) {
            return null;
        }
        
        String cleanPhone = phone.replaceAll("\\D", "");
        
        // Remove código do país se presente
        if (cleanPhone.startsWith("55") && cleanPhone.length() > 11) {
            cleanPhone = cleanPhone.substring(2);
        }
        
        if (cleanPhone.length() == 10) {
            // Telefone fixo: (xx) xxxx-xxxx
            return String.format("(%s) %s-%s",
                cleanPhone.substring(0, 2),
                cleanPhone.substring(2, 6),
                cleanPhone.substring(6, 10)
            );
        } else if (cleanPhone.length() == 11) {
            // Celular: (xx) xxxxx-xxxx
            return String.format("(%s) %s-%s",
                cleanPhone.substring(0, 2),
                cleanPhone.substring(2, 7),
                cleanPhone.substring(7, 11)
            );
        }
        
        return phone; // Retorna original se não conseguir formatar
    }
    
    /**
     * Formata um CEP brasileiro.
     * 
     * @param cep CEP a formatar
     * @return CEP formatado ou null se entrada for null/inválida
     */
    public static String formatCep(String cep) {
        if (cep == null) {
            return null;
        }
        
        String cleanCep = cep.replaceAll("\\D", "");
        if (cleanCep.length() != 8) {
            return cep; // Retorna original se não tem 8 dígitos
        }
        
        return String.format("%s-%s",
            cleanCep.substring(0, 5),
            cleanCep.substring(5, 8)
        );
    }
    
    /**
     * Capitaliza a primeira letra de cada palavra.
     * 
     * @param text texto a capitalizar
     * @return texto capitalizado ou null se entrada for null
     */
    public static String capitalizeWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Remove acentos de uma string.
     * 
     * @param text texto a processar
     * @return texto sem acentos ou null se entrada for null
     */
    public static String removeAccents(String text) {
        if (text == null) {
            return null;
        }
        
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
    
    /**
     * Trunca um texto para um comprimento máximo.
     * 
     * @param text texto a truncar
     * @param maxLength comprimento máximo
     * @return texto truncado ou null se entrada for null
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Mascara um email mostrando apenas alguns caracteres.
     * 
     * @param email email a mascarar
     * @return email mascarado ou null se entrada for null
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return email; // Muito curto para mascarar
        }
        
        String maskedLocal = localPart.charAt(0) + 
                           "*".repeat(localPart.length() - 2) + 
                           localPart.charAt(localPart.length() - 1);
        
        return maskedLocal + "@" + domain;
    }
    
    /**
     * Remove todos os caracteres não numéricos de uma string.
     * 
     * @param text texto a processar
     * @return apenas números ou null se entrada for null
     */
    public static String extractNumbers(String text) {
        return text != null ? text.replaceAll("\\D", "") : null;
    }
    
    /**
     * Formata bytes em formato legível (KB, MB, GB, etc.).
     * 
     * @param bytes quantidade de bytes
     * @return string formatada
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    /**
     * Remove formatação de CPF (pontos e hífen).
     * 
     * @param cpf CPF formatado
     * @return apenas números ou null se entrada for null
     */
    public static String removeCpfFormatting(String cpf) {
        return cpf != null ? cpf.replaceAll("[.\\-]", "") : null;
    }
    
    /**
     * Remove formatação de CNPJ (pontos, barra e hífen).
     * 
     * @param cnpj CNPJ formatado
     * @return apenas números ou null se entrada for null
     */
    public static String removeCnpjFormatting(String cnpj) {
        return cnpj != null ? cnpj.replaceAll("[.\\-/]", "") : null;
    }
    
    /**
     * Remove formatação de telefone (parênteses, espaços e hífen).
     * 
     * @param phone telefone formatado
     * @return apenas números ou null se entrada for null
     */
    public static String removePhoneFormatting(String phone) {
        return phone != null ? phone.replaceAll("[()\\s\\-]", "") : null;
    }
    
    /**
     * Converte string para LocalDate.
     * 
     * @param dateString string de data no formato dd/MM/yyyy
     * @return LocalDate ou null se não conseguir converter
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Converte string para LocalDateTime.
     * 
     * @param dateTimeString string de data/hora no formato dd/MM/yyyy HH:mm:ss
     * @return LocalDateTime ou null se não conseguir converter
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(dateTimeString, DEFAULT_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Capitaliza apenas a primeira letra da string.
     * 
     * @param text texto a capitalizar
     * @return texto com primeira letra maiúscula ou null se entrada for null
     */
    public static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        return Character.toUpperCase(text.charAt(0)) + 
               (text.length() > 1 ? text.substring(1).toLowerCase() : "");
    }
    
    /**
     * Converte texto para Title Case (primeira letra de cada palavra maiúscula).
     * 
     * @param text texto a converter
     * @return texto em Title Case ou null se entrada for null
     */
    public static String toTitleCase(String text) {
        return capitalizeWords(text);
    }
    
    /**
     * Normaliza texto para busca (remove acentos e converte para minúsculas).
     * 
     * @param text texto a normalizar
     * @return texto normalizado ou null se entrada for null
     */
    public static String normalizeForSearch(String text) {
        if (text == null) {
            return null;
        }
        
        return removeAccents(text.toLowerCase().trim());
    }
    
    /**
     * Trunca texto adicionando reticências se necessário.
     * 
     * @param text texto a truncar
     * @param maxLength comprimento máximo
     * @return texto truncado com reticências ou null se entrada for null
     */
    public static String truncateWithEllipsis(String text, int maxLength) {
        return truncate(text, maxLength);
    }
    
    /**
     * Formata número decimal com precisão padrão.
     * 
     * @param value valor a formatar
     * @return string formatada
     */
    public static String formatDecimal(double value) {
        return String.format("%.2f", value);
    }
    
    /**
     * Formata número decimal com precisão específica.
     * 
     * @param value valor a formatar
     * @param decimalPlaces número de casas decimais
     * @return string formatada
     */
    public static String formatDecimal(double value, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f", value);
    }
    
    /**
     * Formata tamanho de arquivo em formato legível.
     * 
     * @param sizeInBytes tamanho em bytes
     * @return string formatada
     */
    public static String formatFileSize(long sizeInBytes) {
        return formatBytes(sizeInBytes);
    }
    
    /**
     * Sobrecarga para formatFileSize com int.
     * 
     * @param sizeInBytes tamanho em bytes
     * @return string formatada
     */
    public static String formatFileSize(int sizeInBytes) {
        return formatBytes(sizeInBytes);
    }
}