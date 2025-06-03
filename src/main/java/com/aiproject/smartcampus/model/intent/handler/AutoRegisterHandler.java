package com.aiproject.smartcampus.model.intent.handler;

import com.aiproject.smartcampus.commons.HandlerRegiserCilent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: SmartCampus
 * @description: 自动注册处理（处理机实现）
 * @author: lk
 * @create: 2025-05-28 00:51
 **/

@Order(5)
@Slf4j
@Component
public abstract class AutoRegisterHandler implements Handler {

    @Autowired
    private HandlerRegiserCilent handlerRegiserCilent;

    @PostConstruct
    public void register() {
        try {
            //基于反射获取功能描述
            Field field = this.getClass().getDeclaredField("functionDescription");
            field.setAccessible(true);
            String functionDescription = (String) field.get(this);
            handlerRegiserCilent.add(functionDescription, this);
            getIntentTaskInfo(functionDescription);
            //设置处理器处理任务描述
            List<String> intentTaskInfo = getIntentTaskInfo(functionDescription);
            for (String intent : intentTaskInfo) {
                handlerRegiserCilent.add(intent, this);
            }
        } catch (Exception e) {
            log.error("{}自动注册失败，请检查是否添加了功能描述", this.getClass().getName());
        }
    }

    //todo 待添加新功能
    private List<String> getIntentTaskInfo(String description) {
        List<String> keywords = new ArrayList<>();
        if (description.contains("增强检索") || description.contains("知识库")) {
            keywords.addAll(Arrays.asList("检索", "搜索", "查找", "知识库", "RAG"));
        }
        if (description.contains("工具") || description.contains("功能操作")) {
            keywords.addAll(Arrays.asList("工具调用", "执行", "调用", "计算", "转换"));
        }
        if (description.contains("大语言模型") || description.contains("对话")) {
            keywords.addAll(Arrays.asList("对话", "问答", "聊天", "回答", "生成"));
        }
        if (description.contains("记忆") || description.contains("上下文")) {
            keywords.addAll(Arrays.asList("记忆", "上下文", "历史", "之前", "继续"));
        }
        return keywords;
    }

}
