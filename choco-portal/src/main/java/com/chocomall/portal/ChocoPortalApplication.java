package com.chocomall.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Customer-facing API: browsing, search, cart, checkout, payments, member centre, personalisation and experiences. */
@SpringBootApplication(scanBasePackages = "com.chocomall")
public class ChocoPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChocoPortalApplication.class, args);
    }
}
