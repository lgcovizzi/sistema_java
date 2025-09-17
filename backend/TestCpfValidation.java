import com.sistema.util.CpfValidator;
import com.sistema.util.ValidationUtils;

public class TestCpfValidation {
    public static void main(String[] args) {
        String cpf = "504.558.154-61";
        System.out.println("CPF: " + cpf);
        System.out.println("CpfValidator.isValid: " + CpfValidator.isValid(cpf));
        System.out.println("ValidationUtils.isValidCpf: " + ValidationUtils.isValidCpf(cpf));
        
        // Teste sem formatação
        String cpfSemFormatacao = "50455815461";
        System.out.println("\nCPF sem formatação: " + cpfSemFormatacao);
        System.out.println("CpfValidator.isValid: " + CpfValidator.isValid(cpfSemFormatacao));
        System.out.println("ValidationUtils.isValidCpf: " + ValidationUtils.isValidCpf(cpfSemFormatacao));
    }
}