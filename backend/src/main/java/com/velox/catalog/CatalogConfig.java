package com.velox.catalog;

import java.util.Map;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.velox.catalog.service.ImageStorage;
import com.velox.common.error.ApiError;

import jakarta.servlet.MultipartConfigElement;

/** Upload limits and the public /uploads folder for store and product photos. */
@Configuration
public class CatalogConfig implements WebMvcConfigurer {

    private final ImageStorage images;

    public CatalogConfig(ImageStorage images) {
        this.images = images;
    }

    /** Hard cap on what the server even reads; the 2 MB rule itself is checked in ImageStorage. */
    @Bean
    public MultipartConfigElement multipartConfig() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(3));
        factory.setMaxRequestSize(DataSize.ofMegabytes(4));
        return factory.createMultipartConfig();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(images.directory().toUri().toString());
    }

    /** Too big for the server to read at all → the same clean error as a 2–3 MB file. */
    @RestControllerAdvice
    static class UploadErrors {
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException e) {
            return ResponseEntity.badRequest().body(new ApiError("FILE_TOO_LARGE", "Photos must be 2 MB or smaller", Map.of()));
        }
    }
}
