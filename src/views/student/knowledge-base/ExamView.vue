<template>
  <div class="exam-view">
    <h2>课程考试内容</h2>
    <el-table :data="exams" style="width: 100%" fit>
      <el-table-column prop="name" label="试卷名称"></el-table-column>
      <el-table-column prop="startTime" label="开始时间"></el-table-column>
      <el-table-column prop="durationMinutes" label="持续时间"></el-table-column>
      <el-table-column prop="status" label="考试状态"></el-table-column>
      <!-- <el-table-column prop="myPaper" label="我的试卷"></el-table-column> -->
      <!-- <el-table-column prop="score" label="得分"></el-table-column> -->
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
        @submit="onSubmitExam"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, defineProps, watch } from 'vue';
import ExamAttempt from '@/components/ExamAttempt.vue';
import {studentService, teacherService} from '@/services/api';
const props = defineProps({
  courseId: {
    type: Number,
    required: true,
  },
});

const exams = ref([]);

const showExamDialog = ref(false);
const currentExamPaper = ref(null);
const examReadonly = ref(false);
const examDialogTitle = ref('');
const examWrongAnalysis = ref({});

async function openExam(row, readonly) {
  try {
    // 从后端获取试卷详情
    const response = await studentService.getCourseExamInfo(row.examId, '16'); // 这里的'16'应该是当前用户ID
    console.log('试卷详情:', response);
    
    if (response.data && response.data.data) {
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
              isCorrect: option.is_correct
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
              answer: correctAnswerData.answer,
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
      
      console.log('构建的错题分析数据:', wrongAnalysis);
      
      currentExamPaper.value = {
        title: row.name,
        questions: questions
      };
      
      examReadonly.value = readonly;
      examDialogTitle.value = readonly ? '试卷回顾' : '考试答题';
      examWrongAnalysis.value = wrongAnalysis;
      showExamDialog.value = true;
    }
  } catch (error) {
    console.error('获取试卷详情失败:', error);
    // 可以添加错误提示
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

function onSubmitExam({ answers }) {
  showExamDialog.value = false;
  // 这里可提交到后端或本地存储
  // 提交后可刷新考试状态等
}

const fetchAllExam = async()=> {
  const response = await studentService.getAllExams(props.courseId);
  console.log(response)
  if (response.data) {
    exams.value = response.data.data.filter(item => item.status !== "draft").map(item => ({
      id: item.examId,
      name: item.title,
      startTime: item.examDate,
      endTime: item.examDate, // 可以根据实际需要计算结束时间
      durationMinutes: item.durationMinutes + 'min',
      status: getStatusText(item.status),
      myPaper: item.status === 'completed' ? '已提交' : '未提交',
      score: item.score || null,
      examId: item.examId // 保存examId用于后续获取试卷详情
    }))
  }
}

// 状态文本转换函数
function getStatusText(status) {
  const statusMap = {
    'draft': '草稿',
    'published': '进行中',
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