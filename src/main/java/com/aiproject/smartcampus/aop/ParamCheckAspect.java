package com.aiproject.smartcampus.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * @program: SmartCampus
 * @description: 参数娇艳前置判断
 * @author: lk_hhh
 * @create: 2025-07-01 20:36
 **/

@Slf4j
@Component
@Aspect
public class ParamCheckAspect {

    @Before("execution(* com.aiproject.smartcampus.service..*(..)) && " +
            "!execution(* com.aiproject.smartcampus.service.impl.*.scheduledCheckAndUpdateQuestions(..))")
    public void before(JoinPoint joinPoint) {

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg == null) {
                log.error("传入参数为null");
                throw new RuntimeException("参数为null");
            }
            if (arg instanceof String str && str.trim().isEmpty()) {
                log.warn("参数为字符串，但为空白，方法：{}", joinPoint.getSignature().toShortString());
                throw new IllegalArgumentException("参数不能是空字符串！");
            }
        }
        log.info("方法 {} 参数校验通过", joinPoint.getSignature().getName());

    }


}