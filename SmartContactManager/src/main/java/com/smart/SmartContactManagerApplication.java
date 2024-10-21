package com.smart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication

public class SmartContactManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartContactManagerApplication.class, args);
    }
}
