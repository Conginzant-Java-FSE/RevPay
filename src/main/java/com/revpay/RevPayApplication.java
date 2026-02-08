package com.revpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RevPayApplication {

    public static void main(String[] args) {
        //System.out.println("DB_URL = " + System.getenv("DB_NAME"));
        //System.out.println("DB_USERNAME = " + System.getenv("DB_USER"));
        //System.out.println("DB_PASSWORD = " + System.getenv("DB_PASS"));
        SpringApplication.run(RevPayApplication.class, args);
    }

}
