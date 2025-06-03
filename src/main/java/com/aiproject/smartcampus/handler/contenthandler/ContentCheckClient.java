package com.aiproject.smartcampus.handler.contenthandler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @program: SmartCampus
 * @description: 内容检测客户端
 * @author: lk
 * @create: 2025-05-29 19:04
 **/

@Component
@RequiredArgsConstructor
public class ContentCheckClient {

    private final List<ContentCheckHandler>contentCheckHandlers;

    public boolean check(List<String> contentList){
        for (ContentCheckHandler contentCheckHandler : contentCheckHandlers) {
            if(!contentCheckHandler.isok(contentList)){
                return false;
            }
        }
        return true;
    }



}
