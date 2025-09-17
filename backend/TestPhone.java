import com.sistema.util.ValidationUtils;
import java.util.regex.Pattern;

public class TestPhone {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+55\\s?)?(\\(\\d{2}\\)|\\d{2})\\s?\\d{4,5}[-\\s]?\\d{4}$");
    
    public static void main(String[] args) {
        String phone = "1234567890";
        System.out.println("Phone: " + phone);
        System.out.println("Is valid: " + ValidationUtils.isValidPhone(phone));
        
        // Teste do regex
        System.out.println("Regex match: " + PHONE_PATTERN.matcher(phone).matches());
        
        // Teste detalhado
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)\\+]", "");
        System.out.println("Clean phone: " + cleanPhone);
        System.out.println("Length: " + cleanPhone.length());
        
        if (cleanPhone.startsWith("55") && cleanPhone.length() > 11) {
            cleanPhone = cleanPhone.substring(2);
            System.out.println("After removing country code: " + cleanPhone);
        }
        
        System.out.println("Final length: " + cleanPhone.length());
        System.out.println("Is numeric: " + cleanPhone.matches("\\d+"));
        
        if (cleanPhone.length() == 10 || cleanPhone.length() == 11) {
            String areaCode = cleanPhone.substring(0, 2);
            System.out.println("Area code: " + areaCode);
            try {
                int code = Integer.parseInt(areaCode);
                System.out.println("Area code valid: " + (code >= 11 && code <= 99));
            } catch (NumberFormatException e) {
                System.out.println("Area code invalid: " + e.getMessage());
            }
        }
    }
}