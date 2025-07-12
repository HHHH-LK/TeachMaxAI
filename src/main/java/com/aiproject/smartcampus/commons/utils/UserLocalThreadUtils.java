package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.pojo.po.User;
import dev.langchain4j.data.message.ChatMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: SmartCampus
 * @description: 线程集合工具类·1 - 增强版支持记忆功能
 * @author: lk
 * @create: 2025-05-17 17:33
 **/

@Data
@Slf4j
public class UserLocalThreadUtils {

    // =============================================================================
    // 现有的ThreadLocal变量保持不变
    // =============================================================================

    private static ThreadLocal<User> threadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> userMemoryLocal = new ThreadLocal<>();
    private static ThreadLocal<String> userLoginToken = new ThreadLocal<>();

    // =============================================================================
    // 新增的记忆相关ThreadLocal变量
    // =============================================================================

    private static ThreadLocal<List<ChatMessage>> chatMemoryLocal = new ThreadLocal<>();
    private static ThreadLocal<String> userIdLocal = new ThreadLocal<>();

    // =============================================================================
    // 现有方法保持不变
    // =============================================================================

    public static String getToken(){
        return userLoginToken.get();
    }

    public static void setToken(String token){
        userLoginToken.set(token);
    }

    public static User getUserInfo() {
        return threadLocal.get();
    }

    public static void setUserInfo(User user) {
        threadLocal.set(user);
    }

    public static void removeUserInfo() {
        threadLocal.remove();
    }

    public static String getUserMemory() {
        return userMemoryLocal.get();
    }

    public static void setUserMemory(String userMemory) {
        userMemoryLocal.set(userMemory);
    }

    public static void removeUserMemory() {
        userMemoryLocal.remove();
    }

    public static String test() {
        return "1";
    }

    // =============================================================================
    // 新增的记忆功能方法
    // =============================================================================

    /**
     * 设置聊天记忆消息列表
     * @param chatMemory 聊天记忆消息列表
     */
    public static void setChatMemory(List<ChatMessage> chatMemory) {
        if (chatMemory != null) {
            chatMemoryLocal.set(new ArrayList<>(chatMemory));
            log.debug("设置聊天记忆: {}条消息", chatMemory.size());
        } else {
            chatMemoryLocal.set(new ArrayList<>());
            log.debug("设置聊天记忆: 空列表");
        }
    }

    /**
     * 获取聊天记忆消息列表
     * @return 聊天记忆消息列表，如果没有则返回空列表
     */
    public static List<ChatMessage> getChatMemory() {
        List<ChatMessage> memory = chatMemoryLocal.get();
        if (memory == null) {
            log.debug("获取聊天记忆: 未找到，返回空列表");
            return new ArrayList<>();
        }
        log.debug("获取聊天记忆: {}条消息", memory.size());
        return new ArrayList<>(memory);
    }

    /**
     * 添加单条聊天记忆消息
     * @param chatMessage 聊天消息
     */
    public static void addChatMemory(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return;
        }

        List<ChatMessage> memory = chatMemoryLocal.get();
        if (memory == null) {
            memory = new ArrayList<>();
        }

        memory.add(chatMessage);
        chatMemoryLocal.set(memory);
        log.debug("添加聊天记忆: 当前总数{}条", memory.size());
    }

    /**
     * 清除聊天记忆
     */
    public static void removeChatMemory() {
        chatMemoryLocal.remove();
        log.debug("清除聊天记忆");
    }

    /**
     * 检查是否有聊天记忆
     * @return true如果有记忆，false如果没有
     */
    public static boolean hasChatMemory() {
        List<ChatMessage> memory = chatMemoryLocal.get();
        boolean hasMemory = memory != null && !memory.isEmpty();
        log.debug("检查聊天记忆: {}", hasMemory ? "有" : "无");
        return hasMemory;
    }

    /**
     * 获取聊天记忆数量
     * @return 记忆消息数量
     */
    public static int getChatMemorySize() {
        List<ChatMessage> memory = chatMemoryLocal.get();
        int size = memory != null ? memory.size() : 0;
        log.debug("获取聊天记忆数量: {}", size);
        return size;
    }


    /**
     * 设置当前用户ID
     * @param userId 用户ID
     */
    public static void setCurrentUserId(String userId) {
        userIdLocal.set(userId);
        log.debug("设置当前用户ID: {}", userId);
    }

    /**
     * 获取当前用户ID
     * @return 用户ID，优先从userIdLocal获取，如果没有则尝试从User对象获取
     */
    public static String getCurrentUserId() {
        // 优先从专门的用户ID ThreadLocal获取
        String userId = userIdLocal.get();
        if (userId != null && !userId.trim().isEmpty()) {
            log.debug("从userIdLocal获取用户ID: {}", userId);
            return userId;
        }

        // 尝试从User对象获取
        User user = getUserInfo();
        if (user != null && user.getUserId() != null) {
            userId = user.getUserId().toString();
            log.debug("从User对象获取用户ID: {}", userId);
            return userId;
        }

        // 返回默认值
        String defaultUserId = "default_user_1";
        log.debug("使用默认用户ID: {}", defaultUserId);
        return defaultUserId;
    }

    /**
     * 清除当前用户ID
     */
    public static void removeCurrentUserId() {
        userIdLocal.remove();
        log.debug("清除当前用户ID");
    }


    /**
     * 同时设置用户ID和聊天记忆
     * @param userId 用户ID
     * @param chatMemory 聊天记忆
     */
    public static void setUserContext(String userId, List<ChatMessage> chatMemory) {
        setCurrentUserId(userId);
        setChatMemory(chatMemory);
        log.debug("设置用户上下文 - 用户ID: {}, 记忆: {}条", userId,
                chatMemory != null ? chatMemory.size() : 0);
    }

    /**
     * 清除所有线程上下文信息
     */
    public static void clearAll() {
        removeUserInfo();
        removeUserMemory();
        removeChatMemory();
        removeCurrentUserId();
        log.debug("清除所有线程上下文信息");
    }

    /**
     * 清除记忆相关的线程上下文
     */
    public static void clearMemoryContext() {
        removeChatMemory();
        removeCurrentUserId();
        log.debug("清除记忆相关的线程上下文");
    }


    /**
     * 获取当前线程上下文状态信息
     * @return 状态信息字符串
     */
    public static String getContextStatus() {
        User user = getUserInfo();
        String userMemory = getUserMemory();
        List<ChatMessage> chatMemory = getChatMemory();
        String userId = getCurrentUserId();

        StringBuilder status = new StringBuilder();
        status.append("=== 线程上下文状态 ===\n");
        status.append("User对象: ").append(user != null ? "已设置" : "未设置").append("\n");
        status.append("用户记忆(String): ").append(userMemory != null ? "已设置" : "未设置").append("\n");
        status.append("聊天记忆: ").append(chatMemory.size()).append("条\n");
        status.append("当前用户ID: ").append(userId).append("\n");
        status.append("===================");

        return status.toString();
    }

    /**
     * 打印当前线程上下文状态到日志
     */
    public static void logContextStatus() {
        log.info(getContextStatus());
    }

    /**
     * 验证线程上下文是否正确设置
     * @return true如果上下文设置正确，false否则
     */
    public static boolean isContextValid() {
        String userId = getCurrentUserId();
        boolean hasValidUserId = userId != null && !userId.trim().isEmpty();

        log.debug("线程上下文验证 - 用户ID有效: {}", hasValidUserId);
        return hasValidUserId;
    }


    /**
     * 兼容性方法：设置用户记忆（重载版本支持ChatMessage列表）
     * 这个方法是为了支持之前代码中可能的 setUserMemory(List<ChatMessage>) 调用
     */
    public static void setUserMemory(List<ChatMessage> chatMemory) {
        setChatMemory(chatMemory);
        log.debug("通过兼容性方法设置聊天记忆: {}条",
                chatMemory != null ? chatMemory.size() : 0);
    }

    /**
     * 兼容性方法：获取用户记忆（ChatMessage列表版本）
     * 这个方法是为了支持之前代码中可能的 getUserMemory() -> List<ChatMessage> 调用
     */
    public static List<ChatMessage> getUserMemoryAsList() {
        return getChatMemory();
    }

    /**
     * 兼容性方法：设置用户ID（如果之前有这样的调用）
     */
    public static void setUserId(String userId) {
        setCurrentUserId(userId);
    }

    /**
     * 兼容性方法：获取用户ID（如果之前有这样的调用）
     */
    public static String getUserId() {
        return getCurrentUserId();
    }

    /**
     * 兼容性方法：清理方法的别名
     */
    public static void clear() {
        clearAll();
    }
}