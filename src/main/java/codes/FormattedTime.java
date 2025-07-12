package codes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class FormattedTime {
    public static String getTime(int type) {
        LocalDateTime now = LocalDateTime.now();
        String formatted = "";
        if (type == 1) {
            formatted = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        else if (type == 2) {
            formatted = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }
        return formatted;
    }
}
