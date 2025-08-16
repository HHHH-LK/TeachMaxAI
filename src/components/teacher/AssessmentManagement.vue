<template>
  <div class="assessment-management">
    <div class="section-header">
      <h2>考核管理</h2>
      <div class="header-actions">
        <el-button type="success" @click="showGenerateModal = true">
          <el-icon><Star /></el-icon>
          智能生成试卷
        </el-button>
      </div>
    </div>

    <!-- 考核列表 -->
    <AssessmentList
      :assessments="assessments"
      @view="viewAssessment"
      @view-exam="viewExamContent"
      @grade="startGrading"
      @delete="deleteAssessment"
      @view-submissions="viewSubmissions"
    />

    <!-- 智能生成试卷模态框 -->
    <GenerateExamModal
      v-model="showGenerateModal"
      @close="showGenerateModal = false"
      @generated="handleExamGenerated"
    />

    <!-- 智能阅卷模态框 -->
    <GradingModal
      v-model="showGradingModal"
      :assessment="selectedAssessment"
      @close="handleCloseGrading"
      @save-submission="saveSubmissionGrading"
    />

    <!-- 查看考核详情模态框 -->
    <AssessmentDetailModal
      v-model="showViewModal"
      :assessment="selectedAssessment"
    />

    <!-- 查看考试内容模态框 -->
    <ExamContentModal
      v-model="showExamModal"
      :examPaper="selectedExamPaper"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Star } from "@element-plus/icons-vue";
import {studentService, teacherService} from "@/services/api";

// 导入子组件
import AssessmentList from './assessmentManage/AssessmentList.vue';
import GenerateExamModal from './assessmentManage/GenerateExamModal.vue';
import GradingModal from './assessmentManage/GradingModal.vue';
import AssessmentDetailModal from './assessmentManage/AssessmentDetailModal.vue';
import ExamContentModal from './assessmentManage/ExamContentModal.vue';

// 响应式数据
const showGenerateModal = ref(false);
const showGradingModal = ref(false);
const showViewModal = ref(false);
const showExamModal = ref(false);
const selectedAssessment = ref(null);
const selectedExamPaper = ref(null);

// 定义props接收courseId
const props = defineProps({
  courseId: {
    type: [String, Number],
    default: 1
  }
});

// 当前课程ID
const currentCourseId = computed(() => props.courseId);

// 考核列表数据

const assessments = ref([]);
// 处理试卷生成成功
const handleExamGenerated = (newAssessment) => {
  assessments.value.unshift(newAssessment);
  ElMessage.success("试卷已添加到考核列表");
};

// 开始阅卷
const startGrading = async (assessment) => {
  try {
    console.log("开始阅卷，assessment对象:", assessment);
    
    // 获取该考试的所有学生作答信息
    const studentResponse = await teacherService.getExamStudentInfo(assessment.id);
    console.log("学生列表:", studentResponse);
    
    if (studentResponse.data && studentResponse.data.success) {
      const students = studentResponse.data.data;
      
      // 为每个学生获取详细的作答信息
      const studentsWithAnswers = await Promise.all(
        students.map(async (student) => {
          try {
            const answerResponse = await studentService.getCourseExamInfo(assessment.id, student.studentId);
            console.log(`学生${student.studentName}的作答详情:`, answerResponse);
            
            if (answerResponse.data && answerResponse.data.success) {
              // 转换题目格式以适配ExamAttempt组件
              const questions = answerResponse.data.data.map((item, index) => {
                const question = {
                  id: `q${item.questionId}`,
                  title: item.questionContent,
                  type: convertQuestionType(item.questionType),
                  score: item.scorePoints,
                  order: item.questionOrder,
                  options: []
                };
                
                // 处理选项
                if (item.questionOptions && item.questionOptions !== '[]') {
                  try {
                    const options = JSON.parse(item.questionOptions);
                    question.options = options.map(option => ({
                      value: option.label,
                      label: option.content,
                      isCorrect: option.is_correct
                    }));
                  } catch (e) {
                    console.error('解析选项失败:', e);
                  }
                }
                
                return question;
              });
              
              // 构建学生答案对象
              const userAnswers = {};
              answerResponse.data.data.forEach(item => {
                userAnswers[`q${item.questionId}`] = item.studentAnswer || '';
              });
              
              return {
                ...student,
                questions: questions,
                userAnswers: userAnswers,
                totalQuestions: questions.length,
                isGraded: student.score !== null,
                totalScore: student.score || 0
              };
            } else {
              return {
                ...student,
                questions: [],
                userAnswers: {},
                totalQuestions: 0,
                isGraded: false,
                totalScore: 0
              };
            }
          } catch (error) {
            console.error(`获取学生${student.studentName}作答详情失败:`, error);
            return {
              ...student,
              questions: [],
              userAnswers: {},
              totalQuestions: 0,
              isGraded: false,
              totalScore: 0
            };
          }
        })
      );
      
      // 将学生作答信息添加到assessment对象中
      assessment.studentsWithAnswers = studentsWithAnswers;
      console.log("成功获取所有学生作答信息:", assessment.studentsWithAnswers);
      
      selectedAssessment.value = assessment;
      showGradingModal.value = true;
    } else {
      ElMessage.error("获取学生信息失败");
    }
  } catch (error) {
    console.error("获取学生作答信息失败:", error);
    ElMessage.error("获取学生作答信息失败，请重试");
  }
};

// 题目类型转换函数
const convertQuestionType = (type) => {
  const typeMap = {
    'single_choice': 'single',
    'multiple_choice': 'multiple',
    'true_false': 'judge',
    'fill_blank': 'blank',
    'short_answer': 'short'
  };
  return typeMap[type] || 'single';
};

// 保存单个学生评分
const saveSubmissionGrading = async (submission) => {
  try {
    // 计算总分
    submission.totalScore = submission.answers.reduce((sum, answer) => sum + (answer.manualScore || 0), 0);
    submission.isGraded = true;
    submission.gradedCount = submission.totalQuestions;
    
    ElMessage.success(`${submission.studentName} 评分已保存`);
  } catch (error) {
    console.error('保存评分失败:', error);
    ElMessage.error('保存评分失败');
  }
};

// 关闭阅卷模态框
const handleCloseGrading = () => {
  showGradingModal.value = false;
  selectedAssessment.value = null;
};

// 查看考核详情
const viewAssessment = (assessment) => {
  selectedAssessment.value = assessment;
  showViewModal.value = true;
};

// 查看考试内容
const viewExamContent = async (assessment) => {
  try {
    const response = await teacherService.assessment.getCourseExamInfo(assessment.id);
    console.log("考试内容响应:", response);

    if (response.data && response.data.success) {
      // 将API返回的题目数据转换为组件需要的格式
      const questions = response.data.data.map(question => {
        // 转换题目类型为ExamAttempt组件支持的格式
        let questionType = question.questionType;
        switch (question.questionType) {
          case 'single_choice':
            questionType = 'single';
            break;
          case 'multiple_choice':
            questionType = 'multiple';
            break;
          case 'true_false':
            questionType = 'judge';
            break;
          case 'fill_blank':
            questionType = 'blank';
            break;
          case 'short_answer':
            questionType = 'short';
            break;
          default:
            questionType = 'single';
        }

        // 解析正确答案
        let correctAnswer = null;
        if (question.correctAnswer) {
          try {
            const parsed = JSON.parse(question.correctAnswer);
            correctAnswer = parsed.answer || parsed;
          } catch (e) {
            correctAnswer = question.correctAnswer;
          }
        }

        return {
          id: question.questionId.toString(),
          title: question.questionContent,
          type: questionType,
          score: question.scorePoints,
          options: question.questionOptions ? JSON.parse(question.questionOptions) : [],
          correctAnswer: correctAnswer,
          explanation: question.explanation,
          difficultyLevel: question.difficultyLevelCn,
          chapterName: question.chapterName,
          pointName: question.pointName
        };
      });

      selectedExamPaper.value = {
        title: assessment.title,
        questions: questions
      };
      showExamModal.value = true;
    } else {
      ElMessage.error(response.data?.message || "获取考试内容失败");
    }
  } catch (error) {
    console.error("获取考试内容失败:", error);
    ElMessage.error("获取考试内容失败，请重试");
  }
};

// 删除考核
const deleteAssessment = async (id) => {
  try {
    await ElMessageBox.confirm(
      "确定要删除这个考核吗？删除后无法恢复。",
      "删除确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );

    const response = await teacherService.assessment.deleteExamById(id);
    console.log(response)

    if (response.data && response.data.success) {
      ElMessage.success("考核删除成功");
      // 删除成功后刷新考核列表
      await loadAssessments();
    } else {
      ElMessage.error(response.data?.message || "删除失败");
    }
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除考核失败:", error);
      ElMessage.error("删除失败，请重试");
    }
  }
};

// 初始化加载考核列表
const loadAssessments = async () => {
  try {
    const response = await teacherService.assessment.generateExam(currentCourseId.value);
    console.log("考核列表数据:", response)

    if (response.data && response.data.success) {
      // 将API返回的数据转换为组件需要的格式
      const examList = response.data.data;
      
      // 获取每个考试的学生信息
      const examWithStudents = await Promise.all(
        examList.map(async (exam) => {
          try {
            const studentResponse = await teacherService.getExamStudentInfo(exam.examId);
            console.log("获取学生信息响应:", studentResponse);
            let submittedCount = 0;
            let gradedCount = 0;
            
            if (studentResponse.data && studentResponse.data.success) {
              const students = studentResponse.data.data;
              // 所有学生都默认已提交
              submittedCount = students.length;
              // 计算已阅卷数量（分数不为null且大于等于0的学生）
              gradedCount = students.filter(student => student.score !== null && student.score >= 0).length;
            }
            
            // 根据考试状态和阅卷情况确定显示状态
            let displayStatus = exam.status;
            if (exam.status === 'scheduled' && gradedCount >= submittedCount && submittedCount > 0) {
              displayStatus = 'completed'; // 如果所有提交的学生都已阅卷，则标记为已完成
            }
            
            return {
              id: exam.examId.toString(),
              title: exam.title,
              type: "期末考试",
              status: displayStatus,
              totalQuestions: exam.questionCount || 0,
              totalScore: exam.max_score || 0,
              totalStudents: 6, // 根据API返回的学生数量
              submittedCount: submittedCount,
              gradedCount: gradedCount,
              createdAt: exam.createdAt,
              lastSubmissionTime: exam.lastSubmissionTime || null,
              chapters: [exam.courseId],
              knowledgePoints: ["综合项目实践"],
              examPaper: {
                title: exam.title,
                questions: [] // 试卷题目数据需要另外获取
              }
            };
          } catch (error) {
            console.error(`获取考试${exam.examId}的学生信息失败:`, error);
            // 如果获取学生信息失败，使用默认值
            return {
              id: exam.examId.toString(),
              title: exam.title,
              type: "期末考试",
              status: exam.status,
              totalQuestions: exam.questionCount || 0,
              totalScore: exam.max_score || 0,
              totalStudents: 6,
              submittedCount: 6, // 默认所有学生都已提交
              gradedCount: 0,
              createdAt: exam.createdAt,
              lastSubmissionTime: null,
              chapters: [exam.courseId],
              knowledgePoints: ["综合项目实践"],
              examPaper: {
                title: exam.title,
                questions: []
              }
            };
          }
        })
      );
      
      assessments.value = examWithStudents;
    }
  } catch (error) {
    console.error("加载考核列表失败:", error);
    // 如果API调用失败，保持使用模拟数据
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadAssessments();
});
</script>

<style scoped>
.assessment-management {
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h2 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .assessment-management {
    padding: 16px;
  }

  .section-header {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }
}
</style>
