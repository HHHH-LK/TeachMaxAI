<template>
  <el-dialog 
    v-model="visible"
    title="学生答题详情" 
    width="900px"
    append-to-body
  >
    <div v-if="submission" class="submission-detail">
      <div class="student-info">
        <h4>
          {{ submission.studentName }} ({{ submission.studentId }})
        </h4>
        <p>提交时间: {{ submission.submitTime }}</p>
      </div>
      <ExamAttempt :paper="paper" :readonly="true" :user-answers="userAnswers" />
      <div class="detail-actions">
        <el-button type="primary" @click="$emit('save', submission)">
          保存评分
        </el-button>
        <el-button type="success" @click="$emit('ai-grade', submission)">
          智能阅卷
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue';
import ExamAttempt from '@/components/ExamAttempt.vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  submission: {
    type: Object,
    default: null
  },
  assessment: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update:modelValue', 'save', 'ai-grade', 'ai-grade-question']);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

// 构造ExamAttempt所需的paper结构
const paper = computed(() => {
  if (!props.submission || !props.submission.answers) return { title: '', questions: [] };
  return {
    title: props.assessment?.title || '试卷',
    questions: props.submission.answers.map((a, idx) => {
      // 题型映射
      let type = '';
      if (a.type.includes('单选')) type = 'single';
      else if (a.type.includes('多选')) type = 'multiple';
      else if (a.type.includes('判断')) type = 'judge';
      else if (a.type.includes('简答')) type = 'short';
      else if (a.type.includes('填空')) type = 'blank';
      else type = 'unknown';
      return {
        id: idx + 1,
        title: a.question,
        type,
        score: a.score,
        options: a.options || [],
        correctAnswer: a.correctAnswer,
        explanation: a.explanation
      };
    })
  };
});

// 构造userAnswers用于ExamAttempt回显
const userAnswers = computed(() => {
  if (!props.submission || !props.submission.answers) return {};
  const result = {};
  props.submission.answers.forEach((a, idx) => {
    result[idx + 1] = a.studentAnswer;
  });
  return result;
});
</script>

<style scoped>
.submission-detail {
  padding: 20px 0;
}

.student-info {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.student-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
}

.student-info p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.answers-section {
  margin-bottom: 24px;
}

.answer-item {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.question-number {
  font-weight: 600;
  color: #303133;
}

.question-type {
  background-color: #e6f7ff;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.question-score {
  color: #909399;
  font-size: 14px;
}

.question-content p {
  margin: 8px 0;
  line-height: 1.6;
}

.ai-grading {
  margin-top: 12px;
  padding: 12px;
  background-color: #f6ffed;
  border-radius: 6px;
  border-left: 4px solid #52c41a;
}

.manual-grading {
  margin-top: 12px;
  padding: 12px;
  background-color: #fff7e6;
  border-radius: 6px;
  border-left: 4px solid #faad14;
}

.detail-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style> 