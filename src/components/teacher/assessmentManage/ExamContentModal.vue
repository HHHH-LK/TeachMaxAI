<template>
  <el-dialog
      v-model="visible"
      width="900px"
      :before-close="handleClose"
  >
    <!-- 自定义弹窗头部，包含标题和导出按钮 -->
    <template #header>
      <div class="dialog-header">
        <span class="dialog-title">考试内容</span>
        <el-button
            type="primary"
            size="small"
            @click="handleExportExam"
            class="export-btn"
        >
          <el-icon><Download /></el-icon> 导出当前考试题
        </el-button>
      </div>
    </template>

    <!-- 考试内容区域 -->
    <div v-if="examPaper" class="exam-content">
      <ExamAdit
          v-if="!props.isView"
          :paper="examPaper"
          :readonly="true"
          :wrong-analysis="correctAnswers"
          :user-answers="{}"
          :show-user-answers="false"
          :courseId="props.courseId"
      />

      <ExamAttempt
          v-if="props.isView"
          :paper="examPaper"
          :readonly="true"
          :wrong-analysis="correctAnswers"
          :user-answers="{}"
          :show-user-answers="false"
          :courseId="props.courseId"
      />
    </div>
  </el-dialog>
</template>

<script setup>
import {computed} from 'vue';
import ExamAttempt from '@/components/ExamAttempt.vue';
import ExamAdit from '@/components/ExamAdit.vue';
import * as XLSX from 'xlsx';
import {ElMessage} from 'element-plus';
import {Download} from '@element-plus/icons-vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  examPaper: {
    type: Object,
    default: null
  },
  courseId: {
    type: [String, Number],
    default: 1
  },
  isView: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['update:modelValue']);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

// 生成正确答案对象
const correctAnswers = computed(() => {
  if (!props.examPaper || !props.examPaper.questions) {
    return {};
  }

  const answers = {};
  props.examPaper.questions.forEach(question => {
    if (question.correctAnswer) {
      answers[question.id] = {
        answer: question.correctAnswer,
        explanation: question.explanation
      };
    }
  });

  return answers;
});

// 导出考试题的核心逻辑
const handleExportExam = () => {
  // 检查是否有试卷数据
  if (!props.examPaper || !props.examPaper.questions || props.examPaper.questions.length === 0) {
    ElMessage.warning('暂无考试数据可导出');
    return;
  }

  // 获取试卷基础信息
  const paperName = props.examPaper.title || '未知试卷';
  const paperId = props.examPaper.examId || '未知ID';

  // 处理试题数据
  const exportData = props.examPaper.questions.map((question, index) => {
    // 处理选项
    let optionsStr = '无选项';
    if (question.options && question.options.length > 0) {
      optionsStr = question.options
          .map(opt => `${opt.label}：${opt.content}（${opt.isCorrect ? '正确' : '错误'}）`)
          .join('\n');
    }

    // 处理正确答案
    let correctAnswerStr = '无';
    if (question.correctAnswer) {
      correctAnswerStr = Array.isArray(question.correctAnswer)
          ? question.correctAnswer.join(',')
          : String(question.correctAnswer);
    }

    // 处理题型
    const questionTypeMap = {
      'multiple': '多选题',
      'single': '单选题',
      'judge': '判断题',
      'blank': '填空题',
      'short': '简答题'
    };
    const questionType = questionTypeMap[question.type] || question.type;

    return {
      '试卷ID': paperId,
      '试卷名称': paperName,
      '题目序号': index + 1,
      '题目ID': question.id || '未知',
      '所属章节': question.chapterName || '未分类',
      '知识点': question.pointName || '无',
      '题型': questionType,
      '题目内容': question.title || '无',
      '选项（含正确性）': optionsStr,
      '正确答案': correctAnswerStr,
      '答案解析': question.explanation || '无',
      '题目分值': question.score || 0,
      '难度等级': question.difficultyLevel || '无'
    };
  });

  // 生成Excel文件
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet(exportData);

  // 优化Excel列宽
  const wscols = [
    {wch: 12}, // 试卷ID
    {wch: 20}, // 试卷名称
    {wch: 10}, // 题目序号
    {wch: 12}, // 题目ID
    {wch: 15}, // 所属章节
    {wch: 20}, // 知识点
    {wch: 10}, // 题型
    {wch: 40}, // 题目内容
    {wch: 50}, // 选项（含正确性）
    {wch: 20}, // 正确答案
    {wch: 50}, // 答案解析
    {wch: 10}, // 题目分值
    {wch: 10}  // 难度等级
  ];
  worksheet['!cols'] = wscols;

  // 添加工作表并下载
  XLSX.utils.book_append_sheet(workbook, worksheet, '试题列表');
  XLSX.writeFile(workbook, `${paperName}_考试题导出.xlsx`);

  // 提示导出成功
  ElMessage.success(`《${paperName}》考试题导出成功，共${exportData.length}道题`);
};

const handleClose = () => {
  emit('update:modelValue', false);
};
</script>

<style scoped>
.exam-content {
  max-height: 70vh;
  overflow-y: auto;
}

/* 自定义弹窗头部样式 */
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.dialog-title {
  font-size: 18px;
  font-weight: bold;
}

/* 导出按钮样式 */
.export-btn {
  padding: 6px 12px;
}
</style>
