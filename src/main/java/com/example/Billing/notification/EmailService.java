package com.example.Billing.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:noreply@billnow.com}")
    private String fromAddress;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Need a default fallback if config is missing in properties
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your BillNow Verification Code");
            message.setText("Hello,\n\nYour OTP code is: " + otp + "\n\nThis code is valid for 5 minutes.\n\nThank you,\nBillNow Team");

            mailSender.send(message);
            log.info("OTP email sent");
        } catch (Exception e) {
            log.error("Failed to send OTP email", e);
            throw new IllegalStateException("Unable to send verification email", e);
        }
    }
    
    public void sendInvoiceEmail(String toEmail, String invoiceNumber, byte[] pdfBytes) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Invoice " + invoiceNumber + " from BillNow");
            helper.setText("Hello,\n\nPlease find invoice " + invoiceNumber + " attached.\n\nThank you.");
            helper.addAttachment("invoice-" + invoiceNumber + ".pdf", new ByteArrayResource(pdfBytes), "application/pdf");
            mailSender.send(message);
            log.info("Invoice email sent for {}", invoiceNumber);
        } catch (Exception exception) {
            log.error("Failed to send invoice email for {}", invoiceNumber, exception);
            throw new IllegalStateException("Unable to send invoice email", exception);
        }
    }
}
