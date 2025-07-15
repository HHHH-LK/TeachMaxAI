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
import java.util.ArrayList;
import java.util.List;
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
     * SSE流式聊天接口 - 按词汇分组输出
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatDTO chatDTO) {
        SseEmitter emitter = new SseEmitter(600000L); // 600秒超时

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

                // 调用ChatAgent获取完整回答
                String fullAnswer = chatAgent.start(chatDTO.getQuestion());

                // 按词汇分组流式输出
                streamAnswerByWords(emitter, fullAnswer);

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
     * 按词汇分组的流式输出
     */
    private void streamAnswerByWords(SseEmitter emitter, String fullAnswer) {
        try {
            if (fullAnswer == null || fullAnswer.trim().isEmpty()) {
                sendStreamMessage(emitter, "content", "暂时没有找到相关信息。");
                return;
            }

            log.debug("开始按词汇分组输出，内容长度：{}", fullAnswer.length());

            // 1. 将文本分割为词汇
            List<String> words = splitIntoWords(fullAnswer);

            log.debug("分割为 {} 个词汇", words.size());

            // 2. 逐词发送
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);

                if (!word.isEmpty()) {
                    // 发送词汇
                    sendStreamMessage(emitter, "content", word);

                    // 根据词汇类型计算延迟
                    int delay = calculateWordDelay(word);

                    log.debug("发送第{}个词：'{}', 延迟：{}ms", i + 1, word.replace("\n", "\\n"), delay);

                    // 词汇间延迟
                    Thread.sleep(delay);
                }
            }

        } catch (InterruptedException e) {
            log.warn("按词汇输出被中断");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("按词汇流式输出失败", e);
            try {
                sendStreamMessage(emitter, "error", "内容输出时出现问题");
            } catch (IOException ioException) {
                log.error("发送错误消息失败", ioException);
            }
        }
    }

    /**
     * 将文本分割为词汇单元
     */
    private List<String> splitIntoWords(String text) {
        List<String> words = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 中文字符 - 单独作为一个词
            if (isChinese(c)) {
                // 先保存之前积累的英文词汇
                if (currentWord.length() > 0) {
                    words.add(currentWord.toString());
                    currentWord = new StringBuilder();
                }
                // 中文字符单独成词
                words.add(String.valueOf(c));
            }
            // 标点符号 - 与前面的内容组合
            else if (isPunctuation(c)) {
                currentWord.append(c);
                words.add(currentWord.toString());
                currentWord = new StringBuilder();
            }
            // 换行符 - 单独处理
            else if (c == '\n') {
                // 先保存当前词汇
                if (currentWord.length() > 0) {
                    words.add(currentWord.toString());
                    currentWord = new StringBuilder();
                }
                // 换行符单独成词
                words.add("\n");
            }
            // 空格 - 作为词汇分隔符
            else if (c == ' ' || c == '\t') {
                // 保存当前词汇
                if (currentWord.length() > 0) {
                    words.add(currentWord.toString());
                    currentWord = new StringBuilder();
                }
                // 空格单独成词
                words.add(String.valueOf(c));
            }
            // 英文字母、数字等 - 组成单词
            else {
                currentWord.append(c);
            }
        }

        // 添加最后积累的词汇
        if (currentWord.length() > 0) {
            words.add(currentWord.toString());
        }

        return words;
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        return (c >= 0x4e00 && c <= 0x9fff) ||          // 基本汉字
                (c >= 0x3400 && c <= 0x4dbf) ||          // 扩展A
                (c >= 0x20000 && c <= 0x2a6df) ||        // 扩展B
                (c >= 0x2a700 && c <= 0x2b73f) ||        // 扩展C
                (c >= 0x2b740 && c <= 0x2b81f) ||        // 扩展D
                (c >= 0x2b820 && c <= 0x2ceaf) ||        // 扩展E
                (c >= 0xf900 && c <= 0xfaff) ||          // 兼容汉字
                (c >= 0x2f800 && c <= 0x2fa1f);          // 兼容扩展
    }

    /**
     * 判断是否为标点符号
     */
    private boolean isPunctuation(char c) {
        // 中文标点
        String chinesePunctuation = "。！？，、；：\"\"''（）【】《》「」『』〈〉〔〕";
        // 英文标点
        String englishPunctuation = ",.!?;:\"'()[]<>{}";
        // 其他常用符号
        String otherSymbols = "…—–-";

        return chinesePunctuation.indexOf(c) != -1 ||
                englishPunctuation.indexOf(c) != -1 ||
                otherSymbols.indexOf(c) != -1;
    }

    /**
     * 计算词汇延迟时间
     */
    private int calculateWordDelay(String word) {
        // 基础延迟
        int baseDelay = 80;

        // 句子结束标点 - 长停顿
        if (word.matches(".*[。！？.!?]$")) {
            return 400;
        }

        // 逗号、分号等 - 中等停顿
        if (word.matches(".*[，、；,;]$")) {
            return 200;
        }

        // 冒号 - 短停顿
        if (word.matches(".*[：:]$")) {
            return 150;
        }

        // 引号、括号等 - 短停顿
        if (word.matches(".*[\"\"''\"'()（）\\[\\]【】]$")) {
            return 100;
        }

        // 换行符 - 段落停顿
        if ("\n".equals(word)) {
            return 250;
        }

        // 空格 - 很短停顿
        if (" ".equals(word) || "\t".equals(word)) {
            return 40;
        }

        // 单个中文字符
        if (word.length() == 1 && isChinese(word.charAt(0))) {
            return baseDelay;
        }

        // 英文单词 - 根据长度调整
        if (word.matches("[a-zA-Z0-9]+")) {
            // 短单词快一些，长单词慢一些
            if (word.length() <= 3) {
                return baseDelay - 20;  // 60ms
            } else if (word.length() <= 6) {
                return baseDelay;       // 80ms
            } else {
                return baseDelay + 30;  // 110ms
            }
        }

        // 数字
        if (word.matches("\\d+")) {
            return baseDelay - 10;
        }

        // 代码相关（包含特殊字符的词汇）
        if (word.matches(".*[{}\\[\\]<>/\\\\=+\\-*%&|^~`].*")) {
            return baseDelay + 20;
        }

        // 其他情况
        return baseDelay;
    }

    /**
     * 词汇类型检测（用于调试和优化）
     */
    private String getWordType(String word) {
        if ("\n".equals(word)) {
            return "换行";
        }
        if (" ".equals(word)) {
            return "空格";
        }
        if (word.length() == 1 && isChinese(word.charAt(0))) {
            return "中文";
        }
        if (word.matches("[a-zA-Z0-9]+")) {
            return "英文词";
        }
        if (word.matches("\\d+")) {
            return "数字";
        }
        if (isPunctuation(word.charAt(word.length() - 1))) {
            return "标点";
        }
        return "其他";
    }

// ============= 可选的优化方法 =============

    /**
     * 获取当前环境的打字速度配置
     * 可以根据用户偏好或系统负载动态调整
     */
    private double getTypingSpeedMultiplier() {
        // 可以从配置文件读取，或根据用户设置调整
        // 1.0 = 正常速度，0.5 = 两倍速度，2.0 = 半速
        return 1.0;
    }

    /**
     * 应用速度倍率的延迟计算
     */
    private int calculateAdjustedDelay(String word) {
        int baseDelay = calculateWordDelay(word);
        double multiplier = getTypingSpeedMultiplier();
        return (int) (baseDelay * multiplier);
    }

    /**
     * 检测内容类型，用于调整整体节奏
     */
    private boolean isCodeContent(String content) {
        return content.contains("```") ||
                content.contains("function") ||
                content.contains("class") ||
                content.matches(".*\\{[\\s\\S]*\\}.*");
    }

    /**
     * 检测是否为列表内容
     */
    private boolean isListContent(String content) {
        return content.matches(".*^\\s*[-*+]\\s+.*") ||
                content.matches(".*^\\s*\\d+\\.\\s+.*");
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