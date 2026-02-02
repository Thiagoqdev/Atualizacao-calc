package com.calculosjuridicos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CalculosJuridicosApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalculosJuridicosApplication.class, args);
    }
}
