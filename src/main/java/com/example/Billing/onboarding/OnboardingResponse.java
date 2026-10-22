package com.example.Billing.onboarding;

import java.util.List;

public record OnboardingResponse(
        int completedSteps,
        int totalSteps,
        int percentComplete,
        boolean complete,
        List<Step> steps
) {
    public record Step(String key, String label, String route, boolean complete) {}
}
