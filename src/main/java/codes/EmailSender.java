package codes;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender {
    public static void sendConfirmationEmail(String emailAddress) {
        final String fromEmail = "shadmansami3s@gmail.com";
        final String appPassword = "sncp euce gmgv ovfw";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromEmail));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
            msg.setSubject("Confirmation");
            msg.setText("Hello");

            Transport.send(msg);
            System.out.println("Mail sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public static void sendVerificationCode(String emailAddress, int code) {

    }

    public static void main(String[] args) {
        EmailSender.sendConfirmationEmail("homosapiens1863@gmail.com");
    }
}


