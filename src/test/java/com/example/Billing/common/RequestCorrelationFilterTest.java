package com.example.Billing.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void preservesSafeRequestIdAndRejectsLogInjection() throws Exception {
        MockHttpServletRequest safe = new MockHttpServletRequest();
        safe.addHeader(RequestCorrelationFilter.HEADER, "client-request-123");
        MockHttpServletResponse safeResponse = new MockHttpServletResponse();
        filter.doFilter(safe, safeResponse, new MockFilterChain());
        assertThat(safeResponse.getHeader(RequestCorrelationFilter.HEADER)).isEqualTo("client-request-123");

        MockHttpServletRequest unsafe = new MockHttpServletRequest();
        unsafe.addHeader(RequestCorrelationFilter.HEADER, "bad\nforged-log");
        MockHttpServletResponse unsafeResponse = new MockHttpServletResponse();
        filter.doFilter(unsafe, unsafeResponse, new MockFilterChain());
        assertThat(unsafeResponse.getHeader(RequestCorrelationFilter.HEADER)).matches("[0-9a-f-]{36}");
    }
}
