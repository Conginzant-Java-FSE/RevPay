package com.revpay;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(
        info = @Info(
                title = "RevPay",
                version = "1.0",
                description = "API Documentations for RevPay Project."
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development Server")
        }
)

@SpringBootApplication
@EnableScheduling
public class RevPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(RevPayApplication.class, args);
    }

}
