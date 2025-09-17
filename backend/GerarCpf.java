import com.sistema.util.CpfGenerator;

public class GerarCpf {
    public static void main(String[] args) {
        String cpf = CpfGenerator.generateValidCpf();
        System.out.println(cpf);
    }
}