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
      @publish="publishAssessment"
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
import { teacherService } from "@/services/api";

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
const assessments = ref([
  {
    id: "5",
    title: "Java程序设计（A）",
    type: "期末考试",
    status: "draft",
    totalQuestions: 25,
    totalScore: 100,
    totalStudents: 7,
    submittedCount: 0,
    gradedCount: 0,
    createdAt: "2024-03-25T09:15:00Z",
    lastSubmissionTime: null,
    chapters: [5],
    knowledgePoints: ["综合项目实践"],
    examPaper: {
      "title": "2025上学期《Java程序设计》期末测验",
      "questions": [
        {
          "id": "q1",
          "title": "Java程序的三大特性是什么？请按顺序写出名称。",
          "type": "short",
          "score": 6
        },
        {
          "id": "q2",
          "title": "抽象类和接口的区别是？请简要说明。",
          "type": "short",
          "score": 6
        },
        {
          "id": "q3",
          "title": "用于实现多态性的Java机制是____。",
          "type": "blank",
          "score": 3
        },
        {
          "id": "q4",
          "title": "下列哪项不是Java关键字？",
          "type": "single",
          "score": 4,
          "options": [
            {"value": "A", "label": "const"},
            {"value": "B", "label": "interface"},
            {"value": "C", "label": "null"}
          ]
        },
        {
          "id": "q5",
          "title": "请选择所有访问修饰符。",
          "type": "multiple",
          "score": 4,
          "options": [
            {"value": "A", "label": "public"},
            {"value": "B", "label": "private"},
            {"value": "C", "label": "synchronized"}
          ]
        },
        {
          "id": "q6",
          "title": "Java中实现线程安全的集合类是____。",
          "type": "blank",
          "score": 3
        },
        {
          "id": "q7",
          "title": "this关键字的作用？",
          "type": "short",
          "score": 6
        },
        {
          "id": "q8",
          "title": "下列哪项不是基本数据类型？",
          "type": "single",
          "score": 4,
          "options": [
            {"value": "A", "label": "int"},
            {"value": "B", "label": "String"},
            {"value": "C", "label": "char"}
          ]
        },
        {
          "id": "q9",
          "title": "以下哪些属于运行时异常？",
          "type": "multiple",
          "score": 4,
          "options": [
            {"value": "A", "label": "NullPointerException"},
            {"value": "B", "label": "FileNotFoundException"},
            {"value": "C", "label": "IOException"},
            {"value": "D", "label": "ArrayIndexOutOfBoundsException"}
          ]
        },
        {
          "id": "q10",
          "title": "JVM的作用是？",
          "type": "single",
          "score": 4,
          "options": [
            {"value": "A", "label": "编译Java代码"},
            {"value": "B", "label": "执行字节码文件"},
            {"value": "C", "label": "调试程序"}
          ]
        },
        {
          "id": "q11",
          "title": "反射机制主要应用于？",
          "type": "single",
          "score": 4,
          "options": [
            {"value": "A", "label": "动态加载类"},
            {"value": "B", "label": "内存管理"},
            {"value": "C", "label": "异常处理"}
          ]
        },
        {
          "id": "q12",
          "title": "try-with-resources语法需实现____接口。",
          "type": "blank",
          "score": 3
        },
        {
          "id": "q13",
          "title": "Math.round(11.5)的返回值是____。",
          "type": "blank",
          "score": 3
        },
        {
          "id": "q14",
          "title": "Java中数组是对象。",
          "type": "judge",
          "score": 3
        },
        {
          "id": "q15",
          "title": "简述static关键字的四种应用场景。",
          "type": "short",
          "score": 6
        },
        {
          "id": "q16",
          "title": "Java中用于定义接口的关键字是？",
          "type": "single",
          "score": 4,
          "options": [
            {"value": "A", "label": "class"},
            {"value": "B", "label": "interface"},
            {"value": "C", "label": "abstract"},
            {"value": "D", "label": "extends"}
          ]
        },
        {
          "id": "q17",
          "title": "以下哪些是Java集合框架中的接口？",
          "type": "multiple",
          "score": 4,
          "options": [
            {"value": "A", "label": "List"},
            {"value": "B", "label": "Map"},
            {"value": "C", "label": "Set"},
            {"value": "D", "label": "Array"}
          ]
        },
        {
          "id": "q18",
          "title": "Java中HashMap允许null键和null值。",
          "type": "judge",
          "score": 4
        },
        {
          "id": "q19",
          "title": "Java中final关键字可以修饰哪些元素？",
          "type": "multiple",
          "score": 4,
          "options": [
            {"value": "A", "label": "类"},
            {"value": "B", "label": "方法"},
            {"value": "C", "label": "变量"},
            {"value": "D", "label": "接口"}
          ]
        },
        {
          "id": "q20",
          "title": "Java中String类是可变类。",
          "type": "judge",
          "score": 3
        },
        {
          "id": "q21",
          "title": "Java中实现多线程的方式有哪几种？",
          "type": "short",
          "score": 6
        },
        {
          "id": "q22",
          "title": "Java中用于创建对象的关键字是____。",
          "type": "blank",
          "score": 3
        },
        {
          "id": "q23",
          "title": "Java中super关键字的作用是____。",
          "type": "blank",
          "score": 3
        },
        {
          "id": "q24",
          "title": "Java中Thread类的start()方法用于启动线程。",
          "type": "judge",
          "score": 2
        },
        {
          "id": "q25",
          "title": "Java中异常处理机制使用哪些关键字？",
          "type": "multiple",
          "score": 4,
          "options": [
            {"value": "A", "label": "try"},
            {"value": "B", "label": "catch"},
            {"value": "C", "label": "finally"},
            {"value": "D", "label": "throw"}
          ]
        }
      ]
    },
  },
]);

// 处理试卷生成成功
const handleExamGenerated = (newAssessment) => {
  assessments.value.unshift(newAssessment);
  ElMessage.success("试卷已添加到考核列表");
};

// 开始阅卷
const startGrading = (assessment) => {
  selectedAssessment.value = assessment;
  showGradingModal.value = true;
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

// 查看学生提交情况
const viewSubmissions = (assessment) => {
  ElMessage.info("查看提交功能开发中...");
};

// 发布考核
const publishAssessment = async (assessment) => {
  try {
    await ElMessageBox.confirm(
      `确定要发布考核“${assessment.title}”吗？发布后学生将可见并可作答。`,
      "发布确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    const response = await teacherService.assessment.releasePaper(assessment.id);
    console.log("发布试卷响应:", response);

    if (response.data && response.data.success) {
      ElMessage.success("考核已发布");
      // 发布成功后刷新考核列表
      await loadAssessments();
    } else {
      ElMessage.error(response.data?.message || "发布失败");
    }
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("发布失败，请重试");
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
      assessments.value = response.data.data.map(exam => ({
        id: exam.examId.toString(),
        title: exam.title,
        type: "期末考试",
        status: exam.status,
        totalQuestions: 25, // 默认题目数量
        totalScore: exam.maxScore,
        totalStudents: 7, // 默认学生数量
        submittedCount: 0,
        gradedCount: 0,
        createdAt: exam.createdAt,
        lastSubmissionTime: null,
        chapters: [exam.courseId],
        knowledgePoints: ["综合项目实践"],
        examPaper: {
          title: exam.title,
          questions: [] // 试卷题目数据需要另外获取
        }
      }));
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
