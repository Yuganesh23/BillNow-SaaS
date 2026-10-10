package com.example.Billing.storage;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
public class ObjectStorageController {
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");
    private final S3Presigner presigner;
    private final ShopContextResolver shopContextResolver;

    @Value("${storage.bucket:uploads}")
    private String bucket;

    @PostMapping("/logo-upload")
    public Map<String, String> createLogoUpload(Authentication authentication, @RequestBody UploadRequest request) {
        Shop_entity shop = activeShop(authentication);
        if (request.contentType() == null || !IMAGE_TYPES.contains(request.contentType())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE", "Use a PNG, JPEG, WebP, or SVG image.");
        }
        String extension = switch (request.contentType()) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "svg";
        };
        String key = "shops/" + shop.getId() + "/logos/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest put = PutObjectRequest.builder().bucket(bucket).key(key).contentType(request.contentType()).build();
        String url = presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10)).putObjectRequest(put).build())
                .url().toString();
        return Map.of("uploadUrl", url, "objectKey", key, "contentType", request.contentType());
    }

    @GetMapping("/object-url")
    public Map<String, String> createReadUrl(Authentication authentication, @RequestParam String key) {
        Shop_entity shop = activeShop(authentication);
        String expectedPrefix = "shops/" + shop.getId() + "/";
        if (!key.startsWith(expectedPrefix)) {
            throw new AppException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Object does not belong to this shop.");
        }
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        String url = presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10)).getObjectRequest(get).build())
                .url().toString();
        return Map.of("url", url);
    }

    private Shop_entity activeShop(Authentication authentication) {
        User_entity user = (User_entity) authentication.getPrincipal();
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        if (shop == null) throw new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found.");
        return shop;
    }

    public record UploadRequest(String contentType) {}
}
