import com.sistema.util.CpfGenerator;
import com.sistema.util.ValidationUtils;

public class TestCpfValidation {
    public static void main(String[] args) {
        String cpf = "12345678909";
        System.out.println("=== TESTE DO CPF USADO NOS TESTES ===");
        System.out.println("CPF: " + cpf);
        System.out.println("É válido (CpfGenerator): " + CpfGenerator.isValidCpf(cpf));
        System.out.println("É válido (ValidationUtils): " + ValidationUtils.isValidCpf(cpf));
        
        // Testar outros CPFs comuns em testes
        String[] testCpfs = {
            "12345678909",
            "11144477735", 
            "12345678901",
            "99999999999",
            "00000000000"
        };
        
        System.out.println("\n=== TESTE DE VÁRIOS CPFs ===");
        for (String testCpf : testCpfs) {
            boolean validCpfGen = CpfGenerator.isValidCpf(testCpf);
            boolean validValidationUtils = ValidationUtils.isValidCpf(testCpf);
            System.out.println("CPF: " + testCpf + 
                " | CpfGenerator: " + validCpfGen + 
                " | ValidationUtils: " + validValidationUtils +
                " | Concordam: " + (validCpfGen == validValidationUtils));
        }
        
        // Gerar um CPF válido para substituir
        System.out.println("\n=== CPF VÁLIDO GERADO ===");
        String validCpf = CpfGenerator.generateCpf();
        System.out.println("CPF válido gerado: " + validCpf);
        System.out.println("É válido (CpfGenerator): " + CpfGenerator.isValidCpf(validCpf));
        System.out.println("É válido (ValidationUtils): " + ValidationUtils.isValidCpf(validCpf));
    }
}