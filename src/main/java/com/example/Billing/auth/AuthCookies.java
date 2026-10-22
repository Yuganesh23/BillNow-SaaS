package com.example.Billing.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Reads and writes the HttpOnly auth cookies.
 *
 * <p>{@code split} (default) uses separate {@code jwt} and {@code refresh_token}
 * cookies. {@code session} stores both tokens in one {@code __session} cookie,
 * because Firebase Hosting forwards only that cookie to Cloud Run. The access
 * token still expires on its own JWT expiry in both modes.
 */
@Component
public class AuthCookies {
    static final String ACCESS = "jwt";
    static final String REFRESH = "refresh_token";
    static final String SESSION = "__session";
    // Not used by JWTs (base64url and '.') or refresh tokens (base64url).
    private static final char SEPARATOR = '~';

    private final boolean sessionMode;
    private final boolean secure;
    private final String sameSite;

    public AuthCookies(@Value("${auth.cookie.mode:split}") String mode,
                       @Value("${auth.cookie.secure:false}") boolean secure,
                       @Value("${auth.cookie.same-site:Lax}") String sameSite) {
        if (!"split".equals(mode) && !"session".equals(mode)) {
            throw new IllegalStateException("auth.cookie.mode must be 'split' or 'session', was '" + mode + "'");
        }
        this.sessionMode = "session".equals(mode);
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public String accessToken(HttpServletRequest request) {
        if (!sessionMode) return value(request, ACCESS);
        String session = value(request, SESSION);
        int split = session == null ? -1 : session.indexOf(SEPARATOR);
        return split <= 0 ? null : session.substring(0, split);
    }

    public String refreshToken(HttpServletRequest request) {
        if (!sessionMode) return value(request, REFRESH);
        String session = value(request, SESSION);
        int split = session == null ? -1 : session.indexOf(SEPARATOR);
        return split < 0 || split == session.length() - 1 ? null : session.substring(split + 1);
    }

    public HttpHeaders issue(String accessToken, Duration accessTtl, String refreshToken, Duration refreshTtl) {
        HttpHeaders headers = new HttpHeaders();
        if (sessionMode) {
            headers.add(HttpHeaders.SET_COOKIE, cookie(SESSION, accessToken + SEPARATOR + refreshToken, refreshTtl).toString());
        } else {
            headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS, accessToken, accessTtl).toString());
            headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH, refreshToken, refreshTtl).toString());
        }
        return headers;
    }

    /** Expires every auth cookie, whichever mode issued it. */
    public HttpHeaders clear() {
        HttpHeaders headers = new HttpHeaders();
        for (String name : new String[]{ACCESS, REFRESH, SESSION}) {
            headers.add(HttpHeaders.SET_COOKIE, cookie(name, "", Duration.ZERO).toString());
        }
        return headers;
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true).secure(secure).path("/").maxAge(maxAge).sameSite(sameSite).build();
    }

    private static String value(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
