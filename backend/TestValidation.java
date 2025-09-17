import com.sistema.util.CpfValidator;
import com.sistema.util.ValidationUtils;

public class TestValidation {
    public static void main(String[] args) {
        System.out.println("=== Teste de Validação de CPF ===");
        
        String[] cpfs = {
            "11144477735",
            "111.444.777-35",
            "12345678909",
            "123.456.789-09",
            "50455815461",
            "504.558.154-61"
        };
        
        for (String cpf : cpfs) {
            boolean validCpfValidator = CpfValidator.isValid(cpf);
            boolean validValidationUtils = ValidationUtils.isValidCpf(cpf);
            
            System.out.println("CPF: " + cpf);
            System.out.println("  CpfValidator.isValid: " + validCpfValidator);
            System.out.println("  ValidationUtils.isValidCpf: " + validValidationUtils);
            System.out.println("  Consistente: " + (validCpfValidator == validValidationUtils));
            System.out.println();
        }
        
        System.out.println("=== Teste de Validação de Email ===");
        String[] emails = {
            "teste@exemplo.com",
            "teste2@exemplo.com",
            "invalid-email",
            "test@test.com"
        };
        
        for (String email : emails) {
            boolean valid = ValidationUtils.isValidEmail(email);
            System.out.println("Email: " + email + " - Válido: " + valid);
        }
        
        System.out.println("=== Teste de Validação de Senha ===");
        String[] senhas = {
            "senha123",
            "MinhaSenh@123",
            "Password123!",
            "123",
            "password"
        };
        
        for (String senha : senhas) {
            try {
                ValidationUtils.validatePassword(senha);
                System.out.println("Senha: " + senha + " - Válida: true");
            } catch (Exception e) {
                System.out.println("Senha: " + senha + " - Válida: false (" + e.getMessage() + ")");
            }
        }
    }
}