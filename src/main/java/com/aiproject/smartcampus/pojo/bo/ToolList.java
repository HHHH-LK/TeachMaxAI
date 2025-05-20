package com.aiproject.smartcampus.pojo.bo;

import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * @program: SmartCampus
 * @description: 工具集合
 * @author: lk
 * @create: 2025-05-17 18:41
 **/

@Data
@Component
public class ToolList {

    private List<ToolSpecification> tools= new  LinkedList();

}
