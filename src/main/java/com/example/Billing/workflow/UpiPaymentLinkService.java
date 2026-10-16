package com.example.Billing.workflow;

import com.example.Billing.common.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class UpiPaymentLinkService {
    public String create(String upiId, String payeeName, BigDecimal amount, String invoiceNumber) {
        if (upiId == null || !upiId.matches("^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$")) {
            throw new AppException(HttpStatus.BAD_REQUEST, "UPI_ID_REQUIRED", "Configure a valid shop UPI ID first");
        }
        if (amount == null || amount.signum() <= 0) throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "UPI amount must be positive");
        return "upi://pay?pa=" + encode(upiId) + "&pn=" + encode(payeeName) + "&am=" + amount.setScale(2)
                + "&cu=INR&tn=" + encode("Invoice " + invoiceNumber);
    }
    private String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20"); }
}
