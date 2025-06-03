
// 2. 增强的HandlerRegiser - 支持模糊匹配和智能路由
package com.aiproject.smartcampus.commons;

import com.aiproject.smartcampus.model.intent.handler.Handler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Order(4)
@Slf4j
@Component
public class HandlerRegiserCilent {

    // 精确匹配映射
    private static final Map<String, Handler> exactMatchMap = new HashMap<>();
    // 关键词匹配映射
    private static final Map<String, Handler> keywordMatchMap = new HashMap<>();
    private static final Map<Handler, String> handlerDescriptionMap = new HashMap<>();
    public static final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 注册一个 Handler
     */
    public void add(String usage, Handler handler) {
        lock.writeLock().lock();
        try {
            exactMatchMap.put(usage, handler);

            // 如果是完整的功能描述，同时注册到关键词映射
            if (usage.contains("处理器")) {
                handlerDescriptionMap.put(handler, usage);
                // 添加关键词映射
                addKeywordMappings(usage, handler);
            }

            log.debug("服务[{}]（{}）添加成功", usage, handler.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("添加服务失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 添加关键词映射
     */
    private void addKeywordMappings(String description, Handler handler) {
        // 基于描述内容添加关键词映射
        if (description.contains("增强检索") || description.contains("知识库")) {
            keywordMatchMap.put("检索", handler);
            keywordMatchMap.put("搜索", handler);
            keywordMatchMap.put("查找", handler);
            keywordMatchMap.put("知识库", handler);
        }
        if (description.contains("工具") || description.contains("功能操作")) {
            keywordMatchMap.put("工具", handler);
            keywordMatchMap.put("调用", handler);
            keywordMatchMap.put("执行", handler);
            keywordMatchMap.put("计算", handler);
        }
        if (description.contains("大语言模型") || description.contains("对话")) {
            keywordMatchMap.put("对话", handler);
            keywordMatchMap.put("聊天", handler);
            keywordMatchMap.put("问答", handler);
            keywordMatchMap.put("回答", handler);
        }
        if (description.contains("记忆") || description.contains("上下文")) {
            keywordMatchMap.put("记忆", handler);
            keywordMatchMap.put("上下文", handler);
            keywordMatchMap.put("历史", handler);
            keywordMatchMap.put("之前", handler);
        }
    }

    /**
     * 智能获取Handler - 专门针对分解后的意图进行匹配
     *
     * @param intent 分解后的意图描述，如："在知识库中搜索关于Java的信息"
     * @return 匹配的Handler实例
     */
    public Handler getHandler(String intent) {
        lock.readLock().lock();
        try {
            log.debug("正在查找处理器，意图描述: {}", intent);
            // 1. 精确匹配（处理完全一致的情况）
            Handler exactHandler = exactMatchMap.get(intent);
            if (exactHandler != null) {
                log.debug("精确匹配成功: {} -> {}", intent, exactHandler.getClass().getSimpleName());
                return exactHandler;
            }
            // 2. 基于意图特征的智能匹配（核心逻辑）
            Handler matchedHandler = matchByIntentPattern(intent);
            if (matchedHandler != null) {
                log.info("意图匹配成功: [{}] -> {}", intent, matchedHandler.getClass().getSimpleName());
                return matchedHandler;
            }
            // 3. 降级到关键词匹配
            Handler keywordHandler = findBestMatch(intent);
            if (keywordHandler != null) {
                log.info("关键词匹配成功: [{}] -> {}", intent, keywordHandler.getClass().getSimpleName());
                return keywordHandler;
            }
            // 4. 匹配失败处理
            log.warn("未找到匹配的处理器，意图: {}", intent);
            log.warn("当前注册的处理器: {}",
                    handlerDescriptionMap.values().stream().collect(Collectors.joining(", ")));
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 基于意图模式的智能匹配 - 核心匹配逻辑
     */
    private Handler matchByIntentPattern(String intent) {
        // 标准化意图文本（去除标点，转小写）
        String normalizedIntent = intent.toLowerCase()
                .replaceAll("[，。！？、；：\"\"''（）【】]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        // 意图模式匹配规则
        for (Map.Entry<Handler, String> entry : handlerDescriptionMap.entrySet()) {
            Handler handler = entry.getKey();
            String description = entry.getValue();
            // 检查意图是否匹配处理器能力
            if (isIntentMatch(normalizedIntent, description)) {
                return handler;
            }
        }
        return null;
    }

    /**
     * 判断意图是否匹配处理器描述
     */
    private boolean isIntentMatch(String intent, String handlerDescription) {
        // 标准化处理
        String normalizedIntent = intent.toLowerCase()
                .replaceAll("[，。！？、；：\"\"''（）【】]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String normalizedDesc = handlerDescription.toLowerCase();

        // RAG处理器匹配规则
        if (normalizedDesc.contains("增强检索") || normalizedDesc.contains("知识库")) {
            return normalizedIntent.contains("检索") || normalizedIntent.contains("搜索") ||
                    normalizedIntent.contains("查找") || normalizedIntent.contains("知识库") ||
                    normalizedIntent.contains("查询") || normalizedIntent.contains("获取") ||
                    normalizedIntent.matches(".*在.*中.*关于.*");
        }

        // 工具函数处理器匹配规则
        if (normalizedDesc.contains("工具函数") || normalizedDesc.contains("功能操作")) {
            return normalizedIntent.contains("调用") || normalizedIntent.contains("执行") ||
                    normalizedIntent.contains("使用") || normalizedIntent.contains("计算") ||
                    normalizedIntent.contains("转换") || normalizedIntent.contains("处理") ||
                    normalizedIntent.matches(".*使用.*工具.*") || normalizedIntent.matches(".*调用.*执行.*");
        }

        // LLM对话处理器匹配规则
        if (normalizedDesc.contains("大语言模型") || normalizedDesc.contains("一般性对话") ||
                normalizedDesc.contains("llm")) {
            return normalizedIntent.contains("生成") || normalizedIntent.contains("回答") ||
                    normalizedIntent.contains("分析") || normalizedIntent.contains("解释") ||
                    normalizedIntent.contains("建议") || normalizedIntent.contains("比较") ||
                    normalizedIntent.contains("创作") || normalizedIntent.matches(".*基于.*生成.*");
        }

        // 记忆处理器匹配规则 - 重点修复
        if (normalizedDesc.contains("对话记忆") || normalizedDesc.contains("上下文") ||
                normalizedDesc.contains("记忆") || normalizedDesc.contains("历史")) {
            return normalizedIntent.contains("历史") || normalizedIntent.contains("之前") ||
                    normalizedIntent.contains("刚才") || normalizedIntent.contains("上下文") ||
                    normalizedIntent.contains("继续") || normalizedIntent.contains("基于前面") ||
                    normalizedIntent.contains("记忆") ||
                    // 新增：精确匹配你的场景
                    normalizedIntent.contains("对话历史") ||
                    normalizedIntent.contains("自然语言回答") ||
                    normalizedIntent.matches(".*基于.*历史.*生成.*") ||
                    normalizedIntent.matches(".*基于.*记忆.*回答.*") ||
                    normalizedIntent.matches(".*从.*对话.*中.*");
        }

        return false;
    }

    /**
     * 降级关键词匹配算法
     */
    private Handler findBestMatch(String intent) {
        Map<Handler, Integer> scoreMap = new HashMap<>();
        // 计算每个处理器的匹配分数
        for (Map.Entry<String, Handler> entry : keywordMatchMap.entrySet()) {
            String keyword = entry.getKey();
            Handler handler = entry.getValue();
            if (intent.contains(keyword)) {
                scoreMap.put(handler, scoreMap.getOrDefault(handler, 0) + keyword.length());
            }
        }
        // 返回得分最高的处理器
        return scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 根据用途获取单个 Handler（保持向后兼容）
     */
    public Handler getHandlerByUsage(String usage) {
        lock.readLock().lock();
        try {
            return exactMatchMap.get(usage);
        } catch (Exception e) {
            log.error("获取服务[{}]失败", usage, e);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有注册的用途及其 Handler 副本
     */
    public Map<String, Handler> getAllHandlers() {
        lock.readLock().lock();
        try {
            Map<String, Handler> allHandlers = new HashMap<>(exactMatchMap);
            allHandlers.putAll(keywordMatchMap);
            return Collections.unmodifiableMap(allHandlers);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 移除指定用途的 Handler
     */
    public void remove(String usage) {
        lock.writeLock().lock();
        try {
            Handler removedHandler = exactMatchMap.remove(usage);
            keywordMatchMap.values().removeIf(handler -> handler.equals(removedHandler));
            handlerDescriptionMap.remove(removedHandler);
            log.info("服务[{}]移除成功", usage);
        } catch (Exception e) {
            log.error("移除服务[{}]失败", usage, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空所有注册
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            exactMatchMap.clear();
            keywordMatchMap.clear();
            handlerDescriptionMap.clear();
            log.info("已清空所有服务注册");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取注册统计信息（用于调试）
     */
    public String getRegistrationStats() {
        lock.readLock().lock();
        try {
            return String.format("注册统计 - 精确匹配: %d, 关键词匹配: %d, 处理器总数: %d",
                    exactMatchMap.size(), keywordMatchMap.size(), handlerDescriptionMap.size());
        } finally {
            lock.readLock().unlock();
        }
    }
}