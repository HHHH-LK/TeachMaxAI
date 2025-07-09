package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.pojo.bo.Side;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: lecture-langchain-20250525
 * @description: JSON工具类
 * @author: lk
 * @create: 2025-05-14 20:14
 **/

@Slf4j
@NoArgsConstructor
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
                    Class<?> fieldType = field.getType();

                    // 字符串
                    if (fieldType.equals(String.class)) {
                        field.set(newObject, value.toString());
                    }
                    // 布尔型（兼容boolean/Boolean）
                    else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                        field.set(newObject, Boolean.valueOf(value.toString()));
                    }
                    // 数值型（整数/浮点数）
                    else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                        field.set(newObject, Integer.valueOf(value.toString()));
                    } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                        field.set(newObject, Double.valueOf(value.toString()));
                    }
                    // 通用Object类型
                    else if (fieldType.equals(Object.class)) {
                        field.set(newObject, value);
                    }


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

    /**
     * 从JSON字符串中解析任务列表
     */
    public static List<String> parseTasksFromJson(String jsonString) {
        List<String> tasks = new ArrayList<>();
        try {
            // 清理可能的markdown标记
            String cleanJson = jsonString.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(cleanJson);
            JsonNode tasksNode = rootNode.get("tasks");

            if (tasksNode != null && tasksNode.isArray()) {
                for (JsonNode taskNode : tasksNode) {
                    String task = taskNode.asText().trim();
                    if (!task.isEmpty()) {
                        tasks.add(task);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析任务JSON失败: {}", jsonString, e);
        }
        return tasks;
    }


    /**
     * 从JSON字符串中解析任务关系
     */
    public static List<Side> parseRelationsFromJson(String jsonString) {
        List<Side> relations = new ArrayList<>();
        try {
            // 清理可能的markdown标记
            String cleanJson = jsonString.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(cleanJson);
            JsonNode relationsNode = rootNode.get("relations");

            if (relationsNode != null && relationsNode.isArray()) {
                for (JsonNode relationNode : relationsNode) {
                    String relationString = relationNode.asText();
                    String[] parts = relationString.split(":");

                    if (parts.length == 2) {
                        String from = parts[0].trim();
                        String to = parts[1].trim();
                        if (!from.isEmpty() && !to.isEmpty()) {
                            relations.add(new Side(from, to));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析关系JSON失败: {}", jsonString, e);
        }
        return relations;
    }





}
