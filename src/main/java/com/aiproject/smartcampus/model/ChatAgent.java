package com.aiproject.smartcampus.model;

import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.intent.Intent;
import com.aiproject.smartcampus.model.store.UnifiedMemoryManager;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatAgent {

    private final Intent intentHandler;
    private final UnifiedMemoryManager memoryManager;

    // 直接调用大模型
    private final ChatLanguageModel llm;

    // 默认检索参数（仅取最相关的几轮）
    private static final UnifiedMemoryManager.MemorySearchOptions DEFAULT_SEARCH_OPTS =
            UnifiedMemoryManager.MemorySearchOptions.builder()
                    .topK(3)
                    .minScore(0.15)
                    .neighborWindow(0)
                    .build();

    private enum Route {LLM, AGENT}

    /**
     * 智能体入口方法（带路由 + 检索）
     */
    public String start(String userMessage) {
        String userId = getCurrentUserId();
        log.info("=== 智能体处理开始（带路由） ===");
        log.info("用户ID: {}, 用户消息: {}", userId, userMessage);

        try {
            // 1) 相关记忆检索（只保留和当前问题相关的短片段，降低幻觉）
            UnifiedMemoryManager.MemorySearchResult searchResult =
                    memoryManager.searchRelevantMemory(userId, userMessage, DEFAULT_SEARCH_OPTS);
            List<ChatMessage> relevantMessages = searchResult.getMatchedMessages();
            String memoryContext = memoryManager.searchRelevantMemoryAsContext(userId, userMessage, DEFAULT_SEARCH_OPTS);

            log.info("检索到相关记忆轮次: {}", searchResult.getMatchedTurnsCount());

            if (log.isDebugEnabled() && !relevantMessages.isEmpty()) {
                log.debug("相关记忆（最多前4条）：");
                for (int i = 0; i < Math.min(4, relevantMessages.size()); i++) {
                    ChatMessage msg = relevantMessages.get(i);
                    String txt = msg.text();
                    log.debug(" [{}] {}: {}", i, msg.type(), txt.length() > 60 ? txt.substring(0, 60) + "..." : txt);
                }
            }

            // 2) 设置线程上下文（仅保留相关记忆，避免噪声）
            UserLocalThreadUtils.setUserId(userId);
            UserLocalThreadUtils.setUserMemory(relevantMessages);

            // 3) 路由决策：LLM vs Agent
            Route route = decideRoute(userMessage, searchResult);
            log.info("路由决策：{}", route);

            String answer;

            if (route == Route.LLM) {
                // 3.1 直接调用 LLM（附带相关记忆片段，要求“不知道就说不知道”）
                answer = callLLM(userMessage, memoryContext);

                // 回退机制：LLM失败或返回空，改走Agent
                if (answer == null || answer.trim().isEmpty()) {
                    log.warn("直接LLM返回为空，回退到Agent处理");
                    answer = processIntentWithMemory(userMessage, relevantMessages);
                }

            } else {
                // 3.2 走智能体意图链路（传入相关记忆）
                answer = processIntentWithMemory(userMessage, relevantMessages);
            }

            // 4) 会话落库（统一存储）
            log.info("准备存储会话记录到记忆");
            memoryManager.storeConversation(userId, userMessage, answer);

            // 5) 校验存储结果（可选）
            List<ChatMessage> updatedMemory = memoryManager.getMemoryMessages(userId);
            log.info("存储后记忆数量: {}", updatedMemory.size());

            log.info("=== 智能体处理完成 ===");
            return answer;

        } catch (Exception e) {
            log.error("=== 智能体处理失败 ===", e);
            String errorResponse = "抱歉，我遇到了一些问题，请稍后再试。";
            try {
                memoryManager.storeConversation(userId, userMessage, errorResponse);
            } catch (Exception memoryError) {
                log.error("错误场景下记忆存储也失败", memoryError);
            }
            return errorResponse;
        } finally {
            UserLocalThreadUtils.clear();
        }
    }

    /**
     * 路由决策：
     * - 与校园业务强相关且可能需要工具/系统/数据的，走Agent
     * - 一般问答/写作/解释/翻译等通用能力，走LLM
     */
    private Route decideRoute(String userMessage, UnifiedMemoryManager.MemorySearchResult searchResult) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Route.LLM;
        }
        String msg = userMessage.toLowerCase(Locale.ROOT);

        // 强触发 Agent 的校园业务关键词（可按需扩充）
        if (containsAny(userMessage,
                "选课", "课表", "成绩", "成绩单", "考试", "补考", "教务", "教务系统",
                "报修", "维修", "预约", "请假", "审批", "门禁", "一卡通", "校园卡", "充值", "缴费", "发票",
                "绑定", "解绑", "宿舍", "空教室", "借用教室", "图书馆借书", "续借", "还书", "借书",
                "导航", "到哪", "定位", "工单", "报到", "注册", "绑定学号", "学号", "工号", "校园网", "wifi",
                "打卡", "签到", "出入", "门禁", "财务", "报销", "课程查询", "导师联系")) {
            return Route.AGENT;
        }

        // 明显属于通用能力的关键词：优先 LLM
        if (containsAny(userMessage,
                "总结", "翻译", "润色", "改写", "解释", "比较", "推荐", "写", "生成", "示例",
                "代码", "范文", "笑话", "故事", "科普", "是什么", "怎么", "如何", "为什么")) {
            return Route.LLM;
        }

        // 提到校园实体但不明确是操作，倾向 Agent（可避免事实类幻觉）
        if (containsAny(userMessage, "图书馆", "食堂", "宿舍", "操场", "教室", "实验室", "校医院", "学院", "导师", "课程", "专业", "学分")) {
            return Route.AGENT;
        }

        // 默认：直接 LLM
        return Route.LLM;
    }

    private boolean containsAny(String text, String... kws) {
        if (text == null) return false;
        for (String kw : kws) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /**
     * 直接调用LLM（使用相关记忆作为上下文，要求模型信息不足时必须坦诚不知道）
     */
    private String callLLM(String userMessage, String memoryContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个严谨的校园助手。只使用我提供的“相关历史对话片段”作为背景知识；")
                .append("如果信息不足，请明确说明不知道或需要更多信息，避免臆造。\n");

        if (memoryContext != null && !memoryContext.isEmpty()) {
            prompt.append("\n【相关历史对话片段】\n").append(memoryContext).append("\n");
        }

        prompt.append("\n【当前用户问题】\n").append(userMessage).append("\n");

        try {
            return llm.generate(prompt.toString());
        } catch (Exception e) {
            log.error("直接LLM调用失败", e);
            return null;
        }
    }

    /**
     * 带记忆的意图处理：改为只传“与本次问题相关”的记忆片段
     */
    private String processIntentWithMemory(String userMessage, List<ChatMessage> relevantMessages) {
        try {
            if (intentHandler instanceof MemoryAwareIntent) {
                log.info("使用支持记忆的意图处理器（相关片段条数：{}）", relevantMessages == null ? 0 : relevantMessages.size());
                return ((MemoryAwareIntent) intentHandler).handlerIntentWithMemory(userMessage, relevantMessages);
            }

            String enhancedMessage = buildEnhancedMessage(userMessage, relevantMessages);
            log.info("构建增强消息，原长度: {}, 增强后长度: {}", userMessage.length(), enhancedMessage.length());
            return intentHandler.handlerIntent(enhancedMessage);

        } catch (Exception e) {
            log.warn("带记忆的意图处理失败，回退到普通处理", e);
            return intentHandler.handlerIntent(userMessage);
        }
    }

    /**
     * 仅用“相关记忆片段”构建增强消息，避免无关内容导致幻觉
     */
    private String buildEnhancedMessage(String userMessage, List<ChatMessage> relevantMessages) {
        if (relevantMessages == null || relevantMessages.isEmpty()) {
            return userMessage;
        }

        StringBuilder enhanced = new StringBuilder();
        enhanced.append("【相关历史对话片段】\n");

        // 相关片段通常已经很少，可直接展开；如需再控长，可加截断
        for (int i = 0; i < relevantMessages.size(); i += 2) {
            ChatMessage userMsg = relevantMessages.get(i);
            ChatMessage aiMsg = (i + 1 < relevantMessages.size()) ? relevantMessages.get(i + 1) : null;

            enhanced.append("用户: ").append(userMsg.text()).append("\n");
            if (aiMsg != null) {
                String aiReply = aiMsg.text();
                if (aiReply.length() > 200) aiReply = aiReply.substring(0, 200) + "...";
                enhanced.append("助手: ").append(aiReply).append("\n");
            }
            enhanced.append("---\n");
        }

        enhanced.append("\n【当前用户问题】\n").append(userMessage)
                .append("\n\n指令：请严格基于以上相关历史回答；若信息不足，请说明不知道并指出所需信息。");

        return enhanced.toString();
    }

    /**
     * 获取当前用户ID（与原逻辑一致）
     */
    private String getCurrentUserId() {
        try {
            if (UserLocalThreadUtils.getUserInfo() != null &&
                    UserLocalThreadUtils.getUserInfo().getUserId() != null) {
                String userId = UserLocalThreadUtils.getUserInfo().getUserId().toString();
                log.debug("从UserLocalThreadUtils获取用户ID: {}", userId);
                return userId;
            }
        } catch (Exception e) {
            log.debug("从UserLocalThreadUtils获取用户ID失败: {}", e.getMessage());
        }
        String defaultUserId = "default_user_1";
        log.debug("使用默认用户ID: {}", defaultUserId);
        return defaultUserId;
    }

    // === 其他公开方法保持不变 ===

    public UnifiedMemoryManager.MemoryStats getUserMemoryStats(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        return memoryManager.getMemoryStats(targetUserId);
    }

    public void clearUserMemory(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        memoryManager.clearUserMemory(targetUserId);
    }

    public List<ChatMessage> getFullMemoryForDebug(String userId) {
        String targetUserId = userId != null ? userId : getCurrentUserId();
        return memoryManager.getMemoryMessages(targetUserId);
    }

    /**
     * 接口：支持记忆的意图处理器
     */
    public interface MemoryAwareIntent {
        String handlerIntentWithMemory(String userMessage, List<ChatMessage> memoryMessages);
    }
}