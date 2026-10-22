package com.example.Billing.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthCookiesTest {
    private static final String ACCESS = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl";
    private static final String REFRESH = "OKxNRZi5Qf1A7KctkZ-QQynjf3JKm3qLBruJQ10IBQ_";

    @Test
    void splitModeUsesSeparateCookies() {
        AuthCookies cookies = new AuthCookies("split", true, "Lax");
        HttpHeaders headers = cookies.issue(ACCESS, Duration.ofMinutes(15), REFRESH, Duration.ofDays(30));
        assertThat(headers.get(HttpHeaders.SET_COOKIE)).hasSize(2)
                .anySatisfy(cookie -> assertThat(cookie).startsWith("jwt=" + ACCESS).contains("Max-Age=900", "HttpOnly", "Secure"))
                .anySatisfy(cookie -> assertThat(cookie).startsWith("refresh_token=" + REFRESH));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("jwt", ACCESS), new Cookie("refresh_token", REFRESH));
        assertThat(cookies.accessToken(request)).isEqualTo(ACCESS);
        assertThat(cookies.refreshToken(request)).isEqualTo(REFRESH);
    }

    @Test
    void sessionModeKeepsBothTokensInTheFirebaseSessionCookie() {
        AuthCookies cookies = new AuthCookies("session", true, "Lax");
        HttpHeaders headers = cookies.issue(ACCESS, Duration.ofMinutes(15), REFRESH, Duration.ofDays(30));
        assertThat(headers.get(HttpHeaders.SET_COOKIE)).singleElement().satisfies(cookie ->
                assertThat(cookie).startsWith("__session=" + ACCESS + "~" + REFRESH).contains("Max-Age=2592000", "HttpOnly"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("__session", ACCESS + "~" + REFRESH), new Cookie("jwt", "ignored"));
        assertThat(cookies.accessToken(request)).isEqualTo(ACCESS);
        assertThat(cookies.refreshToken(request)).isEqualTo(REFRESH);
    }

    @Test
    void malformedSessionCookieYieldsNoTokens() {
        AuthCookies cookies = new AuthCookies("session", false, "Lax");
        for (String value : new String[]{"no-separator", "~only-refresh", "only-access~"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("__session", value));
            String access = cookies.accessToken(request);
            String refresh = cookies.refreshToken(request);
            assertThat(access == null || refresh == null).as(value).isTrue();
        }
        assertThat(cookies.accessToken(new MockHttpServletRequest())).isNull();
    }

    @Test
    void clearExpiresCookiesFromBothModes() {
        assertThat(new AuthCookies("split", false, "Lax").clear().get(HttpHeaders.SET_COOKIE))
                .hasSize(3).allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));
    }

    @Test
    void unknownModeFailsAtStartup() {
        assertThatThrownBy(() -> new AuthCookies("firebase", false, "Lax")).isInstanceOf(IllegalStateException.class);
    }
}
