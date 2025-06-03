package com.aiproject.smartcampus.handler.contenthandler;

import com.aiproject.smartcampus.contest.CommonContest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @program: SmartCampus
 * @description: 黄色信息检测
 * @author: lk
 * @create: 2025-05-29 19:02
 **/

@Order(2)
@Component
public class llegalContentCheck implements ContentCheckHandler {


    @Override
    public boolean isok(List<String> contentList) {

        return Boolean.FALSE.equals(contentList.stream().anyMatch(content ->
                Arrays.stream(CommonContest.illegalContentList).anyMatch(content::contains)
        ));

    }
}
