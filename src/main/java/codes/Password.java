package codes;

public class Password {
    private static final int a = 5;
    private static final int b = 8;
    private static final int a_inv = 21;

    public static String encrypt(String pass){
        StringBuilder result = new StringBuilder();
        int l=pass.length();
        for(int i=0;i<l;i++){
            char ch=pass.charAt(i);
            if (Character.isLetter(ch)) {
                char base = Character.isUpperCase(ch) ? 'A' : 'a';
                int x = ch - base;
                int encrypted = (a * x + b) % 26;
                result.append((char)(encrypted + base));
            } else {
                result.append(ch); // Keep spaces/punctuation unchanged
            }
        }
        return result.toString();
    }

    public static String decrypt(String encrypted){
        StringBuilder result = new StringBuilder();
        int l=encrypted.length();
        for(int i=0;i<l;i++){
            char ch=encrypted.charAt(i);
            if (Character.isLetter(ch)) {
                char base = Character.isUpperCase(ch) ? 'A' : 'a';
                int y = ch - base;
                int decrypted = (a_inv * (y - b + 26)) % 26;
                result.append((char)(decrypted + base));
            } else {
                result.append(ch); // Keep spaces/punctuation unchanged
            }
        }
        return result.toString();
    }
}
