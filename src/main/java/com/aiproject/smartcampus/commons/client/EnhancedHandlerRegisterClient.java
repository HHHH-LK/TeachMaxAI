package com.aiproject.smartcampus.commons.client;

import com.aiproject.smartcampus.model.handler.Handler;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.aiproject.smartcampus.model.prompts.UserPrompts.getHandlerPrompts;


// 改进后的注册中心
@Order(4)
@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedHandlerRegisterClient {

    private final Map<String, Handler> handlerClientMap = new ConcurrentHashMap<>();
    private final ChatLanguageModel chatLanguageModel;

    /**
     * 注册处理器
     */
    public boolean addHandler(String name, Handler handler) {
        if (StrUtil.isBlank(name) || handler == null) {
            log.error("处理器注册失败，信息不完整 name=[{}], handler=[{}]", name, handler);
            return false;
        }

        try {
            Handler existing = handlerClientMap.put(name, handler);
            if (existing != null) {
                log.warn("处理器 [{}] 已存在，已被覆盖", name);
            }
            log.info("处理器 [{}] 注册成功", name);
            return true;
        } catch (Exception e) {
            log.error("处理器 [{}] 注册异常", name, e);
            return false;
        }
    }

    /**
     * 基于任务获取获取处理器
     */
    public Handler getHandler(String task) {
        //智能获取处理器
        String handlerPrompts = getHandlerPrompts(task);
        ChatResponse chat = chatLanguageModel.chat(UserMessage.userMessage(handlerPrompts));
        String text = chat.aiMessage().text();
        //获取处理器
        Handler handlerByName = getHandlerByName(text);
        log.info("获取到的处理器为【{}】", handlerByName);
        return handlerByName;
    }

    /**
     * 基于处理器名字获取处理器
     */
    public Handler getHandlerByName(String name) {
        if (StrUtil.isBlank(name)) {
            log.warn("[{}]处理器不存在,触发兜底机制", name);
            return handlerClientMap.get("StepIntentLLMChatResponseHandler");
        }
        Handler handler = handlerClientMap.get(name);
        if (handler == null) {
            log.warn("[{}]处理器不存在,触发兜底机制", name);
            //兜底机制
            return handlerClientMap.get("StepIntentLLMChatResponseHandler");
        }
        return handler;
    }

    /**
     * 移除处理器
     */
    public boolean removeHandler(String handlerName) {
        if (StrUtil.isBlank(handlerName)) {
            return false;
        }

        Handler removed = handlerClientMap.remove(handlerName);
        if (removed != null) {
            log.info("处理器 [{}] 已移除", handlerName);
            return true;
        }
        return false;
    }

    /**
     * 获取所有处理器名称
     */
    public Set<String> getAllHandlerNames() {
        return new HashSet<>(handlerClientMap.keySet());
    }

    /**
     * 获取处理器数量
     */
    public int getHandlerCount() {
        return handlerClientMap.size();
    }

    /**
     * 检查处理器是否存在
     */
    public boolean containsHandler(String handlerName) {
        return !StrUtil.isBlank(handlerName) && handlerClientMap.containsKey(handlerName);
    }


}

