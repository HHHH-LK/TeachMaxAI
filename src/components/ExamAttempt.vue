<template>
  <div class="exam-attempt">
    <h2 class="exam-title">{{ paper.title }}</h2>
    <div class="exam-info">
      <span>总分：{{ totalScore }}分</span>
      <span>题目数：{{ paper.questions.length }}</span>
      <span v-if="readonly && showUserAnswers" class="submitted-info">（已提交作答）</span>
    </div>
    <div v-for="(q, idx) in paper.questions" :key="q.id" class="question-block">
      <div class="question-header">
        <span class="q-index">{{ idx + 1 }}.</span>
        <span class="q-title">{{ q.title }}</span>
        <span class="q-score">（{{ q.score }}分）</span>
        <span class="q-type">[{{ getTypeText(q.type) }}]</span>
      </div>
      <div class="question-body">
        <!-- 单选题 -->
        <template v-if="q.type === 'single'">
          <el-radio-group v-model="userAnswers[q.id]" :disabled="readonly">
            <el-radio v-for="opt in q.options" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
          </el-radio-group>
        </template>
        <!-- 多选题 -->
        <template v-else-if="q.type === 'multiple'">
          <el-checkbox-group v-model="userAnswers[q.id]" :disabled="readonly">
            <el-checkbox v-for="opt in q.options" :key="opt.value" :value="opt.value">{{ opt.label }}</el-checkbox>
          </el-checkbox-group>
        </template>
        <!-- 判断题 -->
        <template v-else-if="q.type === 'judge'">
          <el-radio-group v-model="userAnswers[q.id]" :disabled="readonly">
            <el-radio :value="true">正确</el-radio>
            <el-radio :value="false">错误</el-radio>
          </el-radio-group>
        </template>
        <!-- 简答题 -->
        <template v-else-if="q.type === 'short'">
          <el-input type="textarea" v-model="userAnswers[q.id]" :disabled="readonly" :rows="3" />
        </template>
        <!-- 填空题 -->
        <template v-else-if="q.type === 'blank'">
          <el-input v-model="userAnswers[q.id]" :disabled="readonly" placeholder="请填写答案" />
        </template>
      </div>

      <!-- 已提交答案对比显示 -->
      <div v-if="readonly && showUserAnswers" class="answer-compare">
        <div class="student-answer">
          <strong>我的答案：</strong>{{ formatUserAnswer(userAnswers[q.id], q) }}
        </div>
        <div v-if="wrongAnalysis && wrongAnalysis[q.id] && wrongAnalysis[q.id].answer" class="correct-answer">
          <strong>正确答案：</strong>{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}
        </div>
        <div v-if="isAnswerCorrect(q.id)" class="answer-status correct">✓ 答案正确</div>
        <div v-else class="answer-status incorrect">✗ 答案错误</div>
      </div>

      <!-- 只显示正确答案（当没有用户答案时） -->
      <div v-else-if="readonly && !showUserAnswers && wrongAnalysis && wrongAnalysis[q.id] && wrongAnalysis[q.id].answer" class="answer-compare">
        <div class="correct-answer">
          <strong>正确答案：</strong>{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}
        </div>
      </div>

      <!-- 题目解析 -->
      <div v-if="readonly && wrongAnalysis && wrongAnalysis[q.id] && wrongAnalysis[q.id].explanation" class="wrong-analysis">
        <div class="analysis-title">题目解析：</div>
        <div class="analysis-content">
          {{ wrongAnalysis[q.id].explanation }}
        </div>
      </div>
    </div>

    <div v-if="!readonly" class="exam-actions">
      <el-button type="primary" @click="submitPaper" :loading="submitting">
        {{ submitting ? '提交中...' : '提交试卷' }}
      </el-button>
    </div>

    <div v-else class="readonly-tip">
      {{ showUserAnswers ? '本试卷已提交，以下为您的作答情况。' : '本试卷为只读模式，仅供查看。' }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

// props: paper(试卷对象), readonly(是否只读), wrongAnalysis(错题解析对象)
const props = defineProps({
  paper: {
    type: Object,
    required: true
  },
  readonly: {
    type: Boolean,
    default: false
  },
  wrongAnalysis: {
    type: Object,
    default: () => ({})
  },
  userAnswers: {
    type: Object,
    default: undefined
  },
  showUserAnswers: {
    type: Boolean,
    default: true
  },
  examId: {
    type: [String, Number],
    default: null
  }
});

const emit = defineEmits(['submit']);

console.log('props paper', props.paper)
console.log('props readonly', props.readonly)
console.log('props wrongAnalysis', props.wrongAnalysis)
console.log('props userAnswers', props.userAnswers)
console.log('props showUserAnswers', props.showUserAnswers)
console.log('props examId', props.examId)

// 用户答案
const userAnswers = ref({});
const submitting = ref(false);

// 优先使用外部传入的userAnswers
watch(
    [() => props.paper, () => props.userAnswers, () => props.readonly],
    ([paper, extUserAnswers, readonly]) => {
      if (readonly && extUserAnswers && Object.keys(extUserAnswers).length > 0) {
        userAnswers.value = { ...extUserAnswers };
      } else if (readonly && paper && paper.userAnswers) {
        userAnswers.value = { ...paper.userAnswers };
      } else {
        userAnswers.value = {};
      }
    },
    { immediate: true }
);

const totalScore = computed(() => {
  return props.paper.questions.reduce((sum, q) => sum + (q.score || 0), 0);
});

function getTypeText(type) {
  switch (type) {
    case 'single': return '单选';
    case 'multiple': return '多选';
    case 'judge': return '判断';
    case 'short': return '简答';
    case 'blank': return '填空';
    default: return '未知';
  }
}

// 判断答案是否正确
function isAnswerCorrect(questionId) {
  const userAnswer = userAnswers.value[questionId];
  const correctAnswer = props.wrongAnalysis[questionId]?.answer;

  if (!correctAnswer || userAnswer === undefined || userAnswer === null) {
    return false;
  }

  // 获取题目类型
  const question = props.paper.questions.find(q => q.id === questionId);
  if (!question) return false;

  switch (question.type) {
    case 'multiple':
      // 多选题比较
      const userAns = Array.isArray(userAnswer) ? userAnswer : [userAnswer];
      let correctAns;

      if (Array.isArray(correctAnswer)) {
        correctAns = correctAnswer;
      } else if (typeof correctAnswer === 'string') {
        // 处理字符串格式，可能是 ["A", "B", "C"] 或 "A,B,C"
        if (correctAnswer.startsWith('[') && correctAnswer.endsWith(']')) {
          try {
            correctAns = JSON.parse(correctAnswer);
          } catch (e) {
            correctAns = correctAnswer.slice(1, -1).split(',').map(s => s.trim().replace(/"/g, ''));
          }
        } else {
          correctAns = correctAnswer.split(',').map(s => s.trim());
        }
      } else {
        correctAns = [correctAnswer];
      }

      return userAns.length === correctAns.length &&
          userAns.every(ans => correctAns.includes(ans));

    case 'judge':
      // 判断题比较
      const userBool = userAnswer === true || userAnswer === 'true' || userAnswer === '正确';
      const correctBool = correctAnswer === true || correctAnswer === 'true' || correctAnswer === '正确';
      return userBool === correctBool;

    default:
      // 其他题型直接字符串比较
      return String(userAnswer).trim() === String(correctAnswer).trim();
  }
}

// 格式化用户答案为字符串，用于提交给后端
function formatAnswerForSubmission(answer, questionType) {
  if (answer === undefined || answer === null) {
    return '';
  }

  switch (questionType) {
    case 'multiple':
      // 多选题：数组转换为逗号分隔的字符串
      return Array.isArray(answer) ? answer.join(',') : String(answer);
    case 'judge':
      // 判断题：布尔值转换为字符串
      return String(answer);
    case 'single':
    case 'short':
    case 'blank':
    default:
      // 单选、简答、填空题直接转换为字符串
      return String(answer);
  }
}

// 检查答案完整性
function validateAnswers() {
  const unansweredQuestions = [];

  props.paper.questions.forEach((question, index) => {
    const answer = userAnswers.value[question.id];

    // 检查是否有答案
    if (answer === undefined || answer === null || answer === '' ||
        (Array.isArray(answer) && answer.length === 0)) {
      unansweredQuestions.push(index + 1);
    }
  });

  return unansweredQuestions;
}

async function submitPaper() {
  try {
    // 检查答案完整性
    const unansweredQuestions = validateAnswers();

    if (unansweredQuestions.length > 0) {
      const result = await ElMessageBox.confirm(
          `第 ${unansweredQuestions.join(', ')} 题尚未作答，确定要提交吗？`,
          '提示',
          {
            confirmButtonText: '确定提交',
            cancelButtonText: '继续答题',
            type: 'warning',
          }
      );

      if (!result) {
        return;
      }
    }

    // 再次确认提交
    await ElMessageBox.confirm(
        '确定要提交试卷吗？提交后无法修改答案。',
        '确认提交',
        {
          confirmButtonText: '确定提交',
          cancelButtonText: '取消',
          type: 'warning',
        }
    );

    submitting.value = true;

    // 构建提交数据
    const studentExamAnswerDTOList = props.paper.questions.map(question => {
      // 从question.id中提取questionId（去掉前缀'q'）
      const questionId = question.id.replace('q', '');
      const answer = userAnswers.value[question.id];

      return {
        questionId: parseInt(questionId),
        studentAnswer: formatAnswerForSubmission(answer, question.type)
      };
    });

    const submitData = {
      examId: props.examId || '',
      studentExamAnswerDTOList: studentExamAnswerDTOList
    };

    console.log('提交的数据:', submitData);

    // 发送给父组件处理实际的API调用
    emit('submit', {
      answers: { ...userAnswers.value },
      submitData: submitData
    });

  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交试卷失败:', error);
      ElMessage.error('提交失败，请重试');
    }
  } finally {
    submitting.value = false;
  }
}

function formatUserAnswer(val, q) {
  if (val === undefined || val === null) {
    return '未作答';
  }

  if (q.type === 'multiple' && Array.isArray(val)) {
    return val.length > 0 ? val.join(', ') : '未作答';
  }
  if (q.type === 'judge') {
    if (val === true || val === 'true') return '正确';
    if (val === false || val === 'false') return '错误';
  }
  return val !== '' ? val : '未作答';
}

function formatCorrectAnswer(val, q) {
  if (!val) return '';

  // 确保val是字符串类型
  const strVal = String(val);

  if (q.type === 'multiple') {
    // 如果是数组，直接join
    if (Array.isArray(val)) {
      return val.join(', ');
    }
    // 如果是字符串，处理JSON数组格式或逗号分隔格式
    if (strVal.startsWith('[') && strVal.endsWith(']')) {
      try {
        const parsed = JSON.parse(strVal);
        return Array.isArray(parsed) ? parsed.join(', ') : strVal;
      } catch (e) {
        // JSON解析失败，尝试手动解析
        return strVal.slice(1, -1).split(',').map(s => s.trim().replace(/"/g, '')).join(', ');
      }
    }
    // 按逗号分割
    return strVal.split(',').map(a => a.trim()).join(', ');
  }
  if (q.type === 'judge') {
    if (val === true || val === 'true') return '正确';
    if (val === false || val === 'false') return '错误';
    if (val === '正确' || val === '错误') return val;
  }
  return strVal;
}
</script>

<style scoped>
.exam-attempt {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  padding: 32px 24px;
}
.exam-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 12px;
}
.exam-info {
  color: #888;
  margin-bottom: 24px;
}
.submitted-info {
  color: #67c23a;
  font-weight: 500;
}
.question-block {
  margin-bottom: 32px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f0f0f0;
}
.question-header {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.q-score {
  color: #409eff;
}
.q-type {
  color: #aaa;
  font-size: 13px;
}
.question-body {
  margin-bottom: 8px;
}
.answer-compare {
  margin: 12px 0;
  background: #f8f9fa;
  border-radius: 6px;
  padding: 12px 16px;
  border-left: 4px solid #e9ecef;
}
.student-answer {
  margin-bottom: 8px;
  color: #495057;
}
.correct-answer {
  color: #28a745;
  margin-bottom: 8px;
}
.answer-status {
  font-weight: 500;
  font-size: 14px;
}
.answer-status.correct {
  color: #67c23a;
}
.answer-status.incorrect {
  color: #f56c6c;
}
.wrong-analysis {
  background: #fff7e6;
  border-left: 4px solid #faad14;
  padding: 12px 16px;
  border-radius: 6px;
  margin-top: 12px;
}
.analysis-title {
  font-weight: 500;
  color: #ad6800;
  margin-bottom: 8px;
}
.analysis-content {
  color: #ad6800;
  line-height: 1.5;
}
.exam-actions {
  text-align: center;
  margin-top: 32px;
}
.readonly-tip {
  color: #888;
  text-align: center;
  margin-top: 32px;
  font-style: italic;
}
.answer-compare {
  margin: 8px 0 0 0;
  color: #444;
  font-size: 15px;
  background: #f6f8fa;
  border-radius: 4px;
  padding: 8px 14px;
}
</style>