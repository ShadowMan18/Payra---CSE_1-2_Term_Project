package codes.Server;

public class EncryptionProcessor {
    private static final int a = 5;
    private static final int b = 8;
    private static final int a_inv = 21;

    public static String encrypt(String text){
        if (text == null || text.isEmpty()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        int l=text.length();
        for(int i=0;i<l;i++){
            char ch=text.charAt(i);
            if (Character.isLetter(ch)) {
                char base = Character.isUpperCase(ch) ? 'A' : 'a';
                int x = ch - base;
                int encrypted = (a * x + b) % 26;
                result.append((char)(encrypted + base));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static String decrypt(String encryptedText){
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        int l=encryptedText.length();
        for(int i=0;i<l;i++){
            char ch=encryptedText.charAt(i);
            if (Character.isLetter(ch)) {
                char base = Character.isUpperCase(ch) ? 'A' : 'a';
                int y = ch - base;
                int decrypted = (a_inv * (y - b + 26)) % 26;
                result.append((char)(decrypted + base));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
