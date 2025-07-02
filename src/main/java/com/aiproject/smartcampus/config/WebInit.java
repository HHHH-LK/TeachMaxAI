package com.aiproject.smartcampus.config;

import com.aiproject.smartcampus.commons.utils.SenderTypeHandler;
import com.aiproject.smartcampus.interceptor.LoginInterceptor;
import com.aiproject.smartcampus.interceptor.RefreashInterceptor;
import com.aiproject.smartcampus.pojo.enums.SenderType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
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
    private final String[] excludePathPatternList = {
            "/doc.html",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/webjars/**",
            "/common/preliminaryregister",
            "/common/register",
            "/common/login",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //todo 取消拦截器做测试
      /*  registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludePathPatternList)
                .order(0);
        registry.addInterceptor(refreashInterceptor)
                .addPathPatterns("/**")
                .order(1);
*/
    }




}

