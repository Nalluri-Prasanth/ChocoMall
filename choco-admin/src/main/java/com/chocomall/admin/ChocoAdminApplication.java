package com.chocomall.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Back-office API for staff: catalog, stock, production, orders, members, marketing, content and reports. */
@SpringBootApplication(scanBasePackages = "com.chocomall")
public class ChocoAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChocoAdminApplication.class, args);
    }
}
