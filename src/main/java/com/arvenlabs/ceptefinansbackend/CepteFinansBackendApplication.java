package com.arvenlabs.ceptefinansbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CepteFinansBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CepteFinansBackendApplication.class, args);
    }

}
