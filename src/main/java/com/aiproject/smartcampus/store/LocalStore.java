package com.aiproject.smartcampus.store;

import com.aiproject.smartcampus.exception.MemoryExpection;
import com.aiproject.smartcampus.handler.memorystorehandler.handlerImpl.MemoryDataBaseMemoryStoreHandler;
import com.aiproject.smartcampus.handler.memorystorehandler.handlerImpl.MemoryIdMemoryStoreHandler;
import com.aiproject.smartcampus.mapper.AIMapper;
import com.aiproject.smartcampus.pojo.bo.handlerentity.MemoryStoreHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.MemoryStoreHandlerquery;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: lecture-langchain-20250525
 * @description: 本地数据库存储消息
 * @author: lk
 * @create: 2025-05-11 10:15
 **/

@Component
@RequiredArgsConstructor
public class LocalStore implements ChatMemoryStore {

    private final AIMapper aiMapper;
    // 全局内存映射，保证在 Handler 链之前已初始化
    private final Map<String, ChatMemory> memoryMap = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(Object userIdObj) {
        String userId = userIdObj.toString();
        MemoryStoreHandlerquery query = new MemoryStoreHandlerquery();
        query.setUserId(userId);
        MemoryStoreHandlerResponse response = new MemoryStoreHandlerResponse();
        // 预先初始化内存映射，防止 null
        response.setMemoryMap(memoryMap);
        // 构建链并执行
        new MemoryIdMemoryStoreHandler()
                .setNextHandler(new MemoryDataBaseMemoryStoreHandler(aiMapper))
                .getMessagesHandle(query, response);

        if (!response.getIsSuccess()) {
            throw new MemoryExpection("获取消息失败: " + response.getErrorMsg());
        }
        return response.getResult();
    }

    @Override
    public void updateMessages(Object userIdObj, List<ChatMessage> list) {
        String userId = userIdObj.toString();
        MemoryStoreHandlerquery query = new MemoryStoreHandlerquery();
        query.setUserId(userId);
        query.setChatMessageList(list);
        MemoryStoreHandlerResponse response = new MemoryStoreHandlerResponse();
        response.setMemoryMap(memoryMap);

        new MemoryIdMemoryStoreHandler()
                .setNextHandler(new MemoryDataBaseMemoryStoreHandler(aiMapper))
                .updateMessagesHandle(query, response);

        if (!response.getIsSuccess()) {
            throw new MemoryExpection("更新消息失败: " + response.getErrorMsg());
        }
    }

    @Override
    public void deleteMessages(Object userIdObj) {
        String userId = userIdObj.toString();
        MemoryStoreHandlerquery query = new MemoryStoreHandlerquery();
        query.setUserId(userId);
        MemoryStoreHandlerResponse response = new MemoryStoreHandlerResponse();
        response.setMemoryMap(memoryMap);

        new MemoryIdMemoryStoreHandler()
                .setNextHandler(new MemoryDataBaseMemoryStoreHandler(aiMapper))
                .deleteMessagesHandle(query, response);

        if (!response.getIsSuccess()) {
            throw new MemoryExpection("删除消息失败: " + response.getErrorMsg());
        }
    }
}

