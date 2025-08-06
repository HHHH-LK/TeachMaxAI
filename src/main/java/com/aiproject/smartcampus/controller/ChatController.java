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
     * SSE流式聊天接口 - 最终工作版本
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

                // 关键修改：先格式化内容，再按词汇分组流式输出
                String formattedAnswer = formatContentForStreaming(fullAnswer);

                log.info("原始内容长度: {}", fullAnswer.length());
                log.info("格式化后长度: {}", formattedAnswer.length());
                log.info("格式化后内容预览: {}", formattedAnswer.substring(0, Math.min(200, formattedAnswer.length())));

                // 按词汇分组流式输出格式化后的内容
                streamAnswerByWords(emitter, formattedAnswer);

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
     * 格式化内容以适配流式输出 - 增强版本
     */
    private String formatContentForStreaming(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "暂时没有找到相关信息。";
        }

        log.debug("开始格式化，原始内容: {}", content.substring(0, Math.min(100, content.length())));

        // 1. 基础清理
        String formatted = content.trim();

        // 2. 在关键位置添加换行 - 这是解决问题的核心
        formatted = addLogicalLineBreaks(formatted);

        // 3. 清理多余空白
        formatted = cleanupWhitespace(formatted);

        // 4. 最终检查 - 确保有足够的换行
        formatted = ensureProperLineBreaks(formatted);

        log.debug("格式化完成，段落数量: {}", formatted.split("\\n\\n").length);

        return formatted;
    }

    /**
     * 在逻辑位置添加换行 - 增强版本
     */
    private String addLogicalLineBreaks(String text) {
        log.debug("添加逻辑换行前: {}", text.substring(0, Math.min(100, text.length())));

        // 1. 在句号后添加换行（如果后面直接跟中文或字母）
        text = text.replaceAll("([。！？])([\\u4e00-\\u9fff\\w])", "$1\n\n$2");

        // 2. 在逻辑连接词前强制换行
        text = text.replaceAll("([。！？，])\\s*(首先|其次|另外|此外|然后|接下来|最后|总之|综上)", "$1\n\n$2");
        text = text.replaceAll("([。！？，])\\s*(但是|然而|不过|另一方面|与此同时)", "$1\n\n$2");
        text = text.replaceAll("([。！？，])\\s*(因此|所以|由此可见|综合来看|总的来说)", "$1\n\n$2");

        // 3. 在"根据"、"关于"等开头词前换行
        text = text.replaceAll("([。！？])\\s*(根据|关于|对于|在|为了)", "$1\n\n$2");

        // 4. 在建议词前换行
        text = text.replaceAll("([。！？])\\s*(建议|推荐|可以|应该|需要)", "$1\n\n$2");

        // 5. 在举例词前换行
        text = text.replaceAll("([。！？，])\\s*(比如|例如|譬如)", "$1\n\n$2");

        // 6. 在数字序号前换行
        text = text.replaceAll("([。！？])\\s*([一二三四五六七八九十1-9][\\.、])", "$1\n\n$2");

        // 7. 处理长句子 - 在逗号+连接词处换行
        text = text.replaceAll("，(同时|此外|另外|特别是|尤其是)", "，\n\n$1");

        // 8. 在问题后换行
        text = text.replaceAll("([？])([\\u4e00-\\u9fff])", "$1\n\n$2");

        // 9. 新增：强制在每个句号后换行（保证基本的句子分隔）
        text = text.replaceAll("([。])([^\\n])", "$1\n\n$2");

        // 10. 强制分割超长句子（超过150字的句子）
        text = breakLongSentences(text);

        log.debug("添加逻辑换行后: {}", text.substring(0, Math.min(200, text.length())));

        return text;
    }

    /**
     * 确保适当的换行
     */
    private String ensureProperLineBreaks(String text) {
        // 如果整个文本中换行太少，强制每80个字符后在句号处换行
        if (text.split("\\n\\n").length < 3 && text.length() > 200) {
            StringBuilder result = new StringBuilder();
            int currentLength = 0;
            String[] sentences = text.split("([。！？])");

            for (int i = 0; i < sentences.length; i++) {
                String sentence = sentences[i];
                if (sentence.trim().isEmpty()) {
                    continue;
                }

                result.append(sentence);
                if (i < sentences.length - 1) {
                    result.append("。");
                    currentLength += sentence.length() + 1;

                    // 如果累计长度超过80字符，添加换行
                    if (currentLength > 80) {
                        result.append("\n");
                        currentLength = 0;
                    }
                }
            }

            return result.toString();
        }

        return text;
    }

    /**
     * 分割超长句子
     */
    private String breakLongSentences(String text) {
        String[] sentences = text.split("\\n\\n");
        StringBuilder result = new StringBuilder();

        for (String sentence : sentences) {
            if (sentence.length() > 150) {
                // 尝试在逗号处分割
                String broken = breakAtCommas(sentence);
                result.append(broken);
            } else {
                result.append(sentence);
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * 在逗号处分割长句子
     */
    private String breakAtCommas(String sentence) {
        String[] parts = sentence.split("，");
        if (parts.length <= 1) {
            return sentence;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder currentPart = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (currentPart.length() + part.length() > 100 && currentPart.length() > 0) {
                // 当前部分已经够长，输出并开始新的部分
                result.append(currentPart.toString()).append("，\n");
                currentPart = new StringBuilder(part);
            } else {
                if (currentPart.length() > 0) {
                    currentPart.append("，");
                }
                currentPart.append(part);
            }
        }

        // 添加最后一部分
        if (currentPart.length() > 0) {
            result.append(currentPart.toString());
        }

        return result.toString();
    }

    /**
     * 清理多余空白
     */
    private String cleanupWhitespace(String text) {
        // 移除多余的空行
        text = text.replaceAll("\\n{3,}", "\n\n");

        // 移除行首行尾空白
        text = text.replaceAll("(?m)^\\s+|\\s+$", "");

        // 规范化空格
        text = text.replaceAll(" +", " ");

        return text.trim();
    }

    /**
     * 按词汇分组的流式输出（加速版本）
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

            // 2. 逐词发送 - 使用加速版本
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);

                if (!word.isEmpty()) {
                    // 发送词汇
                    sendStreamMessage(emitter, "content", word);

                    // 使用加速版本的延迟计算
                    int delay = calculateWordDelayFast(word);

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
     * 计算词汇延迟时间（加速版本）
     */
    private int calculateWordDelayFast(String word) {
        // 基础延迟 - 大幅减少
        int baseDelay = 25; // 从80ms减少到25ms

        // 句子结束标点 - 减少停顿时间
        if (word.matches(".*[。！？.!?]$")) {
            return 120; // 从400ms减少到120ms
        }

        // 逗号、分号等 - 减少停顿
        if (word.matches(".*[，、；,;]$")) {
            return 60; // 从200ms减少到60ms
        }

        // 冒号 - 短停顿
        if (word.matches(".*[：:]$")) {
            return 40; // 从150ms减少到40ms
        }

        // 引号、括号等 - 很短停顿
        if (word.matches(".*[\"\"''\"'()（）\\[\\]【】]$")) {
            return 30; // 从100ms减少到30ms
        }

        // 换行符 - 段落停顿（重要：保持足够停顿以区分段落）
        if ("\n".equals(word)) {
            return 200; // 从300ms减少到200ms，但仍保持明显的段落分隔
        }

        // 空格 - 极短停顿
        if (" ".equals(word) || "\t".equals(word)) {
            return 10; // 从40ms减少到10ms
        }

        // 单个中文字符 - 快速
        if (word.length() == 1 && isChinese(word.charAt(0))) {
            return baseDelay; // 25ms
        }

        // 英文单词 - 根据长度调整，但整体加快
        if (word.matches("[a-zA-Z0-9]+")) {
            if (word.length() <= 3) {
                return 15;  // 从60ms减少到15ms
            } else if (word.length() <= 6) {
                return baseDelay;  // 25ms
            } else {
                return baseDelay + 10;  // 从110ms减少到35ms
            }
        }

        // 数字 - 快速
        if (word.matches("\\d+")) {
            return 20; // 从70ms减少到20ms
        }

        // 其他情况
        return baseDelay; // 25ms
    }

    /**
     * 将文本分割为词汇单元（保持原有逻辑）
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
     * 判断是否为中文字符（保持原有逻辑）
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
     * 判断是否为标点符号（保持原有逻辑）
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
                try {
                    chatSSEController.pushStreamMessageToUser(userId, errorMsg);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
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
                Thread.sleep(20); // 20ms延迟

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
                Thread.sleep(10);

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