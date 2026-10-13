package com.example.Billing.gst;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GstinValidatorTest {
    @Test
    void validatesFormatStatePanAndChecksum() {
        assertThat(GstinValidator.isValid("27AAPFU0939F1ZV")).isTrue();
        assertThat(GstinValidator.isValid("07AAACB2894G1ZP")).isTrue();
        assertThat(GstinValidator.isValid("27AAPFU0939F1ZA")).isFalse();
        assertThat(GstinValidator.isValid("99AAPFU0939F1ZV")).isFalse();
        assertThat(GstinValidator.isValid("27A1PFU0939F1ZV")).isFalse();
    }
}
