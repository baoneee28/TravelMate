package com.travelmate.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleOAuthClientConfigTest {

    @Test
    void missingCredentialsLeavesGoogleOAuthDisabledWithoutBreakingApplicationContext() {
        try (AnnotationConfigApplicationContext context = contextWith(
                "travelmate.oauth2.google.enabled=true",
                "travelmate.oauth2.google.client-id=",
                "travelmate.oauth2.google.client-secret=")) {
            assertThat(context.getBeansOfType(ClientRegistrationRepository.class)).isEmpty();
        }
    }

    @Test
    void completeOptInConfigurationRegistersGoogleClient() {
        try (AnnotationConfigApplicationContext context = contextWith(
                "travelmate.oauth2.google.enabled=true",
                "travelmate.oauth2.google.client-id=local-client.apps.googleusercontent.com",
                "travelmate.oauth2.google.client-secret=local-secret")) {
            ClientRegistrationRepository registrations = context.getBean(ClientRegistrationRepository.class);
            assertThat(registrations.findByRegistrationId("google")).isNotNull();
        }
    }

    private AnnotationConfigApplicationContext contextWith(String... properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of(properties).applyTo(context);
        context.register(GoogleOAuthClientConfig.class);
        context.refresh();
        return context;
    }
}
