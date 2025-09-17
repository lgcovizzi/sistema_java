import com.sistema.util.ValidationUtils;

public class TestPasswordValidation {
    public static void main(String[] args) {
        String password = "MinhaSenh@123";
        System.out.println("Senha: " + password);
        
        try {
            ValidationUtils.validatePassword(password);
            System.out.println("Senha válida!");
        } catch (Exception e) {
            System.out.println("Erro na validação: " + e.getMessage());
        }
        
        System.out.println("isValidPassword: " + ValidationUtils.isValidPassword(password));
        
        // Teste com senha simples
        String senhaSimples = "123456";
        System.out.println("\nSenha simples: " + senhaSimples);
        try {
            ValidationUtils.validatePassword(senhaSimples);
            System.out.println("Senha simples válida!");
        } catch (Exception e) {
            System.out.println("Erro na validação da senha simples: " + e.getMessage());
        }
    }
}