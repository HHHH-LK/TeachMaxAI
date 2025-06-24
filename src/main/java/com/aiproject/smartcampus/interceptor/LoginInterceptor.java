package com.aiproject.smartcampus.interceptor;

import com.github.xiaoymin.knife4j.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * @program: SmartCampus
 * @description: 登入拦截器
 * @author: lk
 * @create: 2025-05-17 17:07
 **/

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        //获取token
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }
        //检测是不是新人以及是否时间过期了
        String user = stringRedisTemplate.opsForValue().get("token" + token);
        if (StrUtil.isBlank(user)) {
            response.setStatus(401);
            return false;
        }
        return true;

    }

}
