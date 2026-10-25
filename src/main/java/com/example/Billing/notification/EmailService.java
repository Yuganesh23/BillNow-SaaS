package com.example.Billing.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${resend.from-email:${RESEND_FROM_EMAIL:XenBill <billing@xenfirm.com>}}")
    private String fromEmail;

    private RestClient getRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Dispatch email via Resend's HTTPS REST API (Port 443 - zero firewall blocks on Cloud Run)
     */
    public boolean sendEmailViaApi(String toEmail, String subject, String htmlContent, byte[] attachmentBytes, String attachmentName) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Recipient email is blank; skipping send.");
            return false;
        }

        String apiKey = resendApiKey;
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("RESEND_API_KEY");
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.error("RESEND_API_KEY is not configured. Cannot send email to {}.", toEmail);
            return false;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", fromEmail != null && !fromEmail.isBlank() ? fromEmail : "XenBill <billing@xenfirm.com>");
            body.put("to", Collections.singletonList(toEmail.trim()));
            body.put("subject", subject);
            body.put("html", htmlContent);

            if (attachmentBytes != null && attachmentBytes.length > 0 && attachmentName != null) {
                Map<String, Object> attachment = new HashMap<>();
                attachment.put("filename", attachmentName);
                attachment.put("content", Base64.getEncoder().encodeToString(attachmentBytes));
                body.put("attachments", Collections.singletonList(attachment));
            }

            RestClient client = getRestClient();
            var response = client.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("User-Agent", "resend-java/1.0.0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            log.info("Email '{}' sent successfully to {} via Resend API (HTTP {})", subject, toEmail, response.getStatusCode());
            return true;
        } catch (Exception e) {
            log.error("Resend API failed to deliver email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // 1. 🔐 PASSWORD RESET LINKS & VERIFICATION
    // =========================================================================

    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetUrl) {
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
                + "<p style='font-size:13px; color:#64748B; line-height:1.6;'>If the button does not work, copy and paste this link into your browser:<br/><a href='" + link + "' style='color:#4F46E5; word-break:break-all;'>" + link + "</a></p>"
                + "<p style='font-size:12px; color:#94A3B8; line-height:1.5; margin-top:24px;'>This link is valid for 1 hour. If you did not request a password reset, you can safely ignore this email.</p>"
                + "<hr style='border:none; border-top:1px solid #E2E8F0; margin:24px 0;'/>"
                + "<p style='font-size:12px; color:#94A3B8; text-align:center; margin:0;'>XenBill &bull; <a href='https://xenbill.web.app' style='color:#4F46E5; text-decoration:none;'>xenbill.web.app</a></p>"
                + "</div></body></html>";

        return sendEmailViaApi(toEmail, "🔐 Reset Your XenBill Password", htmlContent, null, null);
    }

    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetUrl, String otp) {
        return sendPasswordResetEmail(toEmail, userName, resetUrl);
    }

    public boolean sendOtpEmail(String toEmail, String otp) {
        String resetUrl = "https://xenbill.web.app/forgot-password?email=" + toEmail;
        return sendPasswordResetEmail(toEmail, "User", resetUrl);
    }

    // =========================================================================
    // 2. 👤 WELCOME EMAILS ON NEW SHOP REGISTRATION
    // =========================================================================

    public boolean sendWelcomeEmail(String toEmail, String ownerName, String shopName) {
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

        return sendEmailViaApi(toEmail, "🎉 Welcome to XenBill, " + shopName + "!", htmlContent, null, null);
    }

    // =========================================================================
    // 3. 📦 LOW-STOCK ALERTS FOR SHOP OWNERS
    // =========================================================================

    public boolean sendLowStockAlertEmail(String ownerEmail, String shopName, String productName, Double currentStock, Double threshold) {
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

        return sendEmailViaApi(ownerEmail, "⚠️ Low Stock Alert: " + productName + " (" + shopName + ")", htmlContent, null, null);
    }

    public boolean sendLowStockAlertEmail(String ownerEmail, String shopName, List<String> lowStockSummaries) {
        if (lowStockSummaries == null || lowStockSummaries.isEmpty()) return false;

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

        return sendEmailViaApi(ownerEmail, "⚠️ Low Stock Alert: " + lowStockSummaries.size() + " Items Running Low (" + shopName + ")", htmlContent, null, null);
    }

    // =========================================================================
    // INVOICE PDF EMAIL (PRESERVED)
    // =========================================================================

    public boolean sendInvoiceEmail(String toEmail, String invoiceNumber, byte[] pdfBytes, String shopName) {
        String senderTitle = (shopName != null && !shopName.isBlank()) ? shopName : "XenBill";
        String subject = "Tax Invoice #" + invoiceNumber + " from " + senderTitle;

        String htmlContent = "<p>Dear Customer,</p>"
                + "<p>Thank you for shopping with <strong>" + senderTitle + "</strong>!</p>"
                + "<p>Please find attached your official Tax Invoice #" + invoiceNumber + ".</p>"
                + "<p>Warm regards,<br/>" + senderTitle + "<br/>Powered by XenBill (https://xenbill.web.app)</p>";

        return sendEmailViaApi(toEmail, subject, htmlContent, pdfBytes, "Invoice-" + invoiceNumber + ".pdf");
    }

    public boolean sendInvoiceEmail(String toEmail, String invoiceNumber, byte[] pdfBytes) {
        return sendInvoiceEmail(toEmail, invoiceNumber, pdfBytes, "XenBill");
    }
}
