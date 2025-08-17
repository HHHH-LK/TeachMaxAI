<template>
  <div class="challenge-modal">
    <div class="modal-container" @click.stop>
      <!-- 背景粒子效果 -->
      <div class="background-particles">
        <div
          v-for="(particle, index) in particles"
          :key="index"
          class="particle"
          :style="particle.style"
        ></div>
      </div>

      <div class="modal-header">
        <h2>题目挑战</h2>
        <button class="close-button" @click="closeModal">×</button>
      </div>

      <div class="scrollable-content">
        <div class="modal-body">
          <div class="problem-container">
            <div class="problem">{{ question }}</div>

            <!-- 判断题专用界面 -->
            <div v-if="isTrueFalse" class="choice-options">
              <div
                class="option true-option"
                :class="{
                  selected: selectedOption === '正确',
                  correct: showExplanation && isAnswerCorrect('正确'),
                  incorrect: showExplanation && !isAnswerCorrect('正确'),
                }"
                @click="selectOption('正确')"
              >
                <div class="option-content">
                  <div class="option-icon">✓</div>
                  <div class="option-text">正确</div>
                </div>
              </div>
              <div
                class="option false-option"
                :class="{
                  selected: selectedOption === '错误',
                  correct: showExplanation && isAnswerCorrect('错误'),
                  incorrect: showExplanation && !isAnswerCorrect('错误'),
                }"
                @click="selectOption('错误')"
              >
                <div class="option-content">
                  <div class="option-icon">✗</div>
                  <div class="option-text">错误</div>
                </div>
              </div>
            </div>

            <!-- 其他选择题 -->
            <div v-else-if="isChoiceQuestion" class="choice-options">
              <div
                v-for="(option, index) in answers"
                :key="index"
                class="option"
                :class="{
                  selected: isSelected(option),
                  correct: showExplanation,
                  incorrect: showExplanation && !isSelected(option),
                }"
                @click="selectOption(option)"
              >
                <span v-if="isMultipleChoice" class="checkbox">
                  <span v-if="isSelected(option)" class="checkmark">✓</span>
                </span>
                {{ option }}
              </div>

              <!-- 多选题提交按钮 -->
              <div v-if="isMultipleChoice" class="multiple-choice-submit">
                <button
                  class="submit-button"
                  @click="submitAnswer"
                  :disabled="isSubmitting || selectedOptions.length === 0"
                >
                  <span class="button-text">提交答案</span>
                  <span class="button-glow"></span>
                </button>
              </div>
            </div>

            <!-- 填空题输入框 -->
            <div v-else-if="isShortAnswer" class="short-answer-input">
              <input
                type="text"
                v-model="userAnswer"
                placeholder="请输入您的答案"
                @keydown.enter="submitAnswer"
                :disabled="isSubmitting"
              />
              <button
                class="submit-button"
                @click="submitAnswer"
                :disabled="isSubmitting"
              >
                <span class="button-text">提交</span>
                <span class="button-glow"></span>
              </button>
            </div>

            <!-- 问答题输入框 -->
            <div v-else-if="isFillBank" class="answer-input">
              <textarea
                v-model="userAnswer"
                placeholder="请详细阐述您的答案"
                rows="4"
                @keydown.enter="submitAnswer"
                :disabled="isSubmitting"
              ></textarea>
              <button
                class="submit-button"
                @click="submitAnswer"
                :disabled="isSubmitting"
              >
                <span class="button-text">提交</span>
                <span class="button-glow"></span>
              </button>
            </div>
            <div v-else class="unknown-question-type">
              <p>未知题型，无法显示题目</p>
              <p>题目类型: {{ props.questionType }}</p>
            </div>
            <div class="timer">
              <span>剩余时间:</span>
              <div class="time-bar">
                <div
                  class="time-progress"
                  :style="{ width: timePercentage + '%' }"
                ></div>
              </div>
              <span>{{ timeLeft }}秒</span>
            </div>

            <!-- 判题状态提示 -->
            <div v-if="isChecking" class="checking-status">
              <div class="loading-spinner"></div>
              <div class="checking-text">正在判题中...</div>
            </div>

            <div v-else-if="feedback" :class="['feedback', feedbackClass]">
              {{ feedback }}
              <div v-if="showExplanation" class="explanation">
                <p><strong>正确答案:</strong> {{ correctAnswerDisplay }}</p>
                <p><strong>解析:</strong> {{ explanation }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { gameService } from "@/services/game";
import userConfig from "@/config/userConfig";

const props = defineProps({
  question: {
    type: String,
    required: true,
  },
  answers: {
    type: Array,
    required: true,
  },
  correctAnswers: {
    type: Array,
    required: true,
  },
  timeLimit: {
    type: Number,
    default: 120,
  },
  questionType: {
    type: String,
    validator: (value) => {
      return [
        "single_choice",
        "multiple_choice",
        "true_false",
        "fill_blank",
        "short_answer",
      ].includes(value);
    },
  },
  questionId: {
    type: Number,
    required: true,
  },
  floorId: {
    type: Number,
    required: true,
  },
  changeCount: {
    type: Number,
    default: 0,
  },
  explanation: {
    type: String,
    required: true,
  },
});

const emit = defineEmits(["close", "completed", "give-up", "damage"]);

// 用户答案
const userAnswer = ref("");
const selectedOptions = ref([]);
const selectedOption = ref("");
const feedback = ref("");
const feedbackClass = ref("");
const timeLeft = ref(props.timeLimit);
const timer = ref(null);
const isModalOpen = ref(true);
const isSubmitting = ref(false);
const showExplanation = ref(false);
const explanation = ref("");
const correctAnswerDisplay = ref("");
const damageValue = ref(0);
const isChecking = ref(false);

// 粒子效果
const particles = ref([]);
for (let i = 0; i < 30; i++) {
  particles.value.push({
    style: {
      top: `${Math.random() * 100}%`,
      left: `${Math.random() * 100}%`,
      width: `${Math.random() * 8 + 4}px`,
      height: `${Math.random() * 8 + 4}px`,
      animationDuration: `${Math.random() * 5 + 3}s`,
      animationDelay: `${Math.random() * 2}s`,
      opacity: Math.random() * 0.5 + 0.2,
    },
  });
}

// 计算属性
const isChoiceQuestion = computed(() => {
  return ["single_choice", "multiple_choice", "true_false"].includes(
    props.questionType
  );
});

const isFillBank = computed(() => {
  return props.questionType === "fill_blank";
});

const isShortAnswer = computed(() => {
  return props.questionType === "short_answer";
});

const isMultipleChoice = computed(() => {
  return props.questionType === "multiple_choice";
});

const isTrueFalse = computed(() => {
  return props.questionType === "true_false";
});

// 时间百分比
const timePercentage = computed(() => {
  return (timeLeft.value / props.timeLimit) * 100;
});

// 开始计时器
const startTimer = () => {
  clearInterval(timer.value);
  timeLeft.value = props.timeLimit;
  feedback.value = "";

  timer.value = setInterval(() => {
    timeLeft.value--;

    if (timeLeft.value <= 0) {
      clearInterval(timer.value);
      feedback.value = "时间到！挑战失败";
      feedbackClass.value = "error";
      handleChallengeCompleted(false);
    }
  }, 1000);
};

// 检查选项是否被选中
const isSelected = (option) => {
  if (isMultipleChoice.value) {
    return selectedOptions.value.includes(option);
  } else {
    return selectedOption.value === option;
  }
};

// 检查选项是否正确（用于样式）
const isAnswerCorrect = (option) => {
  if (!props.correctAnswers || props.correctAnswers.length === 0) return false;
  
  // 获取正确答案的布尔值
  const correctAnswer = props.correctAnswers[0];
  
  // 将用户选择的选项转换为布尔值
  const userChoice = option === "正确";
  
  // 比较用户选择与正确答案
  return userChoice === correctAnswer;
};

// 选择选项
const selectOption = (option) => {
  if (isSubmitting.value) return;

  if (isMultipleChoice.value) {
    const index = selectedOptions.value.indexOf(option);
    if (index === -1) {
      selectedOptions.value.push(option);
    } else {
      selectedOptions.value.splice(index, 1);
    }
  } else {
    selectedOption.value = option;

    if (
      props.questionType === "single_choice" ||
      props.questionType === "true_false"
    ) {
      submitAnswer();
    }
  }
};

// 提交答案
const submitAnswer = async () => {
  if (isSubmitting.value) return;
  isSubmitting.value = true;
  isChecking.value = true;

  // 添加3秒延迟模拟网络延迟
  // console.log("开始3秒延迟...");
  // await new Promise(resolve => setTimeout(resolve, 3000));
  // console.log("3秒延迟结束，开始处理答案");

  let userAnswerContext = "";

  if (isChoiceQuestion.value) {
    if (
      (isMultipleChoice.value && selectedOptions.value.length === 0) ||
      (!isMultipleChoice.value && !selectedOption.value)
    ) {
      feedback.value = "请选择一个选项";
      feedbackClass.value = "error";
      isSubmitting.value = false;
      isChecking.value = false;
      return;
    }

    if (isMultipleChoice.value) {
      const selectedIndexes = [];
      props.answers.forEach((answer, index) => {
        if (selectedOptions.value.includes(answer)) {
          selectedIndexes.push(index + 1);
        }
      });
      userAnswerContext = selectedIndexes.join(",");
    } else {
      if (isTrueFalse.value) {
        userAnswerContext = selectedOption.value === "正确";
      } else {
        userAnswerContext = selectedOption.value;
      }
    }
  } else if (isFillBank.value || isShortAnswer.value) {
    if (userAnswer.value.trim() === "") {
      feedback.value = "请输入答案";
      feedbackClass.value = "error";
      isSubmitting.value = false;
      isChecking.value = false;
      return;
    }

    userAnswerContext = userAnswer.value.trim();
  }

  try {
    const responseCount = await gameService.fighting.getUserChangeCount(
      userConfig.studentId,
      props.floorId
    );
    const count = responseCount.data.data;
    console.log("挑战次数", count);

    const response = await gameService.fighting.checkAnswerIsTrue(
      userConfig.studentId,
      props.questionId,
      userAnswerContext,
      props.floorId,
      count
    );

    if (response.data?.success) {
      const result = response.data.data;

      showExplanation.value = true;
      explanation.value = props.explanation;
      correctAnswerDisplay.value = formatCorrectAnswers();
      damageValue.value = result.damage;

      if (result.target === "BOSS_HP") {
        feedback.value = "✓ 答案正确！挑战成功";
        feedbackClass.value = "success";
        emit("damage", {
          target: "boss",
          damage: parseInt(result.damage),
        });
        handleChallengeCompleted(true);
      } else if (result.target === "USER_HP") {
        feedback.value = "✗ 答案不正确，挑战失败";
        feedbackClass.value = "error";
        emit("damage", {
          target: "player",
          damage: parseInt(result.damage),
        });
        handleChallengeCompleted(false);
      }
    } else {
      feedback.value = "✗ 答案验证失败，请重试";
      feedbackClass.value = "error";
    }
  } catch (error) {
    console.error("答案验证失败:", error);
    feedback.value = "✗ 答案验证失败，请重试";
    feedbackClass.value = "error";
  } finally {
    isChecking.value = false;
    isSubmitting.value = false;
  }
};

const formatCorrectAnswers = () => {
  if (!props.correctAnswers || props.correctAnswers.length === 0) {
    return "暂无正确答案";
  }

  if (props.questionType === "single_choice") {
    const answer = props.correctAnswers[0];
    if (typeof answer === "number") {
      return String.fromCharCode(65 + answer);
    }
    return answer;
  } else if (props.questionType === "multiple_choice") {
    return props.correctAnswers
      .map((answer) => {
        if (typeof answer === "number") {
          return String.fromCharCode(65 + answer);
        }
        return answer;
      })
      .join(", ");
  } else if (props.questionType === "true_false") {
    return props.correctAnswers[0] ? "正确" : "错误";
  } else {
    return props.correctAnswers;
  }
};

// 处理挑战完成事件
const handleChallengeCompleted = (success) => {
  const event = new CustomEvent("player-attack", { detail: { success } });
  document.dispatchEvent(event);

  setTimeout(() => {
    emit("completed", success);
    closeModal();
  }, 2000);
};

const closeModal = () => {
  clearInterval(timer.value);
  emit("close");
};

onMounted(() => {
  startTimer();
});

onBeforeUnmount(() => {
  clearInterval(timer.value);
});
</script>

<style scoped>
.challenge-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 20003;
  backdrop-filter: blur(10px);
}

.modal-container {
  width: 85%;
  max-width: 600px;
  background: linear-gradient(
    145deg,
    rgba(26, 26, 46, 0.1),
    rgba(22, 33, 62, 0.1)
  );
  border-radius: 20px;
  box-shadow: 0 0 40px rgba(255, 204, 0, 0.7);
  border: 3px solid #f9c80e;
  animation: challengeAppear 0.5s ease-out;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
}

@keyframes challengeAppear {
  0% {
    transform: scale(0.8);
    opacity: 0;
    filter: blur(10px);
  }
  100% {
    transform: scale(1);
    opacity: 1;
    filter: blur(0);
  }
}

.background-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.particle {
  position: absolute;
  background: rgba(249, 200, 14, 0.4);
  border-radius: 50%;
  animation: float linear infinite;
  filter: blur(1px);
}

@keyframes float {
  0% {
    transform: translateY(0) translateX(0);
  }
  50% {
    transform: translateY(-20px) translateX(10px);
  }
  100% {
    transform: translateY(0) translateX(0);
  }
}

.modal-header {
  background: linear-gradient(
    to right,
    rgba(25, 25, 45, 0.9),
    rgba(40, 30, 70, 0.8)
  );
  padding: 15px;
  border-bottom: 2px solid #f9c80e;
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.modal-header h2 {
  margin: 0;
  color: #f9c80e;
  text-align: center;
  font-size: 22px;
  text-shadow: 0 0 10px rgba(249, 200, 14, 0.7);
  letter-spacing: 1px;
  font-weight: 800;
  position: relative;
  z-index: 2;
}

.modal-header::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(249, 200, 14, 0.2),
    transparent
  );
  animation: headerGlow 3s infinite linear;
}

@keyframes headerGlow {
  0% {
    background-position: -100% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.close-button {
  position: absolute;
  top: 8px;
  right: 12px;
  background: transparent;
  border: none;
  color: #f9c80e;
  font-size: 24px;
  cursor: pointer;
  width: 35px;
  height: 35px;
  border-radius: 50%;
  transition: all 0.3s ease;
  z-index: 3;
}

.close-button:hover {
  background: rgba(255, 0, 0, 0.3);
  transform: scale(1.1);
  box-shadow: 0 0 15px rgba(255, 0, 0, 0.5);
}

.scrollable-content {
  overflow-y: auto;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 0 20px;
}

.modal-body {
  color: #e2e2e2;
  position: relative;
  z-index: 2;
  padding: 10px 0;
  flex-grow: 1;
}

.problem-container {
  padding: 10px;
  position: relative;
}

.problem {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #f9c80e;
  text-shadow: 0 0 10px rgba(249, 200, 14, 0.5);
  line-height: 1.5;
  text-align: center;
  padding: 15px;
  background: rgba(30, 30, 50, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(249, 200, 14, 0.3);
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.5);
  position: relative;
}

.problem::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    45deg,
    transparent,
    rgba(249, 200, 14, 0.1),
    transparent
  );
  animation: shine 5s infinite linear;
}

@keyframes shine {
  0% {
    background-position: -100% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.choice-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin: 20px 0;
}

.option {
  padding: 15px;
  border: 2px solid #5d4037;
  border-radius: 10px;
  background: rgba(43, 25, 18, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.option:hover {
  background: rgba(139, 0, 0, 0.3);
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(139, 0, 0, 0.4);
}

.option.selected {
  background: rgba(0, 100, 0, 0.3);
  border-color: #90ee90;
  box-shadow: 0 0 15px rgba(0, 255, 0, 0.5);
}

.checkbox {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid #f9c80e;
  border-radius: 4px;
  margin-right: 10px;
  text-align: center;
  line-height: 18px;
}

.checkmark {
  color: #f9c80e;
  font-weight: bold;
}

.option.correct {
  background: rgba(0, 128, 0, 0.3) !important;
  border-color: #90ee90 !important;
}

.option.incorrect {
  background: rgba(255, 0, 0, 0.3) !important;
  border-color: #ff416c !important;
}

/* 判断题选项共用普通选项的样式 */
.true-option,
.false-option {
  padding: 15px;
  border: 2px solid #5d4037;
  border-radius: 10px;
  background: rgba(43, 25, 18, 0.5);
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

.true-option:hover,
.false-option:hover {
  background: rgba(139, 0, 0, 0.3);
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(139, 0, 0, 0.4);
}

/* 确保选中的选项总是绿色 */
.true-option.selected,
.false-option.selected {
  background: rgba(0, 100, 0, 0.3);
  border-color: #90ee90;
  box-shadow: 0 0 15px rgba(0, 255, 0, 0.5);
}

/* 修复样式优先级 */
.true-option.selected.correct,
.false-option.selected.correct {
  background: rgba(0, 128, 0, 0.4) !important;
  border-color: #90ee90 !important;
  box-shadow: 0 0 25px rgba(0, 255, 0, 0.7) !important;
}

/* 选中的错误选项也保持绿色 */
.true-option.selected.incorrect,
.false-option.selected.incorrect {
  background: rgba(0, 100, 0, 0.3) !important;
  border-color: #90ee90 !important;
  box-shadow: 0 0 15px rgba(0, 255, 0, 0.5) !important;
}

/* 未选中的错误选项显示红色 */
.true-option.incorrect:not(.selected),
.false-option.incorrect:not(.selected) {
  background: rgba(255, 0, 0, 0.3) !important;
  border-color: #ff416c !important;
}

/* 判断题图标样式 */
.option-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.true-option .option-icon {
  color: #90ee90;
}

.false-option .option-icon {
  color: #ff416c;
}

/* 判断题文字样式 */
.option-text {
  font-size: 22px;
  font-weight: bold;
}

.explanation {
  margin-top: 15px;
  padding: 10px;
  background: rgba(30, 30, 50, 0.5);
  border-radius: 8px;
  border-left: 3px solid #f9c80e;
}

.explanation p {
  margin: 5px 0;
  line-height: 1.4;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

textarea:disabled {
  background: rgba(50, 50, 70, 0.3);
}

.short-answer-input {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.short-answer-input input {
  flex: 1;
  padding: 12px 15px;
  border: 2px solid #6a11cb;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 16px;
  transition: all 0.3s ease;
}

.short-answer-input input:focus {
  outline: none;
  border-color: #2575fc;
  box-shadow: 0 0 10px rgba(37, 117, 252, 0.5);
}

.answer-input {
  margin-top: 20px;
}

.answer-input textarea {
  width: 100%;
  padding: 12px 15px;
  border: 2px solid #6a11cb;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 16px;
  transition: all 0.3s ease;
  resize: vertical;
  min-height: 100px;
}

.answer-input textarea:focus {
  outline: none;
  border-color: #2575fc;
  box-shadow: 0 0 10px rgba(37, 117, 252, 0.5);
}

.submit-button {
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(to right, #38ef7d, #11998e);
  border: none;
  padding: 0;
  font-size: 16px;
  color: white;
  border-radius: 10px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  height: 45px;
  width: 100px;
}

.button-text {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  position: relative;
  z-index: 2;
  line-height: 1;
}

.button-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.5),
    transparent
  );
  animation: shine 3s infinite linear;
  z-index: 1;
}

.submit-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(56, 239, 125, 0.6);
}

.timer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  background: rgba(30, 30, 50, 0.6);
  padding: 12px;
  border-radius: 10px;
  border: 1px solid rgba(249, 200, 14, 0.2);
}

.time-bar {
  width: 100%;
  height: 12px;
  background: rgba(50, 50, 70, 0.7);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.5);
}

.time-progress {
  height: 100%;
  background: linear-gradient(to right, #38ef7d, #11998e);
  transition: width 1s linear;
  position: relative;
  overflow: hidden;
}

.time-progress::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  animation: shine 2s infinite linear;
}

.feedback {
  margin-top: 20px;
  padding: 12px;
  border-radius: 10px;
  font-size: 18px;
  font-weight: bold;
  animation: fadeIn 0.5s;
  text-align: center;
  position: relative;
  overflow: hidden;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.feedback.success {
  background: rgba(56, 239, 125, 0.2);
  border: 2px solid #38ef7d;
  color: #38ef7d;
  box-shadow: 0 0 15px rgba(56, 239, 125, 0.3);
}

.feedback.error {
  background: rgba(255, 65, 108, 0.2);
  border: 2px solid #ff416c;
  color: #ff416c;
  box-shadow: 0 0 15px rgba(255, 65, 108, 0.3);
}

.checking-status {
  margin-top: 25px;
  padding: 20px;
  background: rgba(30, 30, 50, 0.7);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px;
  border: 1px solid rgba(249, 200, 14, 0.3);
  box-shadow: 0 0 15px rgba(249, 200, 14, 0.3);
  animation: pulse-border 2s infinite;
}

@keyframes pulse-border {
  0%,
  100% {
    box-shadow: 0 0 15px rgba(249, 200, 14, 0.3);
    border-color: rgba(249, 200, 14, 0.3);
  }
  50% {
    box-shadow: 0 0 25px rgba(249, 200, 14, 0.7);
    border-color: rgba(249, 200, 14, 0.7);
  }
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(249, 200, 14, 0.3);
  border-top: 4px solid #f9c80e;
  border-radius: 50%;
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.checking-text {
  color: #f9c80e;
  font-size: 16px;
  font-weight: bold;
  text-align: center;
  text-shadow: 0 0 8px rgba(249, 200, 14, 0.5);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    text-shadow: 0 0 12px rgba(249, 200, 14, 0.8);
    transform: scale(1.05);
  }
}

.scrollable-content::-webkit-scrollbar {
  width: 8px;
}

.scrollable-content::-webkit-scrollbar-track {
  background: rgba(30, 30, 50, 0.3);
  border-radius: 4px;
}

.scrollable-content::-webkit-scrollbar-thumb {
  background: rgba(74, 72, 62, 0.7);
  border: 1px solid rebeccapurple;
  border-radius: 4px;
}

.scrollable-content::-webkit-scrollbar-thumb:hover {
  background: rgba(249, 200, 14, 0.7);
}

@media (max-width: 768px) {
  .modal-container {
    width: 80%;
    max-height: 85vh;
  }

  .problem {
    font-size: 18px;
    padding: 12px;
  }

  .choice-options {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .option {
    padding: 12px;
    font-size: 14px;
  }

  .option-icon {
    font-size: 36px;
  }

  .option-text {
    font-size: 18px;
  }

  .answer-input textarea {
    font-size: 14px;
    min-height: 80px;
  }

  .submit-button {
    font-size: 14px;
    width: 90px;
    height: 40px;
  }

  .timer {
    padding: 8px;
  }

  .feedback {
    font-size: 16px;
    padding: 10px;
  }
}
</style>