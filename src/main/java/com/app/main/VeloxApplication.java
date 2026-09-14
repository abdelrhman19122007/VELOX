package com.app.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for VELOX Backend API (merged from VELOX-main).
 * Console app still runs via {@link Main#main(String[])}.
 * Run API with: mvn spring-boot:run -Dspring-boot.run.main-class=com.app.main.VeloxApplication
 */
@SpringBootApplication(scanBasePackages = "com.app")
@EnableScheduling
public class VeloxApplication {
    public static void main(String[] args) {
        SpringApplication.run(VeloxApplication.class, args);
    }
}
