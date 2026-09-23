package com.nfcplatform.common.mail;

import com.nfcplatform.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin, template-ready wrapper around Spring Mail. Send failures are logged and
 * swallowed so that flows like "forgot password" never leak whether an email
 * exists via an error response, and never fail hard in local/dev environments
 * without a real SMTP server configured.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        send(toEmail, "Reset your nfc-platform password",
                "We received a request to reset your password. Use the link below (valid for 1 hour):\n\n"
                        + resetUrl + "\n\nIf you did not request this, you can ignore this email.");
    }

    public void sendAccountCreatedEmail(String toEmail, String loginUrl) {
        send(toEmail, "Your nfc-platform account is ready",
                "An account has been created for you. Sign in here: " + loginUrl);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
