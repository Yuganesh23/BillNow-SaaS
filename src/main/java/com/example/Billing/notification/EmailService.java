package com.example.Billing.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${resend.from-email:XenBill <billing@xenfirm.com>}")
    private String fromEmail;

    // =========================================================================
    // 1. 🔐 PASSWORD RESET LINKS & VERIFICATION
    // =========================================================================

    public void sendPasswordResetEmail(String toEmail, String userName, String resetUrl) {
        if (toEmail == null || toEmail.isBlank()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Reset Your XenBill Password");

            String displayName = (userName != null && !userName.isBlank()) ? userName : "Valued User";
            String link = (resetUrl != null && !resetUrl.isBlank()) ? resetUrl : "https://xenbill.web.app/forgot-password";

            String htmlContent = "<!DOCTYPE html>"
                    + "<html><head><meta charset='UTF-8'></head><body style='font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif; background-color:#F8FAFC; margin:0; padding:24px; color:#1E293B;'>"
                    + "<div style='max-width:540px; margin:0 auto; background:#FFFFFF; border-radius:12px; border:1px solid #E2E8F0; padding:32px; box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);'>"
                    + "<div style='text-align:center; margin-bottom:24px;'>"
                    + "  <div style='display:inline-block; background:#4F46E5; color:#ffffff; font-weight:800; font-size:22px; width:44px; height:44px; line-height:44px; border-radius:10px;'>B</div>"
                    + "  <h2 style='margin:12px 0 4px; color:#0F172A; font-size:22px; font-weight:700;'>Reset Your Password</h2>"
                    + "  <p style='color:#64748B; font-size:14px; margin:0;'>XenBill Cloud SaaS</p>"
                    + "</div>"
                    + "<p style='font-size:15px; line-height:1.6;'>Hello <strong>" + displayName + "</strong>,</p>"
                    + "<p style='font-size:14px; line-height:1.6; color:#475569;'>We received a request to reset the password for your XenBill account (<strong>" + toEmail + "</strong>). Click the button below to set a new password:</p>"
                    + "<div style='text-align:center; margin:32px 0;'>"
                    + "  <a href='" + link + "' style='background:#4F46E5; color:#ffffff; padding:14px 32px; text-decoration:none; border-radius:8px; font-weight:700; font-size:15px; display:inline-block; box-shadow:0 4px 6px -1px rgba(79,70,229,0.3);'>Click Here to Set New Password</a>"
                    + "</div>"
                    + "<p style='font-size:13px; color:#64748B; line-height:1.6;'>If the button doesn't work, copy and paste this link into your browser:<br/><a href='" + link + "' style='color:#4F46E5; word-break:break-all;'>" + link + "</a></p>"
                    + "<p style='font-size:12px; color:#94A3B8; line-height:1.5; margin-top:24px;'>This link is valid for 1 hour. If you did not request a password reset, you can safely ignore this email.</p>"
                    + "<hr style='border:none; border-top:1px solid #E2E8F0; margin:24px 0;'/>"
                    + "<p style='font-size:12px; color:#94A3B8; text-align:center; margin:0;'>XenBill &bull; <a href='https://xenbill.web.app' style='color:#4F46E5; text-decoration:none;'>xenbill.web.app</a></p>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset email sent via Resend to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String userName, String resetUrl, String otp) {
        sendPasswordResetEmail(toEmail, userName, resetUrl);
    }

    public void sendOtpEmail(String toEmail, String otp) {
        String resetUrl = "https://xenbill.web.app/forgot-password?email=" + toEmail;
        sendPasswordResetEmail(toEmail, "User", resetUrl);
    }

    // =========================================================================
    // 2. 👤 WELCOME EMAILS ON NEW SHOP REGISTRATION
    // =========================================================================

    public void sendWelcomeEmail(String toEmail, String ownerName, String shopName) {
        if (toEmail == null || toEmail.isBlank()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🎉 Welcome to XenBill, " + shopName + "!");

            String displayName = (ownerName != null && !ownerName.isBlank()) ? ownerName : "Shop Owner";
            String storeTitle = (shopName != null && !shopName.isBlank()) ? shopName : "Your Shop";

            String htmlContent = "<!DOCTYPE html>"
                    + "<html><head><meta charset='UTF-8'></head><body style='font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif; background-color:#F8FAFC; margin:0; padding:24px; color:#1E293B;'>"
                    + "<div style='max-width:540px; margin:0 auto; background:#FFFFFF; border-radius:12px; border:1px solid #E2E8F0; padding:32px; box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);'>"
                    + "<div style='text-align:center; margin-bottom:24px;'>"
                    + "  <div style='display:inline-block; background:#10B981; color:#ffffff; font-weight:800; font-size:22px; width:44px; height:44px; line-height:44px; border-radius:10px;'>✓</div>"
                    + "  <h2 style='margin:12px 0 4px; color:#0F172A; font-size:22px; font-weight:700;'>Welcome to XenBill!</h2>"
                    + "  <p style='color:#64748B; font-size:14px; margin:0;'>Your cloud billing and GST engine is live</p>"
                    + "</div>"
                    + "<p style='font-size:15px; line-height:1.6;'>Hello <strong>" + displayName + "</strong>,</p>"
                    + "<p style='font-size:14px; line-height:1.6; color:#475569;'>Congratulations on launching <strong>" + storeTitle + "</strong> on XenBill! Your account is now fully active with a 14-day free trial.</p>"
                    + "<div style='background:#F1F5F9; border-radius:10px; padding:18px; margin:20px 0;'>"
                    + "  <h4 style='margin:0 0 10px; color:#0F172A; font-size:14px;'>Quick Next Steps:</h4>"
                    + "  <ul style='margin:0; padding-left:20px; font-size:13px; color:#475569; line-height:1.7;'>"
                    + "    <li>📦 <strong>Add Inventory:</strong> Add your products, HSN codes, and GST rates.</li>"
                    + "    <li>🧾 <strong>Create Invoices:</strong> Generate professional GST tax invoices in seconds.</li>"
                    + "    <li>📱 <strong>Direct Sharing:</strong> Send bills directly to WhatsApp & Email.</li>"
                    + "    <li>📊 <strong>Analytics:</strong> Monitor daily sales, profits, and top-selling goods.</li>"
                    + "  </ul>"
                    + "</div>"
                    + "<div style='text-align:center; margin:28px 0;'>"
                    + "  <a href='https://xenbill.web.app/dashboard' style='background:#4F46E5; color:#ffffff; padding:12px 28px; text-decoration:none; border-radius:8px; font-weight:600; font-size:14px; display:inline-block;'>Open XenBill Dashboard</a>"
                    + "</div>"
                    + "<p style='font-size:13px; color:#64748B; line-height:1.5;'>Need assistance or have questions? Simply reply to this email or reach us anytime at support@xenfirm.com.</p>"
                    + "<hr style='border:none; border-top:1px solid #E2E8F0; margin:24px 0;'/>"
                    + "<p style='font-size:12px; color:#94A3B8; text-align:center; margin:0;'>XenBill &bull; <a href='https://xenbill.web.app' style='color:#4F46E5; text-decoration:none;'>xenbill.web.app</a></p>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Welcome email sent via Resend to {} for shop {}", toEmail, shopName);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    // =========================================================================
    // 3. 📦 LOW-STOCK ALERTS FOR SHOP OWNERS
    // =========================================================================

    public void sendLowStockAlertEmail(String ownerEmail, String shopName, String productName, Double currentStock, Double threshold) {
        if (ownerEmail == null || ownerEmail.isBlank()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(ownerEmail);
            helper.setSubject("⚠️ Low Stock Alert: " + productName + " (" + shopName + ")");

            String htmlContent = "<!DOCTYPE html>"
                    + "<html><head><meta charset='UTF-8'></head><body style='font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif; background-color:#F8FAFC; margin:0; padding:24px; color:#1E293B;'>"
                    + "<div style='max-width:540px; margin:0 auto; background:#FFFFFF; border-radius:12px; border:1px solid #E2E8F0; padding:32px; box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);'>"
                    + "<div style='text-align:center; margin-bottom:24px;'>"
                    + "  <div style='display:inline-block; background:#EF4444; color:#ffffff; font-weight:800; font-size:22px; width:44px; height:44px; line-height:44px; border-radius:10px;'>!</div>"
                    + "  <h2 style='margin:12px 0 4px; color:#0F172A; font-size:20px; font-weight:700;'>Low Stock Alert</h2>"
                    + "  <p style='color:#64748B; font-size:14px; margin:0;'>" + shopName + "</p>"
                    + "</div>"
                    + "<p style='font-size:14px; line-height:1.6; color:#475569;'>One of your products has reached or fallen below your minimum safety threshold after a recent sale:</p>"
                    + "<div style='background:#FEF2F2; border:1px solid #FCA5A5; border-radius:8px; padding:16px; margin:20px 0;'>"
                    + "  <div style='font-size:16px; font-weight:700; color:#991B1B; margin-bottom:6px;'>" + productName + "</div>"
                    + "  <div style='font-size:14px; color:#7F1D1D;'>Current Stock: <strong>" + currentStock + " units</strong></div>"
                    + "  <div style='font-size:12px; color:#B91C1C; margin-top:4px;'>Minimum Alert Threshold: " + threshold + " units</div>"
                    + "</div>"
                    + "<p style='font-size:13px; color:#475569;'>We recommend updating your purchase orders or restocking this item to avoid running out of inventory.</p>"
                    + "<div style='text-align:center; margin:26px 0;'>"
                    + "  <a href='https://xenbill.web.app/products' style='background:#0F172A; color:#ffffff; padding:12px 26px; text-decoration:none; border-radius:8px; font-weight:600; font-size:14px; display:inline-block;'>Manage Inventory</a>"
                    + "</div>"
                    + "<hr style='border:none; border-top:1px solid #E2E8F0; margin:24px 0;'/>"
                    + "<p style='font-size:12px; color:#94A3B8; text-align:center; margin:0;'>XenBill Cloud Inventory &bull; <a href='https://xenbill.web.app' style='color:#4F46E5; text-decoration:none;'>xenbill.web.app</a></p>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Low stock alert sent via Resend to {} for product {}", ownerEmail, productName);
        } catch (Exception e) {
            log.error("Failed to send low stock alert to {}: {}", ownerEmail, e.getMessage(), e);
        }
    }

    public void sendLowStockAlertEmail(String ownerEmail, String shopName, List<String> lowStockSummaries) {
        if (ownerEmail == null || ownerEmail.isBlank() || lowStockSummaries == null || lowStockSummaries.isEmpty()) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(ownerEmail);
            helper.setSubject("⚠️ Low Stock Alert: " + lowStockSummaries.size() + " Items Running Low (" + shopName + ")");

            StringBuilder itemsHtml = new StringBuilder();
            for (String summary : lowStockSummaries) {
                itemsHtml.append("<li style='margin-bottom:6px;'>").append(summary).append("</li>");
            }

            String htmlContent = "<!DOCTYPE html>"
                    + "<html><head><meta charset='UTF-8'></head><body style='font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif; background-color:#F8FAFC; margin:0; padding:24px; color:#1E293B;'>"
                    + "<div style='max-width:540px; margin:0 auto; background:#FFFFFF; border-radius:12px; border:1px solid #E2E8F0; padding:32px; box-shadow:0 4px 6px -1px rgba(0,0,0,0.05);'>"
                    + "<div style='text-align:center; margin-bottom:24px;'>"
                    + "  <div style='display:inline-block; background:#EF4444; color:#ffffff; font-weight:800; font-size:22px; width:44px; height:44px; line-height:44px; border-radius:10px;'>!</div>"
                    + "  <h2 style='margin:12px 0 4px; color:#0F172A; font-size:20px; font-weight:700;'>Low Stock Alert</h2>"
                    + "  <p style='color:#64748B; font-size:14px; margin:0;'>" + shopName + "</p>"
                    + "</div>"
                    + "<p style='font-size:14px; line-height:1.6; color:#475569;'>The following products have dropped below their minimum stock threshold:</p>"
                    + "<div style='background:#FEF2F2; border:1px solid #FCA5A5; border-radius:8px; padding:16px; margin:20px 0;'>"
                    + "  <ul style='margin:0; padding-left:20px; font-size:14px; color:#991B1B;'>" + itemsHtml + "</ul>"
                    + "</div>"
                    + "<p style='font-size:13px; color:#475569;'>Please restock these items soon to prevent sales interruption.</p>"
                    + "<div style='text-align:center; margin:26px 0;'>"
                    + "  <a href='https://xenbill.web.app/products' style='background:#0F172A; color:#ffffff; padding:12px 26px; text-decoration:none; border-radius:8px; font-weight:600; font-size:14px; display:inline-block;'>Manage Inventory</a>"
                    + "</div>"
                    + "<hr style='border:none; border-top:1px solid #E2E8F0; margin:24px 0;'/>"
                    + "<p style='font-size:12px; color:#94A3B8; text-align:center; margin:0;'>XenBill Cloud Inventory &bull; <a href='https://xenbill.web.app' style='color:#4F46E5; text-decoration:none;'>xenbill.web.app</a></p>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Low stock summary alert sent via Resend to {} for {} items", ownerEmail, lowStockSummaries.size());
        } catch (Exception e) {
            log.error("Failed to send low stock summary alert to {}: {}", ownerEmail, e.getMessage(), e);
        }
    }

    // =========================================================================
    // INVOICE PDF EMAIL (PRESERVED)
    // =========================================================================

    public void sendInvoiceEmail(String toEmail, String invoiceNumber, byte[] pdfBytes, String shopName) {
        if (toEmail == null || toEmail.isBlank()) {
            log.info("Skipping invoice email: customer email is blank");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);

            String senderTitle = (shopName != null && !shopName.isBlank()) ? shopName : "XenBill";
            helper.setSubject("Tax Invoice #" + invoiceNumber + " from " + senderTitle);
            
            String emailBody = "Dear Customer,\n\n"
                    + "Thank you for shopping with " + senderTitle + "!\n\n"
                    + "Please find attached your official Tax Invoice #" + invoiceNumber + ".\n\n"
                    + "Warm regards,\n"
                    + senderTitle + "\n"
                    + "Powered by XenBill (https://xenbill.web.app)";
            helper.setText(emailBody);

            if (pdfBytes != null && pdfBytes.length > 0) {
                helper.addAttachment("Invoice-" + invoiceNumber + ".pdf", new ByteArrayResource(pdfBytes), "application/pdf");
            }

            mailSender.send(message);
            log.info("Invoice #{} successfully sent to {} via Resend", invoiceNumber, toEmail);
        } catch (Exception e) {
            log.error("Failed to send invoice email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendInvoiceEmail(String toEmail, String invoiceNumber, byte[] pdfBytes) {
        sendInvoiceEmail(toEmail, invoiceNumber, pdfBytes, "XenBill");
    }
}
