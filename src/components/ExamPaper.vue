<template>
  <div class="exam-paper-container">
    <div v-if="questions.length === 0" class="no-questions">
      <p>正在生成中</p>
    </div>
    <div v-else>
      <div v-for="(question, index) in questions" :key="index" class="question-card">
        <div class="question-header">
          <span class="question-number">{{ index + 1 }}.</span>
          <span class="question-type">[{{ getQuestionTypeLabel(question.type) }}]</span>
          <span class="question-text">{{ question.text }}</span>
        </div>

        <div class="question-body">
          <!-- 单选 -->
          <div v-if="question.type === 'single-choice'" class="options-list">
            <label
                v-for="(option, oIndex) in question.options"
                :key="oIndex"
                :class="['option-item', submitted ? getOptionClass(option.value, question.answer, displayUserAnswers[index]) : '', {'selected': displayUserAnswers[index] === option.value}]"
            >
              <input
                  type="radio"
                  :name="'question-' + index"
                  :value="option.value"
                  v-model="localUserAnswers[index]"
                  :disabled="isReadonly || submitted"
              />
              {{ option.label }}. {{ option.content }}
            </label>
          </div>


          <!-- 多选 -->
          <div v-else-if="question.type === 'multiple-choice'" class="options-list">
            <label
                v-for="(option, oIndex) in question.options"
                :key="oIndex"
                :class="['option-item', submitted ? getOptionClass(option.value, question.answer, displayUserAnswers[index]) : '', {'selected': Array.isArray(displayUserAnswers[index]) && displayUserAnswers[index].includes(option.value)}]"
            >
              <input
                  type="checkbox"
                  :name="'question-' + index + '-multiple'"
                  :value="option.value"
                  v-model="localUserAnswers[index]"
                  :disabled="isReadonly || submitted"
              />
              {{ option.label }}. {{ option.content }}
            </label>
          </div>

          <!-- 填空 -->
          <div v-else-if="question.type === 'fill-in-blank'">
            <input
                type="text"
                v-model="localUserAnswers[index]"
                placeholder="请填写答案"
                class="fill-in-input"
                :disabled="isReadonly || submitted"
            />
          </div>

          <!-- 判断 -->
          <div v-else-if="question.type === 'true-false'" class="options-list">
            <label
                v-for="(option, oIndex) in question.options"
                :key="oIndex"
                :class="['option-item', submitted ? getOptionClass(option.value, question.answer, displayUserAnswers[index]) : '', {'selected': displayUserAnswers[index] === option.value}]"
            >
              <input
                  type="radio"
                  :name="'question-tf-' + index"
                  :value="option.value"
                  v-model="localUserAnswers[index]"
                  :disabled="isReadonly || submitted"
              />
              {{ option.label }}. {{ option.content }}
            </label>
          </div>

          <!-- 简答 -->
          <div v-else-if="question.type === 'short-answer'">
            <textarea
                v-model="localUserAnswers[index]"
                placeholder="请在此处作答"
                class="short-answer-textarea"
                :disabled="isReadonly || submitted"
            ></textarea>
          </div>
        </div>

        <!-- 答案与解析区域 -->
        <div v-if="isReadonly || submitted" class="answer-section">
          <p class="your-answer" :class="isCorrect(question, displayUserAnswers[index]) ? 'correct' : 'wrong'">
            你选择的答案：{{ formatUserAnswer(displayUserAnswers[index], question.type) }}
            <span v-if="isCorrect(question, displayUserAnswers[index])">（正确）</span>
            <span v-else-if="displayUserAnswers[index] && (typeof displayUserAnswers[index] === 'string' && displayUserAnswers[index].trim() !== '') || (Array.isArray(displayUserAnswers[index]) && displayUserAnswers[index].length > 0)">（错误）</span>
            <span v-else>（未作答）</span>
          </p>
          <p class="correct-answer">正确答案: {{ formatAnswer(question.answer, question.type) }}</p>
          <p v-if="question.explanation" class="explanation">解析: {{ question.explanation }}</p>
        </div>
      </div>

      <div class="exam-actions" v-if="!isReadonly">
        <button @click="submitExam" :disabled="submitted" class="submit-btn">提交试卷</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { studentService } from '@/services/api';
import { ref, onMounted, watch, computed } from 'vue';

const props = defineProps({
  examContent: {
    type: String,
    default: ''
  },
  chapterId: {
    type: String,
    default: ''
  },
  courseId: {
    type: String,
    default: ''
  },
  minIds: {
    type: String,
    default:''
  },
  maxIds: {
    type: String,
    default: ''
  },
  readonly: {
    type: Boolean,
    default: false
  },
  userAnswers: {
    type: Array,
    default: undefined
  },
  userInfo: {
    type: String,
    default: 'practice'
  }
});


const questions = ref([]);
const localUserAnswers = ref([]);
const submitted = ref(false);

const isReadonly = computed(() => props.readonly);

// 只读模式下优先用外部userAnswers
const displayUserAnswers = computed(() => {
  if (isReadonly.value && Array.isArray(props.userAnswers) && props.userAnswers.length > 0) {
    return props.userAnswers;
  }
  return localUserAnswers.value;
});

// 解析考试内容
const parseExamContent = (content) => {
  if (!content) return [];
  
  const lines = content.split('\n').filter(line => line.trim() !== '');
  
  const parsedQuestions = [];
  let currentQuestion = null;

  lines.forEach((line, lineIndex) => {
    const trimmedLine = line.trim();

    // 匹配题目行（支持单/多选、填空、判断和简答）
    const questionMatch = trimmedLine.match(/^(\d+)\.\s*\[(single-choice|multiple-choice|fill-in-blank|true-false|short-answer)\]\s*(.*)/);

    if (questionMatch) {
      // 如果有当前题目，先保存
      if (currentQuestion) parsedQuestions.push(currentQuestion);

      // 创建新题目
      currentQuestion = {
        type: questionMatch[2],
        text: questionMatch[3],
        options: [],
        answer: '',
        explanation: ''
      };

      // 为判断题添加默认选项
      if (questionMatch[2] === 'true-false') {
        currentQuestion.options = [
          { label: 'A', content: '正确', value: 'true' },
          { label: 'B', content: '错误', value: 'false' }
        ];
      }
    }
    // 匹配选项行（支持A-D）
    else if (currentQuestion && /^[A-D]\.\s+.+$/.test(trimmedLine)) {
      const optionMatch = trimmedLine.match(/^([A-D])\.\s*(.*)/);
      if (optionMatch) {
        // 如果是单选或多选
        if (currentQuestion.type === 'single-choice' || currentQuestion.type === 'multiple-choice') {
          // 过滤掉 [object Object] 这样的无效内容
          const optionContent = optionMatch[2].trim();
          if (optionContent && !optionContent.includes('[object Object]')) {
            currentQuestion.options.push({
              label: optionMatch[1],
              content: optionContent,
              value: optionMatch[1] // 设置value
            });
          } else {
            console.warn('跳过无效选项:', optionContent);
          }
        }
      }
    }
    // 匹配答案行（兼容两种前缀）
    else if (currentQuestion && /^(正确答案|答案|Answer):\s*/.test(trimmedLine)) {
      const answerText = trimmedLine.replace(/^(正确答案|答案|Answer):\s*/, '');
      // 判断题答案转换
      if (currentQuestion.type === 'true-false') {
        currentQuestion.answer = answerText.toUpperCase() === 'A' ? 'true' : 'false';
      } else {
        currentQuestion.answer = answerText;
      }
    }
    // 匹配解析行
    else if (currentQuestion && trimmedLine.startsWith('解析: ')) {
      const explanationText = trimmedLine.replace('解析: ', '');
      currentQuestion.explanation = explanationText;
    }
  });

  // 添加最后一个问题
  if (currentQuestion) parsedQuestions.push(currentQuestion);

  // 确保所有单选题和多选题选项都有value
  parsedQuestions.forEach(q => {
    if (q.type === 'single-choice' || q.type === 'multiple-choice') {
      q.options.forEach(opt => {
        if (!opt.value) {
          opt.value = opt.label;
        }
      });
    }
  });

  // 初始化用户答案数组 - 修复：为多选题初始化为空数组
  localUserAnswers.value = parsedQuestions.map(q =>
      q.type === 'multiple-choice' ? [] : ''
  );

  return parsedQuestions;
};

// 获取题目类型中文标签
const getQuestionTypeLabel = (type) => {
  const typeLabels = {
    'single-choice': '单选题',
    'multiple-choice': '多选题',
    'fill-in-blank': '填空题',
    'true-false': '判断题',
    'short-answer': '简答题'
  };
  return typeLabels[type] || type;
};

// 获取选项样式
const getOptionClass = (optionValue, correctAnswer, userAnswer) => {
  // 检查学生是否作答：undefined、null、空字符串、空数组都视为未作答
  if (userAnswer === undefined || userAnswer === null || 
      (typeof userAnswer === 'string' && userAnswer.trim() === '') ||
      (Array.isArray(userAnswer) && userAnswer.length === 0)) {
    return '';
  }

  // 判断题和单选题直接比较
  if (typeof userAnswer === 'string') {
    if (optionValue === correctAnswer) return 'correct-option';
    if (optionValue === userAnswer && userAnswer !== correctAnswer) return 'wrong-option';
  }
  // 多选题：多个答案
  else if (Array.isArray(userAnswer)) {
    if (correctAnswer.includes(optionValue)) {
      return userAnswer.includes(optionValue) ? 'correct-option' : 'missing-option';
    } else {
      return userAnswer.includes(optionValue) ? 'wrong-option' : '';
    }
  }
  return '';
};

// 检查答案是否正确
const isCorrect = (question, userAnswer) => {
  // 检查学生是否作答：undefined、null、空字符串、空数组都视为未作答
  if (userAnswer === undefined || userAnswer === null || 
      (typeof userAnswer === 'string' && userAnswer.trim() === '') ||
      (Array.isArray(userAnswer) && userAnswer.length === 0)) {
    return false;
  }

  const correctAnswer = question.answer;
  switch (question.type) {
    case 'single-choice':
    case 'fill-in-blank':
    case 'true-false':
      return userAnswer === correctAnswer;

    case 'multiple-choice':
      // 多选题：确保每个选项都正确（假设正确答案是逗号分隔的字符串，如"A,B"）
      const correctArray = correctAnswer.split(',').map(a => a.trim());
      return Array.isArray(userAnswer)
          ? userAnswer.sort().toString() === correctArray.sort().toString()
          : false;

    case 'short-answer':
      // 简答题暂时不判对错
      return false;

    default:
      return false;
  }
};

// 格式化用户答案
const formatUserAnswer = (userAnswer, type) => {
  // 检查学生是否作答：undefined、null、空字符串、空数组都视为未作答
  if (userAnswer === undefined || userAnswer === null || 
      (typeof userAnswer === 'string' && userAnswer.trim() === '') ||
      (Array.isArray(userAnswer) && userAnswer.length === 0)) {
    return '未作答';
  }

  if (type === 'multiple-choice' && Array.isArray(userAnswer)) {
    return userAnswer.join(', ');
  }

  if (type === 'true-false') {
    if(userAnswer === 'true') return '正确';
    else if(userAnswer === 'false') return '错误';
    else return '未作答';
  }

  return userAnswer;
};

// 格式化正确答案
const formatAnswer = (answer, type) => {
  if (!answer) return '';

  if (type === 'multiple-choice') {
    return answer.split(',').map(a => a.trim()).join(', ');
  }

  if (type === 'true-false') {
    return answer === 'true' ? '正确' : '错误';
  }
  return answer;
};

// 提交试卷
const submitExam = async () => {
  submitted.value = true;

  // 修改1：创建单个对象而不是数组
  const payload = {
    chapterId: props.chapterId,
    type: props.userInfo,
    courseId: props.courseId,
    // 初始化答案列表
    studentAnswerDTOList: []
  };

  console.log("提交的payload:", payload)

  const minId = parseInt(props.minIds) || 0;

  questions.value.forEach((question, index) => {
    const questionId = minId + index;

    let formattedAnswer = "";
    if (question.type === "multiple-choice") {
      formattedAnswer = Array.isArray(localUserAnswers.value[index])
          ? localUserAnswers.value[index].join(",")
          : "";
      //
    } else if (question.type === "true-false") {
      formattedAnswer = localUserAnswers.value[index] === "true" ? "A" : "B";
    } else {
      formattedAnswer = localUserAnswers.value[index] || "";
    }

    // 修改2：直接添加到 studentAnswerDTOList
    payload.studentAnswerDTOList.push({
      questionId: questionId.toString(),
      studentAnswer: formattedAnswer || "无",
    });
  });

  console.log("提交的payload:", payload); // 现在是个对象

  try {
    // 发送单个对象
    const response = await studentService.submitStudentAnswer(payload);
    console.log("提交成功并判题成1", response);
  } catch (error) {
    console.error("提交失败", error);
  }
};

// 监听examContent变化以解析题目
watch(() => props.examContent, (newContent) => {
  if (newContent) {
    const parsed = parseExamContent(newContent);
    questions.value = parsed;
  } else {
    questions.value = [];
  }
}, { immediate: true });
</script>
<style scoped>
.exam-paper-container {
  background-color: #f8f9fa;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 6px 24px rgba(37, 99, 235, 0.08);
  margin-top: 30px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

.no-questions {
  text-align: center;
  color: #666;
  font-size: 18px;
  padding: 50px 0;
}

.question-card {
  background-color: #fff;
  border: 1.5px solid #dbeafe;
  border-radius: 12px;
  padding: 28px 28px 18px 28px;
  margin-bottom: 28px;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.04);
  transition: box-shadow 0.2s;
}

.question-card:hover {
  box-shadow: 0 6px 24px rgba(37, 99, 235, 0.10);
}

.question-header {
  margin-bottom: 18px;
  line-height: 1.7;
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-number {
  font-weight: bold;
  color: #2563eb;
  font-size: 1.2em;
}

.question-type {
  color: #fff;
  background: #2563eb;
  border-radius: 6px;
  padding: 2px 10px;
  font-size: 0.95em;
  font-weight: 500;
  margin-right: 8px;
}

.question-text {
  font-size: 1.1em;
  color: #1e293b;
  font-weight: 600;
}

.question-body {
  margin-bottom: 18px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  font-size: 1.08em;
  color: #374151;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 10px 16px;
  cursor: pointer;
  border: 1.5px solid transparent;
  transition: background 0.2s, border 0.2s, color 0.2s;
  margin-bottom: 0;
}

.option-item.selected {
  background: #dbeafe;
  border-color: #2563eb;
  color: #2563eb;
}

.option-item input[type="radio"],
.option-item input[type="checkbox"] {
  margin-right: 12px;
  accent-color: #2563eb;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 提交后高亮正确/错误选项 */
.option-correct {
  background: #e0fce0 !important;
  border-color: #22c55e !important;
  color: #15803d !important;
}
.option-wrong {
  background: #fee2e2 !important;
  border-color: #ef4444 !important;
  color: #b91c1c !important;
}
.option-miss {
  background: #fef9c3 !important;
  border-color: #facc15 !important;
  color: #b45309 !important;
}

.fill-in-input,
.short-answer-textarea {
  width: 100%;
  padding: 12px;
  border: 1.5px solid #cbd5e1;
  border-radius: 6px;
  font-size: 1.08em;
  min-height: 40px;
  resize: vertical;
  box-sizing: border-box;
  background: #f8fafc;
  margin-bottom: 0;
}

.short-answer-textarea {
  min-height: 100px;
}

.answer-section {
  background-color: #f0fdfb;
  border-left: 5px solid #2563eb;
  padding: 16px 22px;
  border-radius: 8px;
  margin-top: 18px;
}

.your-answer {
  font-weight: bold;
  font-size: 1.08em;
  margin-bottom: 8px;
}
.your-answer.correct {
  color: #22c55e;
}
.your-answer.wrong {
  color: #ef4444;
}

.correct-answer {
  font-weight: bold;
  color: #2563eb;
  margin-bottom: 8px;
}

.explanation {
  color: #555;
  font-size: 0.98em;
}

.exam-actions {
  text-align: center;
  margin-top: 36px;
}

.submit-btn {
  background: linear-gradient(to right, #2563eb, #3b82f6);
  color: white;
  border: none;
  padding: 14px 38px;
  border-radius: 8px;
  font-size: 1.18em;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.3s, transform 0.2s;
  margin: 0 10px;
}
.submit-btn:hover {
  background: linear-gradient(to right, #1d4ed8, #2563eb);
  transform: translateY(-2px);
}
.submit-btn:disabled {
  background: #a0c8f8;
  cursor: not-allowed;
  transform: none;
}
</style>