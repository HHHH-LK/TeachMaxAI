package com.aiproject.smartcampus.model.toollist;

import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @program: SmartCampus
 * @description: 工具集合
 * @author: lk
 * @create: 2025-05-17 18:41
 **/

@Data
@Component
public class ChatAgentToolList {

    private List<ToolSpecification> tools= new CopyOnWriteArrayList<>();

}
