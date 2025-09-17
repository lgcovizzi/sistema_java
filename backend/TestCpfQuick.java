import com.sistema.util.CpfValidator;
import com.sistema.util.ValidationUtils;

public class TestCpfQuick {
    public static void main(String[] args) {
        String cpf = "11144477735";
        System.out.println("CPF: " + cpf);
        System.out.println("CpfValidator.isValid: " + CpfValidator.isValid(cpf));
        System.out.println("ValidationUtils.isValidCpf: " + ValidationUtils.isValidCpf(cpf));
    }
}