package com.chocomall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Product search: Elasticsearch in production, the shop database for local development. */
@SpringBootApplication(scanBasePackages = "com.chocomall")
public class ChocoSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChocoSearchApplication.class, args);
    }
}
