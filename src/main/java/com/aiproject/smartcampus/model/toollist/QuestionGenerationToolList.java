package com.aiproject.smartcampus.model.toollist;

import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @program: ss
 * @description: 问题生成agent工具集
 * @author: lk_hhh
 * @create: 2025-07-05 09:49
 **/

@Component
@Data
public class QuestionGenerationToolList {

    private List<ToolSpecification> tools= new CopyOnWriteArrayList<>();

}