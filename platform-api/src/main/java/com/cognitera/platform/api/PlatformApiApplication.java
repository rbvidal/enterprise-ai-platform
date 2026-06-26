package com.cognitera.platform.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot entry point for the platform API.
 */
@SpringBootApplication(scanBasePackages = "com.cognitera.platform")
@EntityScan("com.cognitera.platform")
@EnableJpaRepositories("com.cognitera.platform")
public class PlatformApiApplication {

    /**
     * Launches the Spring Boot application.
     */
    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
