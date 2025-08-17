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
     * @param content             用户教学需求内容
     * @param classAndChapterInfo 课程章节知识点信息
     * @param wrongKnowledgeInfo  班级高频错误知识点分布
     * @param teacherInfo         教师信息
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
     * @param userContent    用户需求内容
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
     * 构建增强版教案生成的提示词（目录不变，要求返回完整 Markdown）
     */
    private String buildEnhancedLessonPlanPrompt(String content, String classAndChapterInfo,
                                                 String wrongKnowledgeInfo, String teacherInfo) {
        return String.format("""
                你是一名专业的教育AI教学设计师，请基于提供的数据生成一份“完整且可直接使用”的教案。
                
                =========================
                输出总规范（必须严格遵守）
                - 只输出“完整的 Markdown 文档”，不要输出解释、提示、JSON、YAML、代码块包裹整篇等任何多余内容。
                - 文档必须从一级标题“# 智能教案生成任务”开始，到文末结束；中途不得缺章少节或变更目录结构。
                - 所有方括号中的占位/示例（如【[根据…提取]】【[设计…]】【[第一个重点知识点]】等）在最终输出中一律不得出现，必须替换为具体内容。
                - 每一节都要有“实质内容”，不得空白、不得只有标题、不得仅用一句话敷衍；表格须补全具体值；列表须给出足量条目。
                - 语言要求：中文表达，专业且清晰；适度使用 Emoji（保持原目录中出现的 Emoji），避免过度堆砌。
                - 版式要求：遵循 Markdown 规范；标题层级与顺序不得改动；列表前缀统一使用 “-” 或数字序号；段落换行规范。
                - 教案时长默认 45 分钟，如章节或目标需要可在环节中说明时间分配的合理性（总时长仍以45分钟为基准）。
                - 输出质量：可直接用于实际授课，具有可操作性与落地性，避免空泛或模板化措辞。
                
                =========================
                可用数据（请据此提取事实并写入教案）
                【教学需求】
                %s
                
                【课程章节知识点分析】
                %s
                
                【班级学习问题分析】
                %s
                
                【授课教师信息】
                %s
                
                =========================
                请严格按照以下“固定目录与结构”生成完整 Markdown（目录顺序与标题必须完全一致）：
                
                # 智能教案生成任务
                
                作为一名专业的教育AI助手，请根据以下详细信息生成一份高质量、个性化的教案。
                
                ## 教学需求
                （将“教学需求”中的关键信息转述为2-4个要点，聚焦本课要解决的核心问题与目标）
                
                ## 课程章节知识点分析
                （将“课程章节知识点分析”整合成结构化要点：本章主题、核心知识点、知识点间的逻辑关系）
                
                ## 班级学习问题分析
                （基于“班级学习问题分析”，输出高频错误点、易混概念、薄弱环节的诊断，总结问题成因）
                
                ## 授课教师信息
                （从“授课教师信息”中提取姓名、部门/教研组、教学风格与特长，并结合本课做适当匹配）
                
                ---
                
                # 教案生成要求
                
                请严格按照以下结构生成完整的Markdown格式教案：
                
                ## 📚 教案基本信息
                
                | 项目 | 内容 |
                |------|------|
                | **课程名称** | （从章节信息中提取明确的课程名称） |
                | **章节内容** | （从章节信息中提取明确的本课章节主题与范围） |
                | **授课教师** | （从教师信息中提取） |
                | **所属部门** | （从教师信息中提取） |
                | **课程时长** | 45分钟 |
                | **授课方式** | 线下面授 + 互动讨论 |
                | **适用对象** | （依据课程层次与班级情况明确对象特征） |
                
                ## 🎯 教学目标
                
                ### 知识目标
                - **核心知识点掌握**：列出3-5个核心知识点，并说明达到何种掌握程度（识记/理解/应用/迁移）
                - **概念理解**：针对重点概念给出“学生应能解释/区分/举例说明”
                - **知识体系构建**：概述本课在知识体系中的定位与前后承接关系
                
                ### 能力目标
                - **分析能力**：给出具体的情境或任务要求
                - **应用能力**：明确能将知识运用于何种问题情境
                - **问题解决能力**：指向高频错误的纠正与策略
                
                ### 情感态度目标
                - **学习兴趣**：通过案例/现象/问题激发兴趣的方式
                - **学习信心**：设置层级化任务逐步达成，增强信心
                - **合作精神**：小组讨论/协作任务安排与价值
                
                ## ⚡ 教学重点与难点
                
                ### 🔥 教学重点
                （从“核心知识点”与 is_core=1 的信息中筛选2-3个重点，并说明重点理由）
                
                ### 💪 教学难点
                （结合高频错误与易混知识点，明确2-3个难点，并解释难点成因）
                
                ### 🎯 难点突破策略
                （针对每个难点给出具体突破方法：引导问题、对比示例、可视化、操作演示、类比等）
                
                ## 📖 教学方法与策略
                
                ### 主要教学方法
                - **问题导向法**：围绕高频错误/关键概念设计递进式问题链
                - **案例教学法**：选择贴近实际的案例，指出案例与知识点的映射
                - **分层教学法**：对不同掌握层次的学生安排差异化任务
                - **互动讨论法**：组织形式、时间控制、预期产出
                
                ### 教学媒体工具
                - 多媒体课件（PPT）
                - 板书设计
                - 在线练习平台
                - 小组讨论工具
                
                ## 📋 教学过程设计
                
                ### 🚀 第一环节：课堂导入（5分钟）
                **导入方式**：设计吸引人的导入（问题/案例/复习/演示）
                **具体内容**：
                1. 导入活动1（具体描述）
                2. 导入活动2（具体描述）
                3. 连接到本节课主题（明确过渡）
                **预期效果**：激发兴趣、唤醒旧知、聚焦新课
                
                ### 📚 第二环节：新课讲授（25分钟）
                
                #### 知识点一：（填写第一个重点知识点，8分钟）
                - **讲解方式**：具体方法（例：可视化+对比案例）
                - **重点强调**：列出2-3个关键点
                - **常见错误预防**：结合“高频错误”给出前置提醒与纠正
                - **学生活动**：对应的小任务（口头/板演/小组交流/随堂练）
                
                #### 知识点二：（填写第二个重点知识点，8分钟）
                - **讲解方式**：具体方法
                - **难点突破**：针对难点的专门策略
                - **互动环节**：提问/同伴互评/教师点拨
                
                #### 知识点三：（填写第三个重点知识点，9分钟）
                - **综合应用**：小案例/综合题/真实场景任务
                - **错误纠正**：针对常见误区的即时纠偏
                - **拓展延伸**：链接下一节或更高阶内容
                
                ### 🏃 第三环节：练习巩固（10分钟）
                #### 基础练习（5分钟）
                - **练习内容**：围绕核心点设计3-5个小题（可分组）
                - **重点关注**：指向高频错误知识点的针对性练习
                
                #### 提升练习（5分钟）
                - **综合应用题**：1-2个跨知识点应用题
                - **分层指导**：针对学困生/中等/优等分别给提示或挑战
                
                ### 📝 第四环节：总结升华（5分钟）
                1. **知识梳理**：本节内容结构化总结（可用小图/要点列表）
                2. **方法归纳**：解题思路、易错点回顾
                3. **错误警示**：再次强调易错点与规避技巧
                4. **课后任务**：布置作业并说明达成目标与提交方式
                
                ## 📊 板书设计
                ```
                课题标题
                1) 核心知识点结构图（树/表/流程）
                2) 重要公式或概念（清晰对齐）
                3) 易错点提示（对比正确示例）
                4) 解题步骤/方法（编号分步）
                ```
                
                ## 📋 作业与评价
                
                ### 课后作业设计
                #### 基础巩固题（必做）
                - 给出3-5道题的简要描述（题型/目标）
                #### 能力提升题（选做）
                - 给出2-3道应用/拓展题（场景/目标）
                #### 错误纠正题（重点）
                - 指向高频错误知识点的专项练习（含“纠错提醒”）
                
                ### 评价方式
                - **过程评价**：课堂参与、讨论贡献、练习完成度
                - **结果评价**：作业成绩、达成度评估
                - **自我评价**：自评量表或反思要点
                
                ## 🔍 教学反思与改进
                
                ### 预期效果分析
                - **知识掌握预期**：针对核心点的达成情况
                - **能力提升预期**：分析/应用/迁移等维度
                - **问题解决预期**：高频错误的改善幅度
                
                ### 潜在问题预测
                - 结合班级特点预测可能的障碍点，并给出触发信号
                
                ### 应对策略
                - 与预测问题一一对应的可执行策略
                
                ### 课后改进方向
                - 根据本次实施效果的优化建议（可量化的行动项）
                
                ## 🎯 个性化教学策略
                
                ### 针对高频错误的专项策略
                - 逐条列出“错误模式 → 纠正办法 → 跟踪方式”
                
                ### 差异化教学安排
                - **学困生支持**：明确“最低达标”与具体帮扶
                - **中等生提升**：关键能力突破点与资源
                - **优等生拓展**：更高阶任务与挑战方向
                
                ### 课后跟进计划
                - 跟进时点（如T+1、T+3、T+7）
                - 跟进方式（答疑/补充练习/个别辅导）
                - 数据回收与复盘机制
                
                ---
                
                ## 🎯 特别要求
                1. **数据驱动**：必须引用“章节知识点数据”和“错误分析数据”的具体信息（但不要贴原始数据，需转化为结论与策略）。
                2. **针对性强**：全部策略与练习需直指高频错误与难点。
                3. **实用性高**：每个环节都要写“怎么做”，可操作、可落地。
                4. **个性化**：体现教师风格与班级特征（如互动方式、任务设计侧重）。
                5. **科学性**：时间安排合理，各环节衔接自然清晰。
                6. **创新性**：在传统方法上加入信息化/项目化/跨学科等现代理念。
                7. **效果性**：所有目标可检核（以可观察行为或产出为准）。
                
                请严格按照以上固定目录输出完整详细的教案内容，确保每一个部分都被“具体内容”填充，不得出现任何占位或空白。
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