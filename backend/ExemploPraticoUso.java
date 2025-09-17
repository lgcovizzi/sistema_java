import com.sistema.util.CpfGenerator;
import com.sistema.util.ValidationUtils;
import java.util.List;

/**
 * Exemplo prático de uso do CpfGenerator no contexto do sistema.
 * Demonstra diferentes cenários de uso para testes e desenvolvimento.
 */
public class ExemploPraticoUso {
    
    public static void main(String[] args) {
        System.out.println("=== Exemplo Prático de Uso do CpfGenerator ===\n");
        
        // Cenário 1: Criação de usuários de teste
        System.out.println("1. Criação de usuários de teste:");
        criarUsuariosTeste();
        System.out.println();
        
        // Cenário 2: Teste de validação de CPF
        System.out.println("2. Teste de validação de CPF:");
        testarValidacaoCpf();
        System.out.println();
        
        // Cenário 3: Geração para testes automatizados
        System.out.println("3. Geração para testes automatizados:");
        gerarParaTestesAutomatizados();
        System.out.println();
        
        // Cenário 4: Demonstração do algoritmo
        System.out.println("4. Demonstração do algoritmo:");
        demonstrarAlgoritmo();
    }
    
    /**
     * Simula a criação de usuários de teste com CPFs válidos
     */
    private static void criarUsuariosTeste() {
        String[] nomes = {"João Silva", "Maria Santos", "Pedro Oliveira"};
        
        for (String nome : nomes) {
            String cpf = CpfGenerator.generateValidCpf();
            String email = nome.toLowerCase().replace(" ", ".") + "@teste.com";
            
            System.out.println("Usuário: " + nome);
            System.out.println("Email: " + email);
            System.out.println("CPF: " + cpf);
            System.out.println("CPF Válido: " + ValidationUtils.isValidCpf(cpf));
            System.out.println("---");
        }
    }
    
    /**
     * Testa a validação de CPFs gerados vs CPFs inválidos
     */
    private static void testarValidacaoCpf() {
        // CPFs válidos gerados
        List<String> cpfsValidos = CpfGenerator.generateMultipleValidCpfs(3);
        System.out.println("CPFs válidos gerados:");
        for (String cpf : cpfsValidos) {
            System.out.println(cpf + " - Válido: " + ValidationUtils.isValidCpf(cpf));
        }
        
        // CPFs inválidos para comparação
        String[] cpfsInvalidos = {"111.111.111-11", "000.000.000-00", "123.456.789-10"};
        System.out.println("\nCPFs inválidos para comparação:");
        for (String cpf : cpfsInvalidos) {
            System.out.println(cpf + " - Válido: " + ValidationUtils.isValidCpf(cpf));
        }
    }
    
    /**
     * Gera CPFs para uso em testes automatizados
     */
    private static void gerarParaTestesAutomatizados() {
        System.out.println("CPFs para testes automatizados:");
        
        // CPF sempre igual para testes determinísticos
        String cpfExemplo = CpfGenerator.generateExampleCpf();
        System.out.println("CPF de exemplo (sempre igual): " + cpfExemplo);
        
        // CPFs sem formatação para testes de API
        System.out.println("\nCPFs sem formatação para APIs:");
        for (int i = 0; i < 3; i++) {
            String cpf = CpfGenerator.generateValidCpf(false);
            System.out.println(cpf + " (11 dígitos)");
        }
        
        // Resultado completo com informações detalhadas
        System.out.println("\nResultado completo para debugging:");
        CpfGenerator.CpfGenerationResult result = CpfGenerator.generateAndValidate();
        System.out.println(result);
    }
    
    /**
     * Demonstra o funcionamento do algoritmo passo a passo
     */
    private static void demonstrarAlgoritmo() {
        String sequencia = "123456789";
        System.out.println("Demonstração do algoritmo com sequência: " + sequencia);
        
        // Gera CPF baseado na sequência
        String cpfCompleto = CpfGenerator.generateCpfFromSequence(sequencia);
        System.out.println("CPF gerado: " + cpfCompleto);
        
        // Verifica se está correto
        boolean valido = ValidationUtils.isValidCpf(cpfCompleto);
        System.out.println("Válido: " + valido);
        
        // Mostra o CPF sem formatação
        String cpfSemFormatacao = cpfCompleto.replaceAll("[^0-9]", "");
        System.out.println("Sem formatação: " + cpfSemFormatacao);
        
        System.out.println("\nEste CPF pode ser usado para:");
        System.out.println("- Testes de registro de usuário");
        System.out.println("- Validação de formulários");
        System.out.println("- Testes de API");
        System.out.println("- Desenvolvimento e debugging");
    }
}