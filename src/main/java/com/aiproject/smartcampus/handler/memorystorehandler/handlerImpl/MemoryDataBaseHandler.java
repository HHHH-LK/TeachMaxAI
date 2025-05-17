package com.aiproject.smartcampus.handler.memorystorehandler.handlerImpl;

import com.aiproject.smartcampus.handler.memorystorehandler.Handler;

import com.aiproject.smartcampus.mapper.AIMapper;
import com.aiproject.smartcampus.pojo.entity.HandlerResponse;
import com.aiproject.smartcampus.pojo.entity.Handlerquery;
import com.aiproject.smartcampus.pojo.entity.LocalMessage;
import com.aiproject.smartcampus.commons.utils.ChatMemoryBuilder;
import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @program: lecture-langchain-20250525
 * @description: 数据层处理（将本地数据库中消息同步到query中）
 * @author: lk
 * @create: 2025-05-11 11:17
 **/

@Slf4j
@Transactional
@RequiredArgsConstructor
public class MemoryDataBaseHandler extends Handler {

    private static final int MAX_TTL_DAYS = 7;
    private final AIMapper aiMapper;

    @Override
    public void getMessagesHandle(Handlerquery query, HandlerResponse response) {
        if (Boolean.FALSE.equals(response.getIsSuccess())) {
            log.error("跳过 DB 读取：前置校验失败");
            return;
        }
        String userId = query.getUserId();
        // 1. 读出该 userId 所有消息
        List<LocalMessage> localMemories =
                aiMapper.selectList(new LambdaQueryWrapper<LocalMessage>()
                        .eq(LocalMessage::getUserId, userId));

        // 2. 过滤掉超过 TTL 天的记录
        LocalDateTime threshold = LocalDateTime.now().minusDays(MAX_TTL_DAYS);
        List<ChatMessage> allChatMessages = localMemories.stream()
                .filter(m -> m.getCreateTime().isAfter(threshold))
                .flatMap(m -> ChatMessageDeserializer
                        .messagesFromJson(m.getContent()).stream())
                .collect(Collectors.toList());

        // 3. 构建并注入 ChatMemory
        ChatMemory chatMemory = ChatMemoryBuilder
                .buildChatMemory(allChatMessages, 10);
        Map<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();
        memoryMap.put(userId, chatMemory);

        // 4. 更新响应
        response.setMemoryMap(memoryMap);
        response.setResult(chatMemory.messages());
        response.setIsSuccess(true);
    }

    @Override
    public void updateMessagesHandle(Handlerquery query, HandlerResponse response) {
        if (Boolean.FALSE.equals(response.getIsSuccess())) {
            log.error("跳过 DB 更新：前置校验失败");
            return;
        }

        String userId = query.getUserId();
        List<ChatMessage> newMessages = query.getChatMessageList();
        if (CollectionUtils.isEmpty(newMessages)) {
            response.setIsSuccess(false);
            response.setErrorMsg("消息列表为空，无法更新");
            log.error("更新流程错误：消息列表为空");
            return;
        }

        // 1. 删除老记录（基于 userId）
        aiMapper.delete(new LambdaQueryWrapper<LocalMessage>()
                .eq(LocalMessage::getUserId, userId));

        // 2. 插入新记录
        LocalMessage record = new LocalMessage();
        record.setUserId(userId);
        record.setContent(ChatMessageSerializer.messagesToJson(newMessages));
        record.setCreateTime(LocalDateTime.now());
        int inserted = aiMapper.insert(record);
        if (inserted <= 0) {
            response.setIsSuccess(false);
            response.setErrorMsg("数据库插入失败");
            log.error("数据库插入失败");
            return;
        }

        // 3. 更新内存映射
        ChatMemory chatMemory = ChatMemoryBuilder.buildChatMemory(newMessages, 10);
        response.getMemoryMap().put(userId, chatMemory);
        response.setResult(chatMemory.messages());
        response.setIsSuccess(true);
    }

    @Override
    public void deleteMessagesHandle(Handlerquery query, HandlerResponse response) {
        if (Boolean.FALSE.equals(response.getIsSuccess())) {
            log.error("跳过 DB 删除：前置校验失败");
            return;
        }

        String userId = query.getUserId();
        aiMapper.delete(new LambdaQueryWrapper<LocalMessage>()
                .eq(LocalMessage::getUserId, userId));

        // 同步内存映射
        response.getMemoryMap().remove(userId);
        response.setIsSuccess(true);
    }
}
