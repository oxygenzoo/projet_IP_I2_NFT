package com.nft.backend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final String[] allowedOriginPatterns;
    private final String[] allowedMethods;

    public CorsConfig(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins,
            @Value("${app.cors.allowed-origin-patterns}") String[] allowedOriginPatterns,
            @Value("${app.cors.allowed-methods}") String[] allowedMethods) {
        this.allowedOrigins = clean(allowedOrigins);
        this.allowedOriginPatterns = clean(allowedOriginPatterns);
        this.allowedMethods = clean(allowedMethods);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/**")
                .allowedMethods(allowedMethods)
                .allowedHeaders("*")
                .allowCredentials(true);

        if (allowedOrigins.length > 0) {
            registration.allowedOrigins(allowedOrigins);
        }

        if (allowedOriginPatterns.length > 0) {
            registration.allowedOriginPatterns(allowedOriginPatterns);
        }
    }

    private static String[] clean(String[] values) {
        return Arrays.stream(values)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }
}
