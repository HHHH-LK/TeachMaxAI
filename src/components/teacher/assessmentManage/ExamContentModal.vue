<template>
  <el-dialog
    v-model="visible"
    title="考试内容"
    width="900px"
    :before-close="handleClose"
  >
    <div v-if="examPaper" class="exam-content">
      <ExamAttempt 
        :paper="examPaper" 
        :readonly="true" 
        :wrong-analysis="correctAnswers"
        :user-answers="{}"
        :show-user-answers="false"
      />
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
  examPaper: {
    type: Object,
    default: null
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

const handleClose = () => {
  emit('update:modelValue', false);
};
</script>

<style scoped>
.exam-content {
  max-height: 70vh;
  overflow-y: auto;
}
</style> 