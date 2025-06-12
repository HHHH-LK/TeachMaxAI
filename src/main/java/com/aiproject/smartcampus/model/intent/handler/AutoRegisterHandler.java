package com.aiproject.smartcampus.model.intent.handler;

import com.aiproject.smartcampus.commons.client.HandlerRegiserCilent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @program: SmartCampus
 * @description: 自动注册处理（处理器实现）- 优化版
 * @author: lk
 * @create: 2025-05-28 00:51
 **/
@Order(5)
@Slf4j
@Component
public abstract class AutoRegisterHandler implements Handler {

    @Autowired
    private HandlerRegiserCilent handlerRegisterClient;

    // 处理器类型枚举
    public enum HandlerType {
        RAG("增强检索处理器", 1),
        TOOL("工具函数处理器", 2),
        LLM("大语言模型处理器", 3),
        MEMORY("记忆处理器", 4),
        GENERAL("通用处理器", 5);

        private final String description;
        private final int priority;

        HandlerType(String description, int priority) {
            this.description = description;
            this.priority = priority;
        }

        public String getDescription() { return description; }
        public int getPriority() { return priority; }
    }

    @PostConstruct
    public void register() {
        try {
            String functionDescription = getFunctionDescription();
            if (functionDescription == null || functionDescription.trim().isEmpty()) {
                log.error("{}缺少功能描述，自动注册失败", this.getClass().getSimpleName());
                return;
            }

            // 获取处理器类型
            HandlerType handlerType = determineHandlerType(functionDescription);

            // 生成意图列表
            List<String> intentList = generateIntentList(functionDescription, handlerType);

            // 注册主功能描述和所有意图
            handlerRegisterClient.addWithIntents(
                    functionDescription,
                    this,
                    intentList.toArray(new String[0])
            );

            log.info("{}自动注册成功 - 类型: {}, 意图数: {}",
                    this.getClass().getSimpleName(),
                    handlerType.name(),
                    intentList.size());

        } catch (Exception e) {
            log.error("{}自动注册失败: {}", this.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 获取功能描述 - 支持多种方式
     */
    private String getFunctionDescription() throws Exception {
        // 1. 尝试通过反射获取 functionDescription 字段
        try {
            Field field = this.getClass().getDeclaredField("functionDescription");
            field.setAccessible(true);
            Object value = field.get(this);
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return (String) value;
            }
        } catch (NoSuchFieldException ignored) {
            // 字段不存在，继续尝试其他方式
        }

        // 2. 尝试通过方法获取
        try {
            return (String) this.getClass().getMethod("getFunctionDescription").invoke(this);
        } catch (Exception ignored) {
            // 方法不存在或调用失败
        }

        // 3. 使用类名生成默认描述
        String className = this.getClass().getSimpleName();
        if (className.endsWith("Handler")) {
            className = className.substring(0, className.length() - 7);
        }
        return className + "处理器";
    }

    /**
     * 确定处理器类型
     */
    private HandlerType determineHandlerType(String description) {
        String normalizedDesc = description.toLowerCase();

        if (normalizedDesc.contains("增强检索") || normalizedDesc.contains("知识库") ||
                normalizedDesc.contains("rag") || normalizedDesc.contains("检索")) {
            return HandlerType.RAG;
        }

        if (normalizedDesc.contains("工具") || normalizedDesc.contains("功能操作") ||
                normalizedDesc.contains("function") || normalizedDesc.contains("调用")) {
            return HandlerType.TOOL;
        }

        if (normalizedDesc.contains("大语言模型") || normalizedDesc.contains("对话") ||
                normalizedDesc.contains("llm") || normalizedDesc.contains("生成")) {
            return HandlerType.LLM;
        }

        if (normalizedDesc.contains("记忆") || normalizedDesc.contains("上下文") ||
                normalizedDesc.contains("历史") || normalizedDesc.contains("memory")) {
            return HandlerType.MEMORY;
        }

        return HandlerType.GENERAL;
    }

    /**
     * 生成意图列表 - 基于处理器类型和自定义规则
     */
    private List<String> generateIntentList(String description, HandlerType type) {
        Set<String> intents = new LinkedHashSet<>(); // 使用Set避免重复，LinkedHashSet保持顺序

        // 基础意图
        intents.addAll(getBaseIntents(type));

        // 场景意图
        intents.addAll(getScenarioIntents(type));

        // 自定义意图（子类可重写）
        intents.addAll(getCustomIntents(description, type));

        // 动态意图（基于描述内容）
        intents.addAll(getDynamicIntents(description));

        return new ArrayList<>(intents);
    }

    /**
     * 获取基础意图 - 每种类型的核心关键词
     */
    private List<String> getBaseIntents(HandlerType type) {
        switch (type) {
            case RAG:
                return Arrays.asList(
                        "检索", "搜索", "查找", "知识库", "查询", "获取信息",
                        "文档查找", "资料搜索", "知识检索", "信息获取"
                );
            case TOOL:
                return Arrays.asList(
                        "工具调用", "执行", "调用", "计算", "转换", "处理",
                        "功能执行", "工具使用", "数据处理", "操作执行"
                );
            case LLM:
                return Arrays.asList(
                        "对话", "问答", "聊天", "回答", "生成", "分析", "解释",
                        "文本生成", "智能对话", "内容创作", "问题解答"
                );
            case MEMORY:
                return Arrays.asList(
                        "记忆", "上下文", "历史", "之前", "继续",
                        "历史对话", "上下文管理", "记忆检索", "对话历史"
                );
            default:
                return Arrays.asList("通用处理", "默认处理");
        }
    }

    /**
     * 获取场景意图 - 更复杂的使用场景
     */
    private List<String> getScenarioIntents(HandlerType type) {
        switch (type) {
            case RAG:
                return Arrays.asList(
                        "在知识库中查找信息", "从文档中获取内容", "检索相关资料",
                        "查找关于某个主题的信息", "获取特定领域的知识"
                );
            case TOOL:
                return Arrays.asList(
                        "调用外部工具", "执行特定功能", "使用系统功能",
                        "进行数据计算", "执行格式转换"
                );
            case LLM:
                return Arrays.asList(
                        "生成文本内容", "分析问题情况", "解释复杂概念",
                        "创作文档报告", "进行智能对话"
                );
            case MEMORY:
                return Arrays.asList(
                        "基于历史对话", "继续之前的话题", "查看对话历史",
                        "恢复上下文信息", "回忆之前的内容"
                );
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 获取自定义意图 - 子类可重写此方法添加特定意图
     */
    protected List<String> getCustomIntents(String description, HandlerType type) {
        return Collections.emptyList();
    }

    /**
     * 基于描述动态生成意图
     */
    private List<String> getDynamicIntents(String description) {
        List<String> dynamicIntents = new ArrayList<>();
        String normalized = description.toLowerCase();

        // 提取描述中的关键动词和名词
        if (normalized.contains("管理")) {
            dynamicIntents.add("管理操作");
        }
        if (normalized.contains("监控")) {
            dynamicIntents.add("监控任务");
        }
        if (normalized.contains("分析")) {
            dynamicIntents.add("数据分析");
        }
        if (normalized.contains("报告")) {
            dynamicIntents.add("生成报告");
        }
        if (normalized.contains("通知")) {
            dynamicIntents.add("发送通知");
        }

        return dynamicIntents;
    }

    /**
     * 获取处理器注册信息 - 用于调试
     */
    public String getRegistrationInfo() {
        try {
            String description = getFunctionDescription();
            HandlerType type = determineHandlerType(description);
            List<String> intents = generateIntentList(description, type);

            return String.format(
                    "处理器: %s\n类型: %s\n描述: %s\n注册意图数量: %d\n意图列表: %s",
                    this.getClass().getSimpleName(),
                    type.name(),
                    description,
                    intents.size(),
                    String.join(", ", intents)
            );
        } catch (Exception e) {
            return "获取注册信息失败: " + e.getMessage();
        }
    }

    /**
     * 验证处理器注册状态
     */
    public boolean validateRegistration() {
        try {
            String description = getFunctionDescription();
            if (description == null || description.trim().isEmpty()) {
                log.warn("{}缺少功能描述", this.getClass().getSimpleName());
                return false;
            }

            Handler registeredHandler = handlerRegisterClient.getHandlerByUsage(description);
            boolean isRegistered = this.equals(registeredHandler);

            if (!isRegistered) {
                log.warn("{}注册状态异常", this.getClass().getSimpleName());
            }

            return isRegistered;
        } catch (Exception e) {
            log.error("验证{}注册状态失败: {}", this.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 重新注册处理器
     */
    public void reRegister() {
        try {
            String description = getFunctionDescription();
            if (description != null && !description.trim().isEmpty()) {
                // 先移除现有注册
                handlerRegisterClient.remove(description);
                // 重新注册
                register();
                log.info("{}重新注册成功", this.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("{}重新注册失败: {}", this.getClass().getSimpleName(), e.getMessage());
        }
    }
}