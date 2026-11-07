package com.smartparcelguard;

import com.smartparcelguard.config.SmartParcelGuardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(SmartParcelGuardProperties.class)
@EnableScheduling
public class SmartParcelGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartParcelGuardApplication.class, args);
    }
}
