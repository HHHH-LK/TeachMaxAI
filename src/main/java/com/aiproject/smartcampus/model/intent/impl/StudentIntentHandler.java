package com.aiproject.smartcampus.model.intent.impl;

import com.aiproject.smartcampus.commons.utils.TaskInitUtils;
import com.aiproject.smartcampus.model.intent.Intent;
import com.aiproject.smartcampus.model.router.StepIntentRouter;
import com.aiproject.smartcampus.pojo.bo.Side;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
public class StudentIntentHandler implements Intent {

    private final ChatLanguageModel chatLanguageModel;
    private final StepIntentRouter stepIntentRouter;
    private final TaskInitUtils taskInitUtils;

    /**
     * 处理用户意图
     *
     * @param intent 用户意图
     * @return 处理结果
     */
    @Override
    public String handlerIntent(String intent) {
        String result = null;
        try {
            log.info("开始分析用户意图: {}", intent);

            // 调用大模型分析意图
            ChatResponse chatResponse = chatLanguageModel.chat(
                    SystemMessage.from(INITENT_ANALYSIS_PROMPT),
                    UserMessage.from(intent)
            );

           String analysisResult = chatResponse.aiMessage().text().trim();
            log.info("用户意图分析结果: {}", analysisResult);

            // 进行意图拆分
            List<String> intentSpider = intentSpilder(analysisResult);
            if (intentSpider.isEmpty()) {
                log.warn("意图拆分结果为空，使用原始意图");
                intentSpider.add(intent);
                // 初始化单个任务
                taskInitUtils.initSingleTask(intent);
            }

            // 进行路由并获取总结
            result = stepIntentRouter.route(intentSpider);
            log.info("用户意图 [{}] 处理完成，处理结果: [{}]", intent, result);

        } catch (Exception e) {
            log.error("用户意图 [{}] 分析失败: {}", intent, e.getMessage(), e);
            return "处理请求失败，请稍后重试";
        }
        return result;
    }

    /**
     * 意图拆分
     *
     * @param intent 分析后的意图
     * @return 拆分后的意图列表
     */
    @Override
    public List<String> intentSpilder(String intent) {
        List<String> splitResult = new ArrayList<>();

        try {
            log.info("开始拆分用户意图: {}", intent);

            // 调用大模型进行意图拆分
            ChatResponse intentChatResponse = chatLanguageModel.chat(
                    SystemMessage.from(INITENT_SPIDLER_PROMPT),
                    UserMessage.from(intent)
            );

            String spiderResult = intentChatResponse.aiMessage().text().trim();
            log.info("意图拆分结果: {}", spiderResult);

            // 解析JSON格式的拆分结果
            splitResult = parseTasksFromJson(spiderResult);
            if (splitResult.isEmpty()) {
                throw new RuntimeException("意图拆解失败: 解析结果为空");
            }

            log.info("意图拆分完成，共生成 {} 个子任务", splitResult.size());

            // 分析任务依赖关系
            List<Side> intentRelations = analyzeTaskRelations(intent, splitResult);

            // 使用TaskInitUtils进行初始化
            taskInitUtils.init(splitResult, intentRelations);

        } catch (Exception e) {
            log.error("意图拆分失败: {}", e.getMessage(), e);
            // 拆分失败时，将原意图作为单个任务处理
            splitResult.clear();
            splitResult.add(intent);
        }

        return splitResult;
    }

    /**
     * 分析任务依赖关系
     *
     * @param originalIntent 原始意图
     * @param tasks          拆分后的任务列表
     * @return 任务依赖关系列表
     */
    private List<Side> analyzeTaskRelations(String originalIntent, List<String> tasks) {
        List<Side> relations = new ArrayList<>();

        try {
            log.info("开始分析任务依赖关系");

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
            log.info("成功解析 {} 个任务依赖关系", relations.size());

        } catch (Exception e) {
            log.error("任务关系分析失败: {}", e.getMessage(), e);
            // 关系分析失败时返回空列表，表示无依赖关系
        }

        return relations;
    }
}