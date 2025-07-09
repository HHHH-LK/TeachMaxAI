package com.aiproject.smartcampus.model;

/**
 * 流式输出回调接口
 */
public interface StreamingCallback {
    /**
     * 接收到新的文本片段
     * @param text 文本片段
     */
    void onChunk(String text);
    
    /**
     * 流式输出完成
     * @param fullResponse 完整回答
     */
    void onComplete(String fullResponse);
    
    /**
     * 发生错误
     * @param error 异常信息
     */
    void onError(Exception error);
    
    /**
     * 开始输出（可选）
     */
    default void onStart() {
        // 默认空实现
    }
}
