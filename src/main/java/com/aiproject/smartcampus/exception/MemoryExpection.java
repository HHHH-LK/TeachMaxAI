package com.aiproject.smartcampus.exception;

/**
 * @program: lecture-langchain-20250525
 * @description: 会话异常处理
 * @author: lk
 * @create: 2025-05-11 10:53
 **/

public class MemoryExpection extends RuntimeException {

    public MemoryExpection(String message) {
        super(message);
    }
    public MemoryExpection() {}

}
