package com.velox.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.velox.security.CurrentUserResolver;

/** Web settings: CORS limited to known origins (OWASP A05) and {@code @CurrentUser} resolution. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] origins;
    private final CurrentUserResolver currentUser;

    public WebConfig(@Value("${velox.security.cors-origins}") String origins, CurrentUserResolver currentUser) {
        this.origins = origins.split(",");
        this.currentUser = currentUser;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS").allowedHeaders("*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUser);
    }
}
