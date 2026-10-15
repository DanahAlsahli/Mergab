package com.example.mergab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MergabApplication {

    public static void main(String[] args) {
        SpringApplication.run(MergabApplication.class, args);
    }

}
