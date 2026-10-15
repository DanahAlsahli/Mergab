package com.example.mergab.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final QrCodeService qrCodeService;

    @Value("${spring.mail.username}")
    private String from;

    // Send welcome email with QR ticket (runs in background)
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String qrToken) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);

            String html = templateEngine.process("email/welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, "Mirqab | مرقاب");
            helper.setTo(toEmail);
            helper.setSubject("مرحباً بك في مرقاب | Welcome to Mirqab");
            helper.setText(html, true);

            // QR image (same qrToken used in check-in/check-out) - must be after setText
            byte[] qrImage = qrCodeService.generateQrCode(qrToken);
            helper.addInline("qrCode", new ByteArrayResource(qrImage), "image/png");

            // Mirqab logo from static/img
            helper.addInline("logo", new ClassPathResource("static/img/logo-white.png"), "image/png");

            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);

        } catch (Exception e) {
            // Email failure should not break registration
            log.error("Failed to send welcome email to {}", toEmail, e);
        }
    }

}
