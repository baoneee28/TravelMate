package com.travelmate.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/**
 * Creates the Google OAuth client only when a complete opt-in configuration is present.
 * Regular form login remains available when OAuth is disabled or only partly configured.
 */
@Configuration
public class GoogleOAuthClientConfig {

    @Bean
    @Conditional(CompleteGoogleOAuthConfiguration.class)
    public ClientRegistrationRepository googleClientRegistrationRepository(
            @Value("${travelmate.oauth2.google.client-id:}") String clientId,
            @Value("${travelmate.oauth2.google.client-secret:}") String clientSecret) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId.trim())
                .clientSecret(clientSecret.trim())
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    static class CompleteGoogleOAuthConfiguration implements Condition {
        @Override
        public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            boolean enabled = context.getEnvironment()
                    .getProperty("travelmate.oauth2.google.enabled", Boolean.class, true);
            String clientId = context.getEnvironment().getProperty("travelmate.oauth2.google.client-id");
            String clientSecret = context.getEnvironment().getProperty("travelmate.oauth2.google.client-secret");
            return enabled && StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
        }
    }
}
