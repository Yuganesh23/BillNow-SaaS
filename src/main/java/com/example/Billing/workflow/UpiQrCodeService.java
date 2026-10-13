package com.example.Billing.workflow;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

@Service
public class UpiQrCodeService {
    public byte[] png(String paymentLink) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var matrix = new QRCodeWriter().encode(paymentLink, BarcodeFormat.QR_CODE, 240, 240);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render UPI QR", exception);
        }
    }
}
