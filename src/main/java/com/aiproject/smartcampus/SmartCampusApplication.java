package com.aiproject.smartcampus;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan("com.aiproject.smartcampus.mapper")
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@Slf4j
public class SmartCampusApplication {

    //加载中
    public static void main(String[] args) {
        SpringApplication.run(SmartCampusApplication.class, args);
        log.info("程序已经启动");
    }


}
