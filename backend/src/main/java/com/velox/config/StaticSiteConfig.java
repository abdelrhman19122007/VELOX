package com.velox.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the two front ends from the same address as the API, so there is one link to share and no CORS:
 * <ul>
 *   <li>{@code /} — the website and the admin / store dashboard ({@code web/})</li>
 *   <li>{@code /app/} — the Flutter app built for the browser ({@code mobile/build/web}, built with --base-href /app/)</li>
 * </ul>
 * API routes ({@code /api/**}) always win over files.
 */
@Configuration
public class StaticSiteConfig implements WebMvcConfigurer {

    private final String webDir;
    private final String appDir;

    public StaticSiteConfig(@Value("${velox.web.dir:../web}") String webDir,
                            @Value("${velox.app.dir:../mobile/build/web}") String appDir) {
        this.webDir = location(webDir);
        this.appDir = location(appDir);
    }

    private static String location(String dir) {
        return Path.of(dir).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/app/**").addResourceLocations(appDir);
        registry.addResourceHandler("/**").addResourceLocations(webDir);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addRedirectViewController("/app", "/app/");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
        registry.addRedirectViewController("/admin", "/admin/");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }
}
