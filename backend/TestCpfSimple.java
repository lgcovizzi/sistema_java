public class TestCpfSimple {
    public static void main(String[] args) {
        String cpf = "11144477735";
        System.out.println("Testando CPF: " + cpf);
        
        // Simular validação manual
        String cleanCpf = cpf.replaceAll("[^0-9]", "");
        System.out.println("CPF limpo: " + cleanCpf);
        System.out.println("Comprimento: " + cleanCpf.length());
        
        // Verificar se todos os dígitos são iguais
        boolean allSame = cleanCpf.matches("(\\d)\\1{10}");
        System.out.println("Todos os dígitos iguais: " + allSame);
        
        // Calcular primeiro dígito
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cleanCpf.charAt(i)) * (10 - i);
        }
        int firstDigit = 11 - (sum % 11);
        if (firstDigit >= 10) {
            firstDigit = 0;
        }
        System.out.println("Primeiro dígito calculado: " + firstDigit);
        System.out.println("Primeiro dígito no CPF: " + Character.getNumericValue(cleanCpf.charAt(9)));
        
        // Calcular segundo dígito
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cleanCpf.charAt(i)) * (11 - i);
        }
        int secondDigit = 11 - (sum % 11);
        if (secondDigit >= 10) {
            secondDigit = 0;
        }
        System.out.println("Segundo dígito calculado: " + secondDigit);
        System.out.println("Segundo dígito no CPF: " + Character.getNumericValue(cleanCpf.charAt(10)));
        
        boolean valid = Character.getNumericValue(cleanCpf.charAt(9)) == firstDigit &&
                       Character.getNumericValue(cleanCpf.charAt(10)) == secondDigit;
        System.out.println("CPF válido: " + valid);
    }
}