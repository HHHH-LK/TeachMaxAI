<template>
  <div class="exam-view">
    <h2>课程考试内容</h2>
    <el-table :data="exams" style="width: 100%" fit>
      <el-table-column prop="name" label="试卷名称"></el-table-column>
      <el-table-column prop="startTime" label="开始时间"></el-table-column>
      <el-table-column prop="durationMinutes" label="持续时间"></el-table-column>
      <el-table-column prop="status" label="考试状态"></el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button v-if="scope.row.status === '已结束'" type="primary" link @click="openExam(scope.row, true)">查看</el-button>
          <el-button v-else-if="scope.row.status === '进行中'" type="primary" link @click="openExam(scope.row, false)">进入考试</el-button>
          <el-button v-else type="primary" link disabled>待开放</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showExamDialog" :title="examDialogTitle" width="900px" top="40px" :close-on-click-modal="false">
      <ExamAttempt
          v-if="currentExamPaper"
          :paper="currentExamPaper"
          :readonly="examReadonly"
          :wrongAnalysis="examWrongAnalysis"
          :userAnswers="currentUserAnswers"
          :showUserAnswers="showUserAnswers"
          :examId="currentExamId"
          @submit="onSubmitExam"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, defineProps, watch } from 'vue';
import { ElMessage } from 'element-plus';
import ExamAttempt from '@/components/ExamAttempt.vue';
import {studentService} from '@/services/api';
import {getCurrentUserId} from "@/utils/userUtils.js";

const props = defineProps({
  courseId: {
    type: Number,
    required: true,
  },
});

const exams = ref([]);

const showExamDialog = ref(false);
const currentExamPaper = ref(null);
const currentExamId = ref(null);
const examReadonly = ref(false);
const examDialogTitle = ref('');
const examWrongAnalysis = ref({});
const currentUserAnswers = ref({});
const showUserAnswers = ref(true);

async function openExam(row, readonly) {
  try {
    // 从后端获取试卷详情
    const userId = getCurrentUserId()
    const res = await studentService.getStudentIdByUserId(userId);
    const studentId = res.data.data.studentId;

    const response = await studentService.getCourseExamInfo(row.examId, studentId);
    console.log('试卷详情:', response);

    if (response.data && response.data.data) {
      // 检查是否已经作答
      const hasAnswered = response.data.data.some(item => item.answered === true);
      console.log('是否已经作答:', hasAnswered);

      // 如果已经作答，强制设置为只读模式
      const isReadonly = readonly || hasAnswered;

      // 转换题目格式以适配ExamAttempt组件
      const questions = response.data.data.map((item, index) => {
        const question = {
          id: `q${item.questionId}`,
          title: item.questionContent,
          type: convertQuestionType(item.questionType),
          score: item.scorePoints,
          order: item.questionOrder
        };

        // 处理选项
        if (item.questionOptions && item.questionOptions !== '[]') {
          try {
            const options = JSON.parse(item.questionOptions);
            question.options = options.map(option => ({
              value: option.label,
              label: option.content,
              isCorrect: option.isCorrect
            }));
          } catch (e) {
            console.error('解析选项失败:', e);
          }
        }

        return question;
      });

      // 构建错题分析数据
      const wrongAnalysis = {};
      response.data.data.forEach(item => {
        if (item.correctAnswer) {
          try {
            const correctAnswerData = JSON.parse(item.correctAnswer);
            wrongAnalysis[`q${item.questionId}`] = {
              answer: correctAnswerData.answer || correctAnswerData,
              explanation: correctAnswerData.explanation || item.explanation || '暂无解析'
            };
          } catch (e) {
            console.error('解析正确答案失败:', e);
            // 如果解析失败，使用原始数据
            wrongAnalysis[`q${item.questionId}`] = {
              answer: item.correctAnswer,
              explanation: item.explanation || '暂无解析'
            };
          }
        }
      });

      // 构建学生答案数据（如果已经作答）
      const userAnswers = {};
      if (hasAnswered) {
        response.data.data.forEach(item => {
          if (item.studentAnswer !== undefined && item.studentAnswer !== null) {
            const questionKey = `q${item.questionId}`;
            const questionType = convertQuestionType(item.questionType);

            // 根据题目类型转换学生答案格式
            userAnswers[questionKey] = formatStudentAnswer(item.studentAnswer, questionType);
          }
        });
      }

      console.log('构建的学生答案数据:', userAnswers);
      console.log('构建的错题分析数据:', wrongAnalysis);

      currentExamPaper.value = {
        title: row.name,
        questions: questions
      };

      currentExamId.value = row.examId;
      examReadonly.value = isReadonly;
      currentUserAnswers.value = userAnswers;
      showUserAnswers.value = hasAnswered; // 只有已作答时才显示学生答案

      // 根据状态设置弹窗标题
      if (hasAnswered) {
        examDialogTitle.value = '试卷回顾 - 已提交';
      } else if (readonly) {
        examDialogTitle.value = '试卷回顾';
      } else {
        examDialogTitle.value = '考试答题';
      }

      examWrongAnalysis.value = wrongAnalysis;
      showExamDialog.value = true;
    }
  } catch (error) {
    console.error('获取试卷详情失败:', error);
    ElMessage.error('获取试卷详情失败，请重试');
  }
}

// 格式化学生答案
function formatStudentAnswer(studentAnswer, questionType) {
  if (studentAnswer === undefined || studentAnswer === null || studentAnswer === '') {
    return undefined;
  }

  switch (questionType) {
    case 'multiple':
      // 多选题：字符串转换为数组
      if (typeof studentAnswer === 'string' && studentAnswer.includes(',')) {
        return studentAnswer.split(',').map(ans => ans.trim());
      }
      return Array.isArray(studentAnswer) ? studentAnswer : [studentAnswer];

    case 'judge':
      // 判断题：转换为布尔值
      if (studentAnswer === 'true' || studentAnswer === true || studentAnswer === '正确') {
        return 'A';
      }
      if (studentAnswer === 'false' || studentAnswer === false || studentAnswer === '错误') {
        return 'B';
      }
      return studentAnswer;

    case 'single':
    case 'short':
    case 'blank':
    default:
      return studentAnswer;
  }
}

// 题目类型转换函数
function convertQuestionType(type) {
  const typeMap = {
    'single_choice': 'single',
    'multiple_choice': 'multiple',
    'true_false': 'judge',
    'fill_blank': 'blank',
    'short_answer': 'short'
  }
  return typeMap[type] || type
}

async function onSubmitExam({ answers, submitData }) {
  try {
    console.log('提交的答案:', answers);
    console.log('提交的数据:', submitData);

    // 调用后端API提交试卷
    const response = await studentService.finishExam(submitData);
    console.log('提交响应:', response);

    if (response.data && response.data.success) {
      // 更新本地考试状态
      const currentExamIndex = exams.value.findIndex(exam => exam.examId === currentExamId.value);
      if (currentExamIndex !== -1) {
        exams.value[currentExamIndex].status = '已结束';
        exams.value[currentExamIndex].myPaper = '已提交';
      }

      showExamDialog.value = false;
      ElMessage.success('试卷提交成功！');

      // 刷新考试列表
      await fetchAllExam();
    } else {
      throw new Error(response.data?.message || '提交失败');
    }

  } catch (error) {
    console.error('提交试卷失败:', error);

    // 根据错误类型显示不同的错误信息
    let errorMessage = '提交试卷失败，请重试';

    if (error.response) {
      // 服务器返回了错误状态码
      if (error.response.status === 400) {
        errorMessage = '提交数据格式错误';
      } else if (error.response.status === 401) {
        errorMessage = '登录已过期，请重新登录';
      } else if (error.response.status === 403) {
        errorMessage = '没有权限提交此试卷';
      } else if (error.response.status === 404) {
        errorMessage = '试卷不存在';
      } else if (error.response.status >= 500) {
        errorMessage = '服务器错误，请稍后重试';
      }
    } else if (error.message) {
      errorMessage = error.message;
    }

    ElMessage.error(errorMessage);
  }
}

const fetchAllExam = async()=> {
  try {
    const response = await studentService.getAllExams(props.courseId);
    console.log('获取考试列表:', response);

    if (response.data && response.data.data) {
      exams.value = response.data.data.filter(item => item.status !== "draft").map(item => ({
        id: item.examId,
        name: item.title,
        startTime: formatDateTime(item.examDate),
        endTime: item.examDate, // 可以根据实际需要计算结束时间
        durationMinutes: item.duration + 'min',
        status: getStatusText(item.status),
        myPaper: item.status === 'completed' ? '已提交' : '未提交',
        score: item.score || null,
        examId: item.examId // 保存examId用于后续获取试卷详情
      }));
    }
  } catch (error) {
    console.error('获取考试列表失败:', error);
    ElMessage.error('获取考试列表失败');
  }
}

// 时间格式化函数
function formatDateTime(dateTimeString) {
  if (!dateTimeString) return '';
  try {
    const date = new Date(dateTimeString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch (e) {
    console.error('时间格式化失败:', e);
    return dateTimeString;
  }
}

// 状态文本转换函数
function getStatusText(status) {
  const statusMap = {
    'draft': '草稿',
    'scheduled': '进行中',
    'completed': '已结束',
    'expired': '已过期'
  }
  return statusMap[status] || status
}

watch(
    () => props.courseId,
    (newCourseId) => {
      if (newCourseId) {
        fetchAllExam();
      }
    },
    { immediate: true }
);
</script>

<style scoped>
.exam-view {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>