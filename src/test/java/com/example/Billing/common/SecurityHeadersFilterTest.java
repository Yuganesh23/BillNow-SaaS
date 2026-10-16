package com.example.Billing.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHeadersFilterTest {
    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void addsBrowserSecurityHeadersToEveryResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Content-Security-Policy"))
                .isEqualTo(SecurityHeadersFilter.API_CSP);
        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(response.getHeader("Permissions-Policy"))
                .doesNotContain("camera=*").contains("camera=()");
    }

    @Test
    void permitsOnlySelfHostedAssetsForTheAuthenticatedApiReference() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Content-Security-Policy"))
                .isEqualTo(SecurityHeadersFilter.DOCS_CSP)
                .contains("script-src 'self'")
                .doesNotContain("script-src 'self' 'unsafe-inline'");
    }
}
