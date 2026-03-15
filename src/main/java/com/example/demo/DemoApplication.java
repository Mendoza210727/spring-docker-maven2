package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main Spring Boot application class.
 * <p>
 * This class serves as the entry point for the Spring Boot application and exposes
 * a simple health check endpoint at <code>/health</code>.
 * </p>
 * 
 * <p>
 * Annotations:
 * <ul>
 *   <li>{@link org.springframework.boot.autoconfigure.SpringBootApplication} - Indicates a Spring Boot application.</li>
 *   <li>{@link org.springframework.web.bind.annotation.RestController} - Marks this class as a REST controller.</li>
 * </ul>
 * </p>
 * 
 * @author Pablo Villazon
 */
@SpringBootApplication
@RestController
public class DemoApplication {

    /**
     * The main entry point of the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/health")
    static public String healthCheck() {
        return "OK - Usando IA Generativa para el pipeline";
    }
}
