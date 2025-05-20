package com.aiproject.smartcampus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.aiproject.smartcampus.mapper")
@EnableTransactionManagement
public class SmartCampusApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCampusApplication.class, args);
    }

}
