package com.abc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * -Dspring.profiles.active=home  -Dlog4j2.profile=dev
 * -Dspring.profiles.active=prod -Dlog4j2.profile=dev
 * -Dspring.profiles.active=prod -Dlog4j2.profile=prod
 */
@SpringBootApplication
@EnableScheduling
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class,args);
    }

}
