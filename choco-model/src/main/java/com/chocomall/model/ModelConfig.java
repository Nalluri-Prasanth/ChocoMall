package com.chocomall.model;

import com.chocomall.model.seed.SeedProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Registers the shared persistence model for every application that depends on this module. */
@Configuration
@EntityScan("com.chocomall.model.entity")
@EnableJpaRepositories("com.chocomall.model.repository")
@EnableConfigurationProperties(SeedProperties.class)
@ComponentScan("com.chocomall.model.seed")
public class ModelConfig {
}
