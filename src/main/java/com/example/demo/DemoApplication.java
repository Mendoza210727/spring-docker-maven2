package com.example.demo;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;

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
    @Autowired
    private JdbcTemplate jdbcTemplate;
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
    @PostConstruct
    public void initDatabase() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS usuarios (id INT AUTO_INCREMENT, nombre VARCHAR(50), secreto VARCHAR(50))");
        // Insertamos un usuario administrador con una contraseña secreta
        jdbcTemplate.execute("INSERT INTO usuarios (nombre, secreto) VALUES ('admin', 'super_password_123')");
    }

    /**
     * Endpoint vulnerable a Inyección SQL (SQLi).
     * Prueba manual: http://tu-ip:8081/buscar?nombre=admin' OR '1'='1
     */
    @GetMapping("/buscar")
    public List<Map<String, Object>> buscarUsuario(@RequestParam(defaultValue = "admin") String nombre) {
        // 🚨 VULNERABILIDAD CRÍTICA: Concatenación directa de strings en SQL
        // ZAP detectará esto al inyectar comillas y operadores lógicos en el parámetro 'nombre'
        String query = "SELECT * FROM usuarios WHERE nombre = '" + nombre + "'";
        
        return jdbcTemplate.queryForList(query);
    }
}
