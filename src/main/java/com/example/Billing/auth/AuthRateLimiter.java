package com.example.Billing.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local fixed-window protection. This is suitable for one Railway replica; use a
 * shared Redis-backed limiter before scaling the API to multiple instances.
 */
@Component
public class AuthRateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long windowMillis;
    private final int loginIpMax;
    private final int loginEmailMax;
    private final int forgotIpMax;
    private final int forgotEmailMax;
    private final int resetIpMax;
    private final int resetEmailMax;
    private final int registerIpMax;

    @Autowired
    public AuthRateLimiter(
            @Value("${auth.rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${auth.rate-limit.login.ip-max:20}") int loginIpMax,
            @Value("${auth.rate-limit.login.email-max:10}") int loginEmailMax,
            @Value("${auth.rate-limit.forgot-password.ip-max:10}") int forgotIpMax,
            @Value("${auth.rate-limit.forgot-password.email-max:3}") int forgotEmailMax,
            @Value("${auth.rate-limit.reset-password.ip-max:10}") int resetIpMax,
            @Value("${auth.rate-limit.reset-password.email-max:5}") int resetEmailMax,
            @Value("${auth.rate-limit.register.ip-max:5}") int registerIpMax) {
        this(windowSeconds, loginIpMax, loginEmailMax, forgotIpMax, forgotEmailMax,
                resetIpMax, resetEmailMax, registerIpMax, Clock.systemUTC());
    }

    AuthRateLimiter(long windowSeconds, int loginIpMax, int loginEmailMax,
                    int forgotIpMax, int forgotEmailMax, int resetIpMax,
                    int resetEmailMax, int registerIpMax, Clock clock) {
        if (windowSeconds < 1) throw new IllegalArgumentException("Rate-limit window must be positive");
        this.windowMillis = windowSeconds * 1000;
        this.loginIpMax = loginIpMax;
        this.loginEmailMax = loginEmailMax;
        this.forgotIpMax = forgotIpMax;
        this.forgotEmailMax = forgotEmailMax;
        this.resetIpMax = resetIpMax;
        this.resetEmailMax = resetEmailMax;
        this.registerIpMax = registerIpMax;
        this.clock = clock;
    }

    public void checkLogin(HttpServletRequest request, String email) {
        enforce("login:ip:" + clientIp(request), loginIpMax);
        enforceEmail("login:email:", email, loginEmailMax);
    }

    public void checkForgotPassword(HttpServletRequest request, String email) {
        enforce("forgot:ip:" + clientIp(request), forgotIpMax);
        enforceEmail("forgot:email:", email, forgotEmailMax);
    }

    public void checkResetPassword(HttpServletRequest request, String email) {
        enforce("reset:ip:" + clientIp(request), resetIpMax);
        enforceEmail("reset:email:", email, resetEmailMax);
    }

    public void checkRegister(HttpServletRequest request) {
        enforce("register:ip:" + clientIp(request), registerIpMax);
    }

    private void enforceEmail(String prefix, String email, int maximum) {
        String normalized = email == null ? "missing" : email.trim().toLowerCase(Locale.ROOT);
        enforce(prefix + normalized, maximum);
    }

    private void enforce(String key, int maximum) {
        long now = clock.millis();
        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.expiresAt) return new Window(1, now + windowMillis);
            current.count++;
            return current;
        });
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt);
        if (updated.count > maximum) {
            throw new RateLimitExceededException((updated.expiresAt - now + 999) / 1000);
        }
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static final class Window {
        private int count;
        private final long expiresAt;

        private Window(int count, long expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
