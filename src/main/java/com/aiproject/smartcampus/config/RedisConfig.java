package com.aiproject.smartcampus.config;

import com.aiproject.smartcampus.pojo.bo.CacheEntry;
import com.aiproject.smartcampus.pojo.po.*;
import com.aiproject.smartcampus.pojo.vo.KnowledgePointSimpleVO;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.text.SimpleDateFormat;

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

    /**
     * 创建配置了JavaTimeModule的ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 🔥 关键配置：注册JSR310模块处理Java 8时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 配置时间格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 设置可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 激活默认类型（可选，根据需要）
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        return objectMapper;
    }

    /**
     * 创建配置了ObjectMapper的Jackson2JsonRedisSerializer
     */
    private <T> Jackson2JsonRedisSerializer<T> createJsonSerializer(Class<T> clazz) {
        Jackson2JsonRedisSerializer<T> serializer = new Jackson2JsonRedisSerializer<>(clazz);
        serializer.setObjectMapper(createObjectMapper());
        return serializer;
    }

    @Bean
    public RedisTemplate<String, CacheEntry> chatMemoryredisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, CacheEntry> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 使用配置了JavaTimeModule的序列化器
        Jackson2JsonRedisSerializer<CacheEntry> serializer = createJsonSerializer(CacheEntry.class);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Student> studentRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Student> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 使用配置了JavaTimeModule的序列化器
        Jackson2JsonRedisSerializer<Student> serializer = createJsonSerializer(Student.class);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Teacher> teacherRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Teacher> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 使用配置了JavaTimeModule的序列化器
        Jackson2JsonRedisSerializer<Teacher> serializer = createJsonSerializer(Teacher.class);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Admin> adminRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Admin> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 使用配置了JavaTimeModule的序列化器
        Jackson2JsonRedisSerializer<Admin> serializer = createJsonSerializer(Admin.class);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, KnowledgePointSimpleVO> knowledgePointSimpleVORedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, KnowledgePointSimpleVO> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<KnowledgePointSimpleVO> serializer = createJsonSerializer(KnowledgePointSimpleVO.class);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, User> userRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, User> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 使用配置了JavaTimeModule的序列化器
        Jackson2JsonRedisSerializer<User> serializer = createJsonSerializer(User.class);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    @Bean
    public RedisTemplate<String, SseEmitter>  sseEmitterRedisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, SseEmitter> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // 使用配置了JavaTimeModule的序列化器
        Jackson2JsonRedisSerializer<SseEmitter> serializer = createJsonSerializer(SseEmitter.class);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;



    }

}