package com.chocomall.core;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Boots the shared business services (no web security) for integration tests. */
@SpringBootApplication(scanBasePackages = "com.chocomall")
public class CoreTestApplication {
}
