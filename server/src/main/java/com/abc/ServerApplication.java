package com.abc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * created by CaiBaoHong at 2018/4/17 14:15<br>
 *
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class,args);
    }

}
