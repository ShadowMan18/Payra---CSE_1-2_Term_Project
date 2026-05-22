package codes.Client;

public class Validator {
    public static String validateName(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (!((name.charAt(i) >= 'A' && name.charAt(i) <= 'Z') || (name.charAt(i) >= 'a' && name.charAt(i) <= 'z') || name.charAt(i) == ' ' || name.charAt(i) == '.' || name.charAt(i) == '-')) {
                return "Invalid name";
            }
        }

        return "";
    }

    public static String validateEmail(String email) {
        if (!(email.length() > 10 && email.endsWith("@gmail.com"))) {
            return "Invalid email address";
        }
        else {
            String id = email.substring(0, email.length() - "@gmail.com".length());

            if (id.length() < 6) {
                return "Username is too short";
            }
            else if (id.length() > 30) {
                return "Username is too long";
            }
            else if (!(id.charAt(0) >= 'a' && id.charAt(0) <= 'z')) {
                return "Email address must start with a letter";
            }
            else {
                for(int i = 0; i < id.length(); i++) {
                    if (!((id.charAt(i) >= 'a' && id.charAt(i) <= 'z') || (id.charAt(i) >= '0' && id.charAt(i) <= '9') || id.charAt(i) == '.' || id.charAt(i) == '-' || id.charAt(i) == '_')) {
                        return "Characters must be a~z, 0~9, . _ -";
                    }
                }
            }
        }

        return "";
    }
    
    public static String validatePassword(String password) {
        int upperCase = 0;
        int lowerCase = 0;
        int specialCharacter = 0;
        int digit = 0;
        String specialCharacters = "!@#$%^&*_+-=(){}[]<>|\\/,.?:;\"'";

        if (password.length() < 8) {
            return "Password must contain at least 8 characters";
        }
        else {
            for (int i = 0; i < password.length(); i++){
                if (password.charAt(i) >= 'A' && password.charAt(i) <= 'Z') {
                    upperCase++;
                }
                else if (password.charAt(i) >= 'a' && password.charAt(i) <= 'z') {
                    lowerCase++;
                }
                else if (specialCharacters.contains(String.valueOf(password.charAt(i)))) {
                    specialCharacter++;
                }
                else if (password.charAt(i) >= '0' && password.charAt(i) <= '9') {
                    digit++;
                }
                else {
                    return "Characters must be A~Z, a~z, 0~9, !@#$%^&*_+-=(){}[]<>|\\/,.?:;\"'";
                }
            }

            if (upperCase == 0 || lowerCase == 0 || specialCharacter == 0 || digit ==0) {
                return "Password must contain uppercase, lowercase, special character and digit";
            }
        }

        return "";
    }
}
