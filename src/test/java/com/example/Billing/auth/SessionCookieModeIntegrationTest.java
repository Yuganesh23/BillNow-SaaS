package com.example.Billing.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Firebase Hosting forwards only the __session cookie; the whole auth flow must work with it alone. */
@SpringBootTest(properties = "auth.cookie.mode=session")
@AutoConfigureMockMvc
class SessionCookieModeIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void registerRefreshAndLogoutWorkWithOnlyTheSessionCookie() throws Exception {
        String email = "session-" + UUID.randomUUID() + "@example.test";
        MvcResult registered = mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                {"ownerName":"Session Owner","email":"%s","password":"Str0ng!Passw0rd","shopName":"Session Shop",
                 "mobileNumber":"9876543210","address":"Pune"}
                """.formatted(email))).andExpect(status().isCreated()).andReturn();
        Cookie session = sessionCookie(registered);

        mvc.perform(get("/api/subscription/usage").cookie(session)).andExpect(status().isOk());
        mvc.perform(get("/api/subscription/usage")).andExpect(status().is4xxClientError());

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(session)).andExpect(status().isOk()).andReturn();
        Cookie rotated = sessionCookie(refreshed);
        assertThat(rotated.getValue()).isNotEqualTo(session.getValue());
        mvc.perform(get("/api/subscription/usage").cookie(rotated)).andExpect(status().isOk());
        // The rotated-away refresh token can no longer be used.
        mvc.perform(post("/api/auth/refresh").cookie(session)).andExpect(status().isUnauthorized());

        MvcResult loggedOut = mvc.perform(post("/api/auth/logout").cookie(rotated)).andExpect(status().isOk()).andReturn();
        assertThat(loggedOut.getResponse().getHeaders("Set-Cookie")).anySatisfy(header ->
                assertThat(header).startsWith("__session=;").contains("Max-Age=0"));
        mvc.perform(post("/api/auth/refresh").cookie(rotated)).andExpect(status().isUnauthorized());
    }

    private static Cookie sessionCookie(MvcResult result) {
        List<String> headers = result.getResponse().getHeaders("Set-Cookie");
        assertThat(headers).singleElement().satisfies(header -> assertThat(header).startsWith("__session="));
        String value = headers.get(0).substring("__session=".length(), headers.get(0).indexOf(';'));
        assertThat(value).contains("~");
        return new Cookie("__session", value);
    }
}
