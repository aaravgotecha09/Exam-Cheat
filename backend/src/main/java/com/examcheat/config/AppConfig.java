package com.examcheat.config;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {
    private final String[] originPatterns;

    public AppConfig(@Value("${app.cors.allowed-origin-patterns}") String patterns) {
        this.originPatterns = java.util.Arrays.stream(patterns.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    /** Injectable clock so tests can control time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(originPatterns)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "X-Session-Token")
                .exposedHeaders("Content-Disposition")
                .maxAge(3600);
    }
}
