package com.example.startspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StartspringApplication {

    public static void main(String[] args) {
        // This line starts the Spring container and your backend worker.
        SpringApplication.run(StartspringApplication.class, args);
        
    }
}