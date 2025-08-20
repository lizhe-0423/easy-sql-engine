package com.easysql.engine.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.easysql.engine")
public class EasySqlServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasySqlServerApplication.class, args);
    }
}