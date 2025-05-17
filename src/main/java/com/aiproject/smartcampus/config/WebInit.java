package com.aiproject.smartcampus.config;

import com.aiproject.smartcampus.interceptor.LoginInterceptor;
import com.aiproject.smartcampus.interceptor.RefreashInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @program: SmartCampus
 * @description: web拦截器相关配置
 * @author: lk
 * @create: 2025-05-17 16:08
 **/

@Configuration
@RequiredArgsConstructor
public class WebInit implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final RefreashInterceptor refreashInterceptor;
    //todo 设置允许公共操作的路径
    private final String[]paths={};
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(paths)
                .order(0);
        registry.addInterceptor(refreashInterceptor)
                .addPathPatterns("/**")
                .order(1);




    }
}

