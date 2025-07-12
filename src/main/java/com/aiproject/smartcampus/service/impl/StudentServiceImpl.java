package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.commons.utils.PasswordEncryptionUtils;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.commons.utils.UserToTypeUtils;
import com.aiproject.smartcampus.exception.StudentExpection;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.ExamCorrectAnswer;
import com.aiproject.smartcampus.pojo.dto.StudentAnswerDTO;
import com.aiproject.smartcampus.pojo.dto.StudentExamAnswerDTO;
import com.aiproject.smartcampus.pojo.po.QuestionBank;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.StudentAnswer;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.pojo.vo.ExamQuestionVO;
import com.aiproject.smartcampus.pojo.vo.StudentKnowledgePointVO;
import com.aiproject.smartcampus.pojo.vo.StudentSelectAllVO;
import com.aiproject.smartcampus.pojo.vo.StudentWrongQuestionVO;
import com.aiproject.smartcampus.service.StudentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @program: SmartCampus
 * @description:
 * @author: lk
 * @create: 2025-05-20 08:38
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private final StudentMapper studentMapper;

    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserToTypeUtils userToTypeUtils;

    private final QuestionBankMapper questionBankMapper;

    private final ExamMapper examMapper;

    private final StudentAnswerMapper studentAnswerMapper;

    private final ChatLanguageModel chatLanguageModel;

    private final KnowledgePointMapper knowledgePointMapper;

    /**
     * 查询所有学生信息
     *
     * @return
     */
    @Override
    public List<StudentSelectAllVO> selectAllStudents() {
        return studentMapper.selectAllStudents();
    }

    /**
     * 根据学生学号查询学生信息
     *
     * @param studentNumber
     * @return
     */
    @Override
    public Student findByStudentNumber(String studentNumber) {
        Student student = studentMapper.findByStudentNumber(studentNumber);
        return student;
    }

    /**
     * 根据班级名称查询学生信息
     *
     * @param className
     * @return
     */
    @Override
    public List<Student> findByClassName(String className) {
        return studentMapper.findByClassName(className);
    }

    /**
     * 更新学生信息
     *
     * @param student
     */
    @Override
    @Transactional//添加事务注解，保证数据一致性
    public void updateStudent(Student student) {
        User user = new User();
        BeanUtils.copyProperties(student.getUser(), user);
        // 更新学生信息
        studentMapper.updateById(student);
        // 更新学生基本信息
        //加密密码
        user.setPasswordHash(PasswordEncryptionUtils.encryption(user.getPasswordHash()));
        userMapper.updateById(user);
    }

    @Override
    public String academicAnalysis(String courseId) {

        try {
            /*String studentId = userToTypeUtils.change();*/
            String studentId = "1";

            //根据学生考试情况以及答题记录
            log.info("开始检索学生{}错误题目信息", studentId);
            List<StudentWrongQuestionVO> studentWrongQuestionVOS = studentMapper.selectWrongQuestion(studentId, courseId);
            log.info("检索数据成功,数据条数{}", studentWrongQuestionVOS.size());
            StringBuffer questionStringBuffer = new StringBuffer();
            for (StudentWrongQuestionVO studentWrongQuestionVO : studentWrongQuestionVOS) {
                questionStringBuffer.append(studentWrongQuestionVO.toString() + "\n");
            }
            String questionStringBufferString = questionStringBuffer.toString();

            //查询当前学生的知识点掌握情况
            log.info("开始检索学生{}知识点信息", studentId);
            StringBuffer knowledgePointStringBuffer = new StringBuffer();
            List<StudentKnowledgePointVO> studentKnowledgePointVOS = knowledgePointMapper.selectKnowledgePointByStudentIdOptionalCourseId(studentId, courseId);
            log.info("检索数据成功,数据条数{}", studentWrongQuestionVOS.size());
            for (StudentKnowledgePointVO studentKnowledgePointVO : studentKnowledgePointVOS) {
                knowledgePointStringBuffer.append(studentKnowledgePointVO.toString() + "\n");
            }
            String knowledgePointStringBufferString = knowledgePointStringBuffer.toString();

            //设计提示词
            String PROMPT = buildAnalysisPrompt(knowledgePointStringBufferString, questionStringBufferString);

            log.info("开始生成学情分析报告");
            ChatResponse response = chatLanguageModel.chat(UserMessage.userMessage(PROMPT));
            log.info("学情分析报告生成完毕");

            return response.aiMessage().text();

        } catch (Exception e) {

            log.info("学习情报生成失败");
            throw new RuntimeException("学习情报生成失败");
        }

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finshExam(StudentExamAnswerDTO studentExamAnswerDTO) {

        //解析学生的答题情况
        List<StudentAnswerDTO> studentExamAnswerDTOList = studentExamAnswerDTO.getStudentExamAnswerDTOList();

        Integer i = 0;

        //获取答题情况
        for (StudentAnswerDTO studentAnswerDTO : studentExamAnswerDTOList) {

            Integer questionId = studentAnswerDTO.getQuestionId();
            //查询正确题目信息进行校验
            QuestionBank questionBank = questionBankMapper.selectById(questionId);
            if (questionBank == null) {
                log.error("题目{}不存在", questionId);
                throw new StudentExpection("考试题目不存在");
            }

            log.info("提取出学生的第{}题目信息", i++);

            String studentId = userToTypeUtils.change();
            String examId = studentExamAnswerDTO.getExamId();
            StudentAnswer studentAnswer = new StudentAnswer();
            studentAnswer.setStudentId(Integer.valueOf(studentId));
            studentAnswer.setExamId(Integer.valueOf(examId));
            studentAnswer.setQuestionId(questionId);
            studentAnswer.setStudentAnswer(studentAnswerDTO.getFormattedAnswer());

            studentAnswerMapper.insert(studentAnswer);

        }

        log.info("题目入库完毕,学生一共提交了[{}]道题目", studentExamAnswerDTOList.size());

    }


    // 构建分析提示词的方法
    private String buildAnalysisPrompt(String knowledgePointData, String questionData) {
        return String.format("""
                你是一位专业的教育数据分析师和学习顾问。请基于以下学生的学习数据，生成一份专业、全面的学情分析报告。
                
                ### 🎯 分析任务
                请深入分析学生的学习状况，生成个性化的学情报告和学习建议。
                
                ### 📊 数据源
                **学生知识点掌握情况：**
                %s
                
                **学生错题记录：**
                %s
                
                ### 📋 分析要求
                请按以下结构生成分析报告（使用markdown格式）：
                
                #### 1. 📈 学习现状总览 (200-250字)
                - 基于知识点掌握数据，评估整体学习水平
                - 分析各课程学习进度和掌握程度分布
                - 概括主要学习特征和表现
                
                #### 2. 🧠 知识掌握深度分析 (300-350字)
                - **课程掌握对比**：各课程间的掌握水平差异
                - **核心知识点分析**：重点关注isCorePoint=true的知识点掌握情况
                - **难度分布**：easy/medium/hard各难度级别的掌握状况
                - **学习投入效果**：practiceCount与practiceScore的关系分析
                - **章节进度评估**：chapterProgressRate反映的学习进度
                
                #### 3. ❌ 错题深度剖析 (300-350字)
                - **错题类型分析**：single_choice/multiple_choice/true_false/fill_blank/short_answer的错误分布
                - **难度集中度**：错题在easy/medium/hard的分布特征
                - **课程薄弱点**：各courseName中的错题集中情况
                - **答题准确性**：scoreEarned与scorePoints的得分率分析
                - **错误模式**：高频错误类型和潜在原因
                
                #### 4. 🔍 学习行为洞察 (200-250字)
                - **练习频次**：practiceCount反映的学习勤奋度
                - **学习时长**：chapterStudyTime体现的时间投入
                - **学习路径**：基于chapterOrder和pointOrderInChapter的学习轨迹
                - **学习专注度**：各知识点投入时间与掌握效果的匹配度
                
                #### 5. ⚖️ 优势与挑战识别 (250-300字)
                **💪 学习优势：**
                - 至少3个具体优势（基于掌握程度高的知识点和错题少的领域）
                - 学习习惯中的积极表现
                
                **🔧 学习挑战：**
                - 至少3个需要改进的方面（基于未掌握的核心知识点和高频错题）
                - 学习效率和方法上的问题
                
                #### 6. 💡 个性化学习建议 (400-450字)
                **🎯 即刻行动（本周内）：**
                - 3-4个基于当前数据的紧急改进建议
                - 重点关注masteryLevel='not_learned'的核心知识点
                
                **📈 短期计划（2-4周）：**
                - 针对错题集中的questionType制定专项练习计划
                - 对practiceScore<60分的知识点进行重点突破
                
                **🚀 中期目标（1-2个月）：**
                - 基于课程整体掌握情况制定系统学习计划
                - 提升章节学习效率和知识点掌握深度
                
                **📚 学习方法建议：**
                - 根据错题类型提供针对性的解题策略
                - 基于学习时长和效果提供时间管理建议
                
                #### 7. ⚠️ 重点关注预警 (150-200字)
                - 需要优先解决的学习问题（基于核心知识点掌握不足）
                - 潜在的学习风险（基于错题趋势和学习投入度）
                - 家长和教师需要重点关注的方面
                
                ### 📝 输出要求
                1. 每个分析结论都要有具体的数据支撑
                2. 使用专业但易懂的教育语言
                3. 建议具体可操作，避免空泛表述
                4. 语调积极正面，注重鼓励和指导
                5. 关键数据和建议用**加粗**突出
                6. 使用emoji增加可读性
                7. 确保分析客观准确，建议切实可行
                
                请开始分析并生成完整的学情报告。
                """, knowledgePointData, questionData);
    }

}
