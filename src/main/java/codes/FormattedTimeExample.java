package codes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class FormattedTimeExample {
    public static String getTime() {
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return formatted;
    }
}
