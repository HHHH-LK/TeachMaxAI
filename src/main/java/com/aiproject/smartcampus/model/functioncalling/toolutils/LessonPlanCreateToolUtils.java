package com.aiproject.smartcampus.model.functioncalling.toolutils;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @program: SmartCampus
 * @description: 教案生成工具
 * @author: lk_hhh
 * @create: 2025-07-11 16:28
 **/

@Component
@Slf4j
@RequiredArgsConstructor
public class LessonPlanCreateToolUtils {

    private final ChatLanguageModel chatLanguageModel;

    /**
     * 生成高质量教案
     *
     * @param content 用户教学需求内容
     * @param classAndChapterInfo 课程章节知识点信息
     * @param wrongKnowledgeInfo 班级高频错误知识点分布
     * @param teacherInfo 教师信息
     * @return 以Markdown格式返回的教案内容
     */
    public String createLessonPlan(String content, String classAndChapterInfo, String wrongKnowledgeInfo, String teacherInfo) {
        log.info("开始生成增强版教案");
        log.info("用户需求：{}", content);
        log.info("章节知识点信息长度：{}", classAndChapterInfo != null ? classAndChapterInfo.length() : 0);
        log.info("错误知识点信息长度：{}", wrongKnowledgeInfo != null ? wrongKnowledgeInfo.length() : 0);
        log.info("教师信息：{}", teacherInfo);

        try {
            // 构建详细的提示词
            String prompt = buildEnhancedLessonPlanPrompt(content, classAndChapterInfo, wrongKnowledgeInfo, teacherInfo);

            // 调用语言模型生成教案
            String lessonPlan = chatLanguageModel.chat(prompt);

            log.info("教案生成成功，内容长度：{}", lessonPlan.length());
            return lessonPlan;

        } catch (Exception e) {
            log.error("教案生成失败", e);
            return generateErrorResponse(e.getMessage());
        }
    }

    /**
     * 生成高质量教案（原版保留）
     *
     * @param userContent 用户需求内容
     * @param classInfoBasic 班级情况分析
     * @return 以Markdown格式返回的教案内容
     */
    public String createLessonPlan(String userContent, String classInfoBasic) {
        log.info("开始生成教案，用户需求：{}，班级情况：{}", userContent, classInfoBasic);

        try {
            // 构建详细的提示词
            String prompt = buildLessonPlanPrompt(userContent, classInfoBasic);

            // 调用语言模型生成教案
            String lessonPlan = chatLanguageModel.chat(prompt);

            log.info("教案生成成功，内容长度：{}", lessonPlan.length());
            return lessonPlan;

        } catch (Exception e) {
            log.error("教案生成失败", e);
            return generateErrorResponse(e.getMessage());
        }
    }

    /**
     * 构建增强版教案生成的提示词
     */
    private String buildEnhancedLessonPlanPrompt(String content, String classAndChapterInfo,
                                                 String wrongKnowledgeInfo, String teacherInfo) {
        return String.format("""
            # 智能教案生成任务
            
            作为一名专业的教育AI助手，请根据以下详细信息生成一份高质量、个性化的教案。
            
            ## 教学需求
            %s
            
            ## 课程章节知识点分析
            %s
            
            ## 班级学习问题分析
            %s
            
            ## 授课教师信息
            %s
            
            ---
            
            # 教案生成要求
            
            请严格按照以下结构生成完整的Markdown格式教案：
            
            ## 📚 教案基本信息
            
            | 项目 | 内容 |
            |------|------|
            | **课程名称** | [根据章节信息提取] |
            | **章节内容** | [根据章节信息提取] |
            | **授课教师** | [根据教师信息提取] |
            | **所属部门** | [根据教师信息提取] |
            | **课程时长** | 45分钟 |
            | **授课方式** | 线下面授 + 互动讨论 |
            | **适用对象** | [根据课程层次确定] |
            
            ## 🎯 教学目标
            
            ### 知识目标
            - **核心知识点掌握**：[基于章节知识点列表，列出3-5个核心知识点]
            - **概念理解**：[针对重点知识点的理解要求]
            - **知识体系构建**：[知识点间的逻辑关系]
            
            ### 能力目标
            - **分析能力**：[结合学科特点设定]
            - **应用能力**：[理论联系实际的能力]
            - **问题解决能力**：[针对常见错误的纠正能力]
            
            ### 情感态度目标
            - **学习兴趣**：激发学生对本章节内容的学习兴趣
            - **学习信心**：通过针对性教学增强学生学习信心
            - **合作精神**：培养学生协作学习的意识
            
            ## ⚡ 教学重点与难点
            
            ### 🔥 教学重点
            [根据章节核心知识点确定，重点突出is_core=1的知识点]
            
            ### 💪 教学难点
            [根据班级高频错误知识点分析，重点关注错误率最高的知识点]
            
            ### 🎯 难点突破策略
            [针对高频错误知识点，设计具体的教学策略和方法]
            
            ## 📖 教学方法与策略
            
            ### 主要教学方法
            - **问题导向法**：针对班级高频错误设计问题链
            - **案例教学法**：结合实际应用场景
            - **分层教学法**：根据学生掌握情况分层指导
            - **互动讨论法**：促进学生主动思考
            
            ### 教学媒体工具
            - 多媒体课件（PPT）
            - 板书设计
            - 在线练习平台
            - 小组讨论工具
            
            ## 📋 教学过程设计
            
            ### 🚀 第一环节：课堂导入（5分钟）
            
            **导入方式**：[设计吸引人的导入，可以是问题、案例或复习]
            
            **具体内容**：
            1. [导入活动1]
            2. [导入活动2]
            3. 引出本节课主题
            
            **预期效果**：激发学习兴趣，为新课学习做好准备
            
            ### 📚 第二环节：新课讲授（25分钟）
            
            #### 知识点一：[第一个重点知识点]（8分钟）
            - **讲解方式**：[具体教学方法]
            - **重点强调**：[需要特别注意的内容]
            - **常见错误预防**：[根据错误知识点分析，预防性讲解]
            - **学生活动**：[设计相应的学生参与活动]
            
            #### 知识点二：[第二个重点知识点]（8分钟）
            - **讲解方式**：[具体教学方法]
            - **难点突破**：[针对高频错误的教学策略]
            - **互动环节**：[师生互动或生生互动]
            
            #### 知识点三：[第三个重点知识点]（9分钟）
            - **综合应用**：[知识点的综合运用]
            - **错误纠正**：[针对班级常见错误进行重点纠正]
            - **拓展延伸**：[适当的知识拓展]
            
            ### 🏃 第三环节：练习巩固（10分钟）
            
            #### 基础练习（5分钟）
            - **练习内容**：[针对核心知识点的基础练习]
            - **重点关注**：[班级高频错误知识点的针对性练习]
            
            #### 提升练习（5分钟）
            - **综合应用题**：[知识点综合运用]
            - **分层指导**：[为不同层次学生提供指导]
            
            ### 📝 第四环节：总结升华（5分钟）
            
            1. **知识梳理**：[本节课知识点总结]
            2. **方法归纳**：[解题方法和思路总结]
            3. **错误警示**：[强调易错点，预防错误]
            4. **课后任务**：[布置针对性作业]
            
            ## 📊 板书设计
            
            ```
            [设计清晰的板书布局，突出：]
            1. 课题标题
            2. 核心知识点结构图
            3. 重要公式或概念
            4. 易错点提醒
            5. 解题步骤或方法
            ```
            
            ## 📋 作业与评价
            
            ### 课后作业设计
            
            #### 基础巩固题（必做）
            [针对核心知识点设计3-5道基础题]
            
            #### 能力提升题（选做）
            [针对知识点应用设计2-3道提升题]
            
            #### 错误纠正题（重点）
            [针对班级高频错误知识点设计专项练习]
            
            ### 评价方式
            - **过程评价**：课堂参与度、问题回答质量
            - **结果评价**：作业完成情况、知识掌握程度
            - **自我评价**：学生自主反思学习效果
            
            ## 🔍 教学反思与改进
            
            ### 预期效果分析
            - **知识掌握预期**：[基于章节知识点的掌握预期]
            - **能力提升预期**：[学生能力发展预期]
            - **问题解决预期**：[针对高频错误的改善预期]
            
            ### 潜在问题预测
            [根据班级错误知识点分析，预测可能遇到的教学问题]
            
            ### 应对策略
            [针对预测问题的具体应对方案]
            
            ### 课后改进方向
            [基于本次教学效果的改进建议]
            
            ## 🎯 个性化教学策略
            
            ### 针对高频错误的专项策略
            [根据错误知识点分析，制定专门的纠错策略]
            
            ### 差异化教学安排
            - **学困生支持**：[针对掌握困难的学生的支持措施]
            - **中等生提升**：[帮助中等生突破的具体方法]
            - **优等生拓展**：[为优秀学生提供的拓展内容]
            
            ### 课后跟进计划
            [针对本次课程的后续跟进和辅导计划]
            
            ---
            
            ## 🎯 特别要求
            
            1. **数据驱动**：充分利用章节知识点数据和错误分析数据
            2. **针对性强**：重点关注班级高频错误知识点的教学
            3. **实用性高**：每个教学环节都要具体可操作
            4. **个性化**：体现教师特色和班级特点
            5. **科学性**：时间安排合理，教学逻辑清晰
            6. **创新性**：在传统教学基础上融入现代教学理念
            7. **效果性**：确保教学目标的达成和问题的解决
            
            请严格按照以上结构生成完整详细的教案内容，确保每个部分都有具体内容，不得出现空白或简略的部分。
            """, content, classAndChapterInfo, wrongKnowledgeInfo, teacherInfo);
    }

    /**
     * 构建教案生成的提示词（原版）
     */
    private String buildLessonPlanPrompt(String userContent, String classInfoBasic) {
        return String.format("""
            # 教案生成任务
            
            请根据以下信息生成一份高质量的教案，要求以Markdown格式输出：
            
            ## 用户需求
            %s
            
            ## 班级情况分析
            %s
            
            ## 教案要求
            请生成一份完整的教案，包含以下结构：
            
            ### 1. 教案基本信息
            - 课程名称
            - 授课时间
            - 授课对象
            - 课时安排
            - 授课教师
            
            ### 2. 教学目标
            - 知识目标（学生应掌握的知识点）
            - 能力目标（学生应培养的能力）
            - 情感态度价值观目标
            
            ### 3. 教学重点与难点
            - 教学重点
            - 教学难点
            - 解决策略
            
            ### 4. 教学方法与手段
            - 教学方法选择及理由
            - 教学媒体和工具
            - 互动方式设计
            
            ### 5. 教学过程设计
            #### 5.1 导入环节（5-8分钟）
            - 导入方式
            - 导入内容
            - 预期效果
            
            #### 5.2 新课讲授（25-30分钟）
            - 知识点讲解顺序
            - 每个知识点的教学活动
            - 学生参与方式
            - 板书设计
            
            #### 5.3 练习巩固（8-10分钟）
            - 练习题设计
            - 学生活动安排
            - 教师指导要点
            
            #### 5.4 总结提升（5分钟）
            - 知识梳理
            - 能力提升
            - 布置作业
            
            ### 6. 板书设计
            - 板书布局
            - 关键内容展示
            - 逻辑结构体现
            
            ### 7. 作业设计
            - 基础练习题
            - 拓展思考题
            - 实践应用题
            
            ### 8. 教学反思
            - 预期效果分析
            - 可能遇到的问题
            - 改进措施
            
            ### 9. 差异化教学策略
            根据班级情况，针对不同学习水平的学生提供：
            - 学困生辅导策略
            - 优等生拓展内容
            - 中等生提升方案
            
            ## 特别要求
            1. **针对性强**：根据提供的班级情况，调整教学策略和内容难度
            2. **实用性高**：教学活动要具体可操作，时间安排要合理
            3. **互动性好**：设计多种师生互动和生生互动环节
            4. **层次分明**：照顾不同学习水平的学生需求
            5. **格式规范**：严格按照Markdown格式输出，层次清晰
            6. **内容完整**：每个部分都要有具体内容，不能只是标题
            7. **语言专业**：使用教育教学专业术语，表达准确
            
            请生成完整的教案内容：
            """, userContent, classInfoBasic);
    }

    /**
     * 生成错误响应
     */
    private String generateErrorResponse(String errorMessage) {
        return String.format("""
            # 教案生成失败
            
            ## 错误信息
            %s
            
            ## 建议
            1. 请检查输入的用户需求是否清晰完整
            2. 请确认班级情况分析信息是否准确
            3. 如问题持续，请联系技术支持
            
            ## 示例格式
            您可以参考以下格式重新提供信息：
            
            ### 用户需求示例
            - 课程名称：高等数学
            - 章节内容：函数的极限
            - 课时安排：45分钟
            - 特殊要求：注重概念理解和应用
            
            ### 班级情况示例
            - 班级规模：30人
            - 学习基础：数学基础较好，80%%学生已掌握基础知识
            - 学习特点：活跃度高，喜欢互动讨论
            - 注意事项：有3名学困生需要特别关注
            """, errorMessage);
    }

    /**
     * 验证输入参数
     */
    private boolean validateInputs(String userContent, String classInfoBasic) {
        if (userContent == null || userContent.trim().isEmpty()) {
            log.warn("用户需求内容为空");
            return false;
        }

        if (classInfoBasic == null || classInfoBasic.trim().isEmpty()) {
            log.warn("班级情况分析为空");
            return false;
        }

        return true;
    }

    /**
     * 带参数验证的教案生成方法
     */
    public String createLessonPlanWithValidation(String userContent, String classInfoBasic) {
        // 参数验证
        if (!validateInputs(userContent, classInfoBasic)) {
            return generateErrorResponse("输入参数不完整，请提供完整的用户需求和班级情况分析");
        }

        return createLessonPlan(userContent, classInfoBasic);
    }
}