package com.aiproject.smartcampus.model.intent;

import com.aiproject.smartcampus.commons.TaskClient;
import com.aiproject.smartcampus.commons.utils.CreateDiagram;
import com.aiproject.smartcampus.commons.utils.TaskStatusChange;
import com.aiproject.smartcampus.model.intent.router.StepIntentRouter;
import com.aiproject.smartcampus.model.prompts.SystemPrompts;
import com.aiproject.smartcampus.pojo.po.Node;
import com.aiproject.smartcampus.pojo.po.Side;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aiproject.smartcampus.commons.utils.JsonUtils.parseRelationsFromJson;
import static com.aiproject.smartcampus.commons.utils.JsonUtils.parseTasksFromJson;
import static com.aiproject.smartcampus.model.prompts.SystemPrompts.*;

/**
 * @program: SmartCampus
 * @description: 意图处理
 * @author: lk
 * @create: 2025-05-27 23:09
 **/

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentHandler implements Intent {

    private final ChatLanguageModel chatLanguageModel;
    private final StepIntentRouter stepIntentRouter;
    private final TaskStatusChange taskStatusChange;
    private final TaskClient taskClient;
    private final CreateDiagram createDiagram;

    //处理意图
    @Override
    public String handlerIntent(String intent) {
        String result = null;
        try {
            //调用大模型处理意图
            log.info("开始分析用户:{}意图中", intent);
            ChatResponse chatResponse = chatLanguageModel.chat(SystemMessage.from(INITENT_ANALYSIS_PROMPT), UserMessage.from(intent));
            log.info("用户意图:{}分析结果为：", chatResponse.aiMessage().text());
            //进行拆分
            List<String> intentSpilder = intentSpilder(chatResponse.aiMessage().text().trim());
            if (intentSpilder.isEmpty()) {
                log.warn("意图拆分结果为空，使用原始意图");
                intentSpilder.add(intent);
                //todo 设置入度为0
                createDiagram.addInDegree(intent,new AtomicInteger(0));
            }
            //进行路由并获取总结
            result = stepIntentRouter.route(intentSpilder);
            log.info("用户:[{}]意图处理完成，处理结果为[{}]：", intent,result);

        } catch (Exception e) {
            log.error("用户:{}意图分析失败", intent);
            return "处理请求失败，请稍后重试";
        }
        return result;
    }

    //意图拆分
    @Override
    public List<String> intentSpilder(String intent) {

        List<String> splitResult=new ArrayList<>();
        try{
            log.info("用户:{}意图拆分中");
            ChatResponse intentchatResponse = chatLanguageModel.chat(SystemMessage.from(INITENT_SPIDLER_PROMPT), UserMessage.from(intent));
            String s = intentchatResponse.aiMessage().text().trim();
            log.info("用户:{}意图拆分结果为：",s);
            //todo 解析出list
            splitResult = parseTasksFromJson(s);
            if(splitResult.isEmpty()){
                throw new RuntimeException("意图拆解失败");
            }
            //todo 解析拆分结果并存入 加入图中并建实现图的创建
            for (String initent : splitResult) {
                taskClient.addTask(new Node(initent));
            }
            log.info("意图拆分完成，共生成 {} 个子任务", splitResult.size());
            // 处理依赖关系
            List<Side> intentRelations = analyzeTaskRelations(intent, splitResult);
            if (!intentRelations.isEmpty()) {
                taskClient.addTaskRelation(intentRelations);
            }
            else{
                //设置入度为0
                for(String intents: splitResult){
                    createDiagram.addInDegree(intent,new AtomicInteger(0));
                }
            }
            //初始化(建图)
            createDiagram.init();
            //初始化（状态）
            taskStatusChange.init();

        }catch (Exception e){
            log.error("用户:{}意图拆分失败",e.getMessage());
            //若失败则将其处理成原意图
            splitResult.add(intent);
        }

        return splitResult;

    }

    private List<Side> analyzeTaskRelations(String originalIntent, List<String> tasks) {
        List<Side> relations = new ArrayList<>();
        try {
            // 构建包含任务列表的上下文
            String contextInput = String.format("原始意图: %s\n子任务列表: %s",
                    originalIntent, String.join(", ", tasks));
            ChatResponse relationResponse = chatLanguageModel.chat(
                    SystemMessage.from(INITENT_RELATION_PROMPT),
                    UserMessage.from(contextInput)
            );
            String relationResult = relationResponse.aiMessage().text().trim();
            log.info("任务关系分析结果: {}", relationResult);
            // 解析关系JSON
            relations = parseRelationsFromJson(relationResult);

        } catch (Exception e) {
            log.error("任务关系分析失败", e);
        }

        return relations;
    }

}
