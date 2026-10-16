package com.velox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** VELOX — local delivery platform for Samannoud. Entry point of the REST API. */
@SpringBootApplication
@EnableScheduling
public class VeloxApplication {

    public static void main(String[] args) {
        SpringApplication.run(VeloxApplication.class, args);
    }
}
