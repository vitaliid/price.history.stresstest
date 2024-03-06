package com.smarttrader.price.history.service.stresstest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableEnvironment env = SpringApplication.run(Application.class, args).getEnvironment();
        log.info("""

                        ----------------------------------------------------------
                        \tApplication '{}' is running!\s
                        \tAccess URL: http://localhost:{}
                        \tProfile(s): {}
                        ----------------------------------------------------------""",
                env.getProperty("spring.application.name"),
                env.getProperty("server.port"),
                env.getActiveProfiles());
   }
}
