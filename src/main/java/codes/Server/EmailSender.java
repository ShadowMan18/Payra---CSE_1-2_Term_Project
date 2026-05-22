package codes.Server;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class EmailSender {
    private static final String FROM_EMAIL = "noreply.payra2025@gmail.com";
    private static final String APP_PASSWORD = "lnup tsxi adeu ptgy";

    public static void sendEmail(String emailAddress, String subject, String message) {
        Session session = getMailSession();

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, "Payra"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
            msg.setSubject(subject);
            msg.setText(message);
            Transport.send(msg);
            System.out.println("Mail sent successfully to " + emailAddress);
        }
        catch (MessagingException | UnsupportedEncodingException e) {}
    }

    private static Session getMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        session.setDebug(true);
        return session;
    }
}
