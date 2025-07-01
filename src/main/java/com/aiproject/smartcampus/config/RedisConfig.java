package com.aiproject.smartcampus.config;


import com.aiproject.smartcampus.pojo.bo.CacheEntry;
import com.aiproject.smartcampus.pojo.po.Student;
import dev.langchain4j.memory.ChatMemory;
import okhttp3.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @program: SmartTeachAgent
 * @description: redis配置
 * @author: lk_hhh
 * @create: 2025-06-19 11:16
 **/

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }
    @Bean
    public RedisTemplate<String, CacheEntry> chatMemoryredisTemplate(RedisConnectionFactory redisConnectionFactory) {
        //序列化
        RedisTemplate<String, CacheEntry> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(CacheEntry.class));
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(CacheEntry.class));
        // 初始化
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Student> studentRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Student> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Student.class));
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Student.class));
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


}