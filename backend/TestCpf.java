import com.sistema.util.CpfValidator;

public class TestCpf {
    public static void main(String[] args) {
        String cpf1 = "22255588896";
        String cpf2 = "33366699914";
        
        System.out.println("CPF " + cpf1 + " válido: " + CpfValidator.isValid(cpf1));
        System.out.println("CPF " + cpf2 + " válido: " + CpfValidator.isValid(cpf2));
        
        // Teste com CPFs conhecidamente válidos
        System.out.println("CPF 11144477735 válido: " + CpfValidator.isValid("11144477735"));
        System.out.println("CPF 12345678909 válido: " + CpfValidator.isValid("12345678909"));
    }
}