package com.aiproject.smartcampus.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @program: lecture-langchain-20250525
 * @description: JSON工具类
 * @author: lk
 * @create: 2025-05-14 20:14
 **/

@Slf4j
public class JsonUtils {

    public static Object toJsonObject(String json, Class<?> clazz) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        Object newObject = clazz.getDeclaredConstructor().newInstance();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            try {
                Field field=clazz.getDeclaredField(key);
                field.setAccessible(true);
                field.set(newObject, value);
            } catch (NoSuchFieldException e) {
                log.error("创建类失败,字段名:{}", key, e);
            }
        }
        return newObject;
    }

    public static String toJsonString(Object object) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(object);
    }





}
