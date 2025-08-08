<template>
  <div class="exam-attempt">
    <h2 class="exam-title">{{ paper.title }}</h2>
    <div class="exam-info">
      <span>总分：{{ totalScore }}分</span>
      <span>题目数：{{ paper.questions.length }}</span>
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
      <!-- 新增：只读时显示学生答案和正确答案 -->
      <div v-if="readonly && showUserAnswers" class="answer-compare">
        <div>学生答案：{{ formatUserAnswer(userAnswers[q.id], q) }}</div>
        <div v-if="wrongAnalysis && wrongAnalysis[q.id] && wrongAnalysis[q.id].answer">
          正确答案：{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}
        </div>
      </div>
      <!-- 只显示正确答案 -->
      <div v-if="readonly && !showUserAnswers && wrongAnalysis && wrongAnalysis[q.id] && wrongAnalysis[q.id].answer" class="answer-compare">
        <div>正确答案：{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}</div>
      </div>
      <!-- 错题解析（只读且有解析时显示） -->
      <div v-if="readonly && wrongAnalysis && wrongAnalysis[q.id]" class="wrong-analysis">
        <div class="analysis-title">题目解析：</div>
        <div class="analysis-content">
          <div v-if="wrongAnalysis[q.id].explanation">{{ wrongAnalysis[q.id].explanation }}</div>
          <div v-if="wrongAnalysis[q.id].answer">正确答案：{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}</div>
        </div>
      </div>
    </div>
    <div v-if="!readonly" class="exam-actions">
      <el-button type="primary" @click="submitPaper">提交试卷</el-button>
    </div>
    <div v-else class="readonly-tip">本试卷为只读模式，仅供查看。</div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';

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
  }
});

const emit = defineEmits(['submit']);

// 用户答案
const userAnswers = ref({});

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

function submitPaper() {
  // 只提交答案，不显示正确答案
  emit('submit', { answers: { ...userAnswers.value } });
  ElMessage.success('试卷已提交！');
}

function formatUserAnswer(val, q) {
  if (q.type === 'multiple' && Array.isArray(val)) {
    return val.join(', ');
  }
  if (q.type === 'judge') {
    if (val === true || val === 'true') return '正确';
    if (val === false || val === 'false') return '错误';
  }
  return val !== undefined && val !== null ? val : '未作答';
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
    // 如果是字符串，按逗号分割
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
.wrong-analysis {
  background: #fff7e6;
  border-left: 4px solid #faad14;
  padding: 10px 16px;
  border-radius: 4px;
  margin-top: 8px;
  color: #ad6800;
}
.exam-actions {
  text-align: center;
  margin-top: 32px;
}
.readonly-tip {
  color: #888;
  text-align: center;
  margin-top: 32px;
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