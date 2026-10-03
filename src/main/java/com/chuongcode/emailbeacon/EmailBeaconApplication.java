package com.chuongcode.emailbeacon;

import com.chuongcode.emailbeacon.config.BeaconProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BeaconProperties.class)
public class EmailBeaconApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailBeaconApplication.class, args);
    }
}

