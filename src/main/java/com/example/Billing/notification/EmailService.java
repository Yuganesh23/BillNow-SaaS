package com.example.Billing.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // Need a default fallback if config is missing in properties
            message.setFrom("noreply@billnow.com");
            message.setTo(toEmail);
            message.setSubject("Your BillNow Verification Code");
            message.setText("Hello,\n\nYour OTP code is: " + otp + "\n\nThis code is valid for 5 minutes.\n\nThank you,\nBillNow Team");

            mailSender.send(message);
            log.info("OTP Email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }
    
    public void sendInvoiceEmail(String toEmail, String invoiceNumber, byte[] pdfBytes) {
        // Just mock for now
        log.info("Mock: Invoice {} sent to {}", invoiceNumber, toEmail);
    }
}
