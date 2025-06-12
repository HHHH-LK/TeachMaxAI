package com.aiproject.smartcampus.commons.exceptionhandler;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.exception.MemoryExpection;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @program: SmartCampus
 * @description: 全局异常处理器
 * @author: lk
 * @create: 2025-05-17 17:00
 **/

public class GobalExpectionHandler {

    @ExceptionHandler
    public Result handlerException(MemoryExpection e) {
        return Result.error(e.getMessage());
    }

}
