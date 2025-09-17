import com.sistema.util.CpfGenerator;
import com.sistema.util.ValidationUtils;
import java.util.List;

/**
 * Teste simples para verificar o funcionamento do CpfGenerator.
 * Execute com: javac -cp "src/main/java" TestCpfGenerator.java && java -cp ".:src/main/java" TestCpfGenerator
 */
public class TestCpfGenerator {
    
    public static void main(String[] args) {
        System.out.println("=== Teste do Gerador de CPF ===\n");
        
        // Teste 1: CPF de exemplo (sempre o mesmo)
        System.out.println("1. Teste do CPF de exemplo:");
        String exampleCpf = CpfGenerator.generateExampleCpf();
        System.out.println("CPF de exemplo: " + exampleCpf);
        System.out.println("Válido: " + ValidationUtils.isValidCpf(exampleCpf));
        System.out.println();
        
        // Teste 2: Geração de CPF aleatório
        System.out.println("2. Teste de geração aleatória:");
        for (int i = 1; i <= 5; i++) {
            String cpf = CpfGenerator.generateValidCpf();
            boolean isValid = ValidationUtils.isValidCpf(cpf);
            System.out.println("CPF " + i + ": " + cpf + " - Válido: " + isValid);
        }
        System.out.println();
        
        // Teste 3: Geração sem formatação
        System.out.println("3. Teste de geração sem formatação:");
        for (int i = 1; i <= 3; i++) {
            String cpf = CpfGenerator.generateValidCpf(false);
            boolean isValid = ValidationUtils.isValidCpf(cpf);
            System.out.println("CPF " + i + ": " + cpf + " - Válido: " + isValid);
        }
        System.out.println();
        
        // Teste 4: Geração baseada em sequência específica
        System.out.println("4. Teste de geração baseada em sequência:");
        String[] sequences = {"123456789", "987654321", "111111111", "000000000"};
        for (String seq : sequences) {
            String cpf = CpfGenerator.generateCpfFromSequence(seq);
            boolean isValid = ValidationUtils.isValidCpf(cpf);
            System.out.println("Sequência " + seq + " -> CPF: " + cpf + " - Válido: " + isValid);
        }
        System.out.println();
        
        // Teste 5: Geração múltipla
        System.out.println("5. Teste de geração múltipla:");
        List<String> multipleCpfs = CpfGenerator.generateMultipleValidCpfs(3);
        for (int i = 0; i < multipleCpfs.size(); i++) {
            String cpf = multipleCpfs.get(i);
            boolean isValid = ValidationUtils.isValidCpf(cpf);
            System.out.println("CPF " + (i + 1) + ": " + cpf + " - Válido: " + isValid);
        }
        System.out.println();
        
        // Teste 6: Geração e validação combinada
        System.out.println("6. Teste de geração e validação combinada:");
        CpfGenerator.CpfGenerationResult result = CpfGenerator.generateAndValidate();
        System.out.println(result);
        System.out.println();
        
        // Teste 7: Verificação do algoritmo com exemplo do artigo
        System.out.println("7. Verificação do algoritmo com exemplo do artigo:");
        String articleExample = CpfGenerator.generateCpfFromSequence("123456789");
        System.out.println("CPF baseado no exemplo do artigo (123456789): " + articleExample);
        System.out.println("Deve ser: 123.456.789-09");
        System.out.println("Correto: " + articleExample.equals("123.456.789-09"));
        System.out.println("Válido: " + ValidationUtils.isValidCpf(articleExample));
        
        System.out.println("\n=== Teste concluído ===");
    }
}