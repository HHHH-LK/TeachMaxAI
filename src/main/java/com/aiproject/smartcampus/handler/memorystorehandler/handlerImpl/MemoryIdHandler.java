package com.aiproject.smartcampus.handler.memorystorehandler.handlerImpl;

import com.aiproject.smartcampus.handler.memorystorehandler.Handler;
import com.aiproject.smartcampus.pojo.entity.HandlerResponse;
import com.aiproject.smartcampus.pojo.entity.Handlerquery;
import com.aiproject.smartcampus.commons.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @program: lecture-langchain-20250525
 * @description: id处理（对id处理异常）
 * @author: lk
 * @create: 2025-05-11 10:40
 **/
@Slf4j
public class MemoryIdHandler extends Handler {

    @Override
    public void getMessagesHandle(Handlerquery query, HandlerResponse response) {
        String userId = query.getUserId();
        if (StringUtils.isBlank(userId)) {
            response.setIsSuccess(false);
            response.setErrorMsg("userId is null or blank");
            log.error("{} 流程错误：userId is null or blank", getClass().getSimpleName());
            return;
        }
        response.setIsSuccess(true);
        if (nextHandler != null) {
            nextHandler.getMessagesHandle(query, response);
        }
    }

    @Override
    public void updateMessagesHandle(Handlerquery query, HandlerResponse response) {
        String userId = query.getUserId();
        if (StringUtils.isBlank(userId) || CollectionUtils.isEmpty(query.getChatMessageList())) {
            response.setIsSuccess(false);
            response.setErrorMsg("userId or message list is empty");
            log.error("{} 更新流程错误：userId 或消息列表为空", getClass().getSimpleName());
            return;
        }
        response.setIsSuccess(true);
        if (nextHandler != null) {
            nextHandler.updateMessagesHandle(query, response);
        }
    }

    @Override
    public void deleteMessagesHandle(Handlerquery query, HandlerResponse response) {
        String userId = query.getUserId();
        if (StringUtils.isBlank(userId)) {
            response.setIsSuccess(false);
            response.setErrorMsg("userId is null or blank");
            log.error("{} 删除流程错误：userId is null or blank", getClass().getSimpleName());
            return;
        }
        response.setIsSuccess(true);
        if (nextHandler != null) {
            // 修正：调用下一个 Handler 的删除方法
            nextHandler.deleteMessagesHandle(query, response);
        }
    }
}

