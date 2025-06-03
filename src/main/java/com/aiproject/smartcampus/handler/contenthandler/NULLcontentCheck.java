package com.aiproject.smartcampus.handler.contenthandler;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 空内容检测
 * @author: lk
 * @create: 2025-05-29 19:03
 **/

@Order(1)
@Component
public class NULLcontentCheck implements ContentCheckHandler{
    @Override
    public boolean isok(List<String> contentList) {

        for(String content:contentList){
            if(content==null||content.isEmpty()){
                return false;
            }
        }
        return true;
    }
}
