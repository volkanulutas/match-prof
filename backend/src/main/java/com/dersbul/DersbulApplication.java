package com.dersbul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DersbulApplication {
    public static void main(String[] args) {
        SpringApplication.run(DersbulApplication.class, args);
    }
}
