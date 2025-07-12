package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.model.ChatAgent;
import com.aiproject.smartcampus.pojo.dto.ChatDTO;
import com.aiproject.smartcampus.pojo.dto.ChatStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatAgent chatAgent;
    private final ChatSSEController chatSSEController;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

    /**
     * 原有的同步聊天接口，保持不变
     */
    @PostMapping("/userchat")
    public Result userchat(@RequestBody ChatDTO chatDTO) {
        log.info("用户问题：{}", chatDTO.getQuestion());
        String start = chatAgent.start(chatDTO.getQuestion());
        String trim = start.trim();
        return Result.success(trim);
    }

    /**
     * SSE流式聊天接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatDTO chatDTO) {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时

        log.info("开始流式处理用户问题：{}", chatDTO.getQuestion());

        // 异步处理聊天请求
        CompletableFuture.runAsync(() -> {
            try {
                // 获取用户ID
                String userId = getCurrentUserId();

                // 设置用户上下文
                UserLocalThreadUtils.setUserMemory(chatDTO.getQuestion());

                // 发送开始处理的消息
                sendStreamMessage(emitter, "thinking", "正在思考中...");

                // 模拟流式输出：调用ChatAgent获取完整回答
                String fullAnswer = chatAgent.start(chatDTO.getQuestion());

                // 将完整答案分块流式输出
                streamAnswer(emitter, fullAnswer);

                // 发送完成消息
                sendStreamMessage(emitter, "completed", "回答完成");

                // 完成流
                emitter.complete();

            } catch (Exception e) {
                log.error("流式处理失败", e);
                try {
                    sendStreamMessage(emitter, "error", "抱歉，我遇到了一些问题，请稍后再试。");
                    emitter.complete();
                } catch (IOException ioException) {
                    log.error("发送错误消息失败", ioException);
                    emitter.completeWithError(ioException);
                }
            }
        }, executor);

        // 设置回调
        emitter.onCompletion(() -> log.info("流式聊天完成"));
        emitter.onTimeout(() -> log.warn("流式聊天超时"));
        emitter.onError((e) -> log.error("流式聊天错误", e));

        return emitter;
    }

    /**
     * 通过用户ID进行流式聊天（结合SSE Controller）
     */
    @PostMapping("/stream/{userId}")
    public Result streamChatByUserId(@PathVariable String userId, @RequestBody ChatDTO chatDTO) {
        log.info("用户 {} 开始流式聊天：{}", userId, chatDTO.getQuestion());

        // 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                // 发送处理中状态
                ChatStreamMessage thinkingMsg = new ChatStreamMessage();
                thinkingMsg.setMessageType("thinking");
                thinkingMsg.setContent("正在思考中...");
                thinkingMsg.setUserId(userId);
                thinkingMsg.setTimestamp(System.currentTimeMillis());
                chatSSEController.pushStreamMessageToUser(userId, thinkingMsg);

                // 设置用户上下文
                UserLocalThreadUtils.setUserMemory(chatDTO.getQuestion());

                // 获取完整回答
                String fullAnswer = chatAgent.start(chatDTO.getQuestion());

                // 流式推送答案
                streamAnswerToUser(userId, fullAnswer);

                // 发送完成状态
                ChatStreamMessage completedMsg = new ChatStreamMessage();
                completedMsg.setMessageType("completed");
                completedMsg.setContent("回答完成");
                completedMsg.setUserId(userId);
                completedMsg.setTimestamp(System.currentTimeMillis());
                chatSSEController.pushStreamMessageToUser(userId, completedMsg);

            } catch (Exception e) {
                log.error("用户 {} 流式聊天失败", userId, e);

                // 发送错误消息
                ChatStreamMessage errorMsg = new ChatStreamMessage();
                errorMsg.setMessageType("error");
                errorMsg.setContent("抱歉，我遇到了一些问题，请稍后再试。");
                errorMsg.setUserId(userId);
                errorMsg.setTimestamp(System.currentTimeMillis());
                chatSSEController.pushStreamMessageToUser(userId, errorMsg);
            }
        }, executor);

        return Result.success("流式处理已开始");
    }

    /**
     * 流式输出答案内容
     */
    private void streamAnswer(SseEmitter emitter, String answer) {
        if (answer == null || answer.isEmpty()) {
            return;
        }

        // 按字符或词语分割进行流式输出
        String[] words = answer.split("(?<=\\p{P})|(?=\\p{P})|\\s+");

        for (int i = 0; i < words.length; i++) {
            try {
                // 模拟打字效果的延迟
                Thread.sleep(50); // 50ms延迟

                sendStreamMessage(emitter, "chunk", words[i]);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                log.error("流式输出失败", e);
                break;
            }
        }
    }

    /**
     * 向指定用户流式推送答案
     */
    private void streamAnswerToUser(String userId, String answer) {
        if (answer == null || answer.isEmpty()) {
            return;
        }

        String[] words = answer.split("(?<=\\p{P})|(?=\\p{P})|\\s+");

        for (String word : words) {
            try {
                // 模拟打字效果的延迟
                Thread.sleep(50);

                ChatStreamMessage chunkMsg = new ChatStreamMessage();
                chunkMsg.setMessageType("chunk");
                chunkMsg.setContent(word);
                chunkMsg.setUserId(userId);
                chunkMsg.setTimestamp(System.currentTimeMillis());

                chatSSEController.pushStreamMessageToUser(userId, chunkMsg);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 发送流式消息
     */
    private void sendStreamMessage(SseEmitter emitter, String eventType, String content) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventType)
                .data(content));
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            if (UserLocalThreadUtils.getUserInfo() != null &&
                    UserLocalThreadUtils.getUserInfo().getUserId() != null) {
                return UserLocalThreadUtils.getUserInfo().getUserId().toString();
            }
        } catch (Exception e) {
            log.debug("从UserLocalThreadUtils获取用户ID失败: {}", e.getMessage());
        }

        return "default_user_1";
    }
}