package com.example.Billing.importdata;

import com.example.Billing.auth.User_entity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER')")
public class ImportController {
    private final ImportFileParser parser;
    private final ImportService importService;
    private final ImportTemplateService templateService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam ImportModels.Type type,
                                           @RequestParam(defaultValue = "xlsx") String format) {
        String extension = format.toLowerCase(Locale.ROOT);
        MediaType mediaType = extension.equals("csv") ? MediaType.parseMediaType("text/csv")
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(type.name().toLowerCase(Locale.ROOT) + "-template." + extension).build().toString())
                .body(templateService.create(type, format));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportModels.Preview> preview(Authentication authentication,
                                                        @RequestParam ImportModels.Type type,
                                                        @RequestParam MultipartFile file) {
        return ResponseEntity.ok(importService.preview(user(authentication), type, parser.parse(file)));
    }

    @PostMapping("/commit")
    public ResponseEntity<ImportModels.CommitResponse> commit(Authentication authentication,
                                                              @Valid @RequestBody ImportModels.CommitRequest request) {
        return ResponseEntity.ok(importService.commit(user(authentication), request));
    }

    private User_entity user(Authentication authentication) {
        return (User_entity) authentication.getPrincipal();
    }
}
