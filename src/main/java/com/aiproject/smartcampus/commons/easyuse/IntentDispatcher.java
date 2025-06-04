package com.aiproject.smartcampus.commons.easyuse;

import com.aiproject.smartcampus.commons.HandlerRegiserCilent;
import com.aiproject.smartcampus.model.intent.handler.Handler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @program: SmartCampus
 * @description: 获取服务信息
 * @author: lk
 * @create: 2025-05-28 23:45
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class IntentDispatcher {

    public final HandlerRegiserCilent handlerRegiserCilent;

    public Handler getHandler(String intent) {
        log.debug("正在获取Handler，意图: {}", intent);
        Handler handler = handlerRegiserCilent.getHandler(intent);
        if (handler != null) {
            log.debug("成功获取Handler: {} -> {}", intent, handler.getClass().getSimpleName());
        } else {
            log.warn("未找到匹配的Handler，意图: {}", intent);
            // 输出当前注册的Handler信息，便于调试
            log.warn("当前注册统计: {}", handlerRegiserCilent.getRegistrationStats());
        }
        return handler;
    }

    public String dispatch(Handler handler, String intent, List<CompletableFuture<String>> futureList) {
        try {
            return handler.run(intent, futureList);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

}
