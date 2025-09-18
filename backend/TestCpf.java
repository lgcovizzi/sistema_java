import com.sistema.util.CpfGenerator;

public class TestCpf {
    public static void main(String[] args) {
        // Testando CPFs conhecidos válidos
        String[] testCpfs = {
            "11144477735",
            "52998224725", 
            "85914200166",
            "12345678909",
            "98765432100",
            "11111111111",
            "00000000000",
            "12312312312",
            "45645645645",
            "78978978978"
        };
        
        for (String cpf : testCpfs) {
            boolean isValid = CpfGenerator.isValidCpf(cpf);
            System.out.println("CPF: " + cpf + " - Válido: " + isValid);
        }
        
        // Gerando CPFs válidos
        System.out.println("\nCPFs válidos gerados:");
        for (int i = 0; i < 5; i++) {
            String validCpf = CpfGenerator.generateCpf();
            System.out.println("CPF " + (i+1) + ": " + validCpf + " - Válido: " + CpfGenerator.isValidCpf(validCpf));
        }
    }
}