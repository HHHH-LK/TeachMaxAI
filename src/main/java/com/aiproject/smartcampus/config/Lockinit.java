package com.aiproject.smartcampus.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: SmartCampus
 * @description: 分布式锁初始化
 * @author: lk
 * @create: 2025-05-20 12:49
 **/

@Configuration
public class Lockinit {

    @Bean
    public RedissonClient redissionClient(){

        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379")
                .setConnectionPoolSize(100)
                .setConnectTimeout(10000);

        return Redisson.create(config);
    }

}
