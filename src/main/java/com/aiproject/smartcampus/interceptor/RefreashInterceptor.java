package com.aiproject.smartcampus.interceptor;

import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

import static com.aiproject.smartcampus.contest.CommonContest.REFRESH_TIEM;

/**
 * @program: SmartCampus
 * @description: 刷新登录有效期
 * @author: lk
 * @create: 2025-05-17 17:15
 **/

@Component
public class RefreashInterceptor implements HandlerInterceptor {

    @Autowired
    private  StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("token");

        if(!StrUtil.isBlank(token)){
            //避免对于未登录的用户访问公共资源带来的null问题
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(token))) {
                stringRedisTemplate.expire(token, REFRESH_TIEM, TimeUnit.DAYS);
            }
        }
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        UserLocalThreadUtils.removeUserInfo();

    }
}
