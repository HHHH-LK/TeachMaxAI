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

            <div class="answer-input">
              <textarea
                v-model="userAnswer"
                placeholder="请输入您的答案"
                rows="4"
                @keydown.enter.exact.prevent="submitAnswer"
              ></textarea>
              <button class="submit-button" @click="submitAnswer">
                <span class="button-text">提交</span>
                <span class="button-glow"></span>
              </button>
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

            <div v-if="feedback" :class="['feedback', feedbackClass]">
              {{ feedback }}
            </div>
          </div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="give-up-button" @click="handleGiveUp">
          <span class="button-text">放弃</span>
          <span class="button-glow"></span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from "vue";

const props = defineProps({
  question: {
    type: String,
    required: true,
  },
  answers: {
    type: Array,
    required: true,
  },
  timeLimit: {
    type: Number,
    default: 120,
  },
});

const emit = defineEmits(["close", "completed"]);

// 用户答案
const userAnswer = ref("");
const feedback = ref("");
const feedbackClass = ref("");
const timeLeft = ref(props.timeLimit);
const timer = ref(null);

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

// 提交答案
const submitAnswer = () => {
  if (userAnswer.value.trim() === "") {
    feedback.value = "请输入答案";
    feedbackClass.value = "error";
    return;
  }

  // 检查答案是否正确（不区分大小写）
  const userAnswerNormalized = userAnswer.value.trim().toLowerCase();
  const isCorrect = props.answers.some((answer) =>
    userAnswerNormalized.includes(answer.toLowerCase())
  );

  // 处理挑战完成
  handleChallengeCompleted(isCorrect);
};

// 处理放弃挑战
const handleGiveUp = () => {
  // 触发敌人攻击玩家
  const event = new CustomEvent("player-attack", {
    detail: { success: false },
  });
  document.dispatchEvent(event);

  // 设置反馈文本
  feedback.value = "✗ 放弃挑战，挑战失败";
  feedbackClass.value = "error";

  // 延迟关闭挑战框
  setTimeout(() => {
    emit("completed", false);
    closeModal();
  }, 700);
};

// 处理挑战完成事件
const handleChallengeCompleted = (success) => {
  // 触发全局攻击事件
  const event = new CustomEvent("player-attack", { detail: { success } });
  document.dispatchEvent(event);

  if (success) {
    feedback.value = "✓ 答案正确！挑战成功";
    feedbackClass.value = "success";
  } else {
    feedback.value = "✗ 答案不正确，挑战失败";
    feedbackClass.value = "error";
  }

  // 延迟关闭挑战框
  setTimeout(() => {
    emit("completed", success);
    closeModal();
  }, 700);
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

/* 背景粒子效果 */
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

/* 可滚动内容区域 */
.scrollable-content {
  overflow-y: auto; /* 启用垂直滚动 */
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

.answer-input {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin: 20px 0;
}

.answer-input textarea {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  border-radius: 10px;
  border: 2px solid #f9c80e;
  background: rgba(0, 0, 0, 0.3);
  color: white;
  outline: none;
  resize: vertical;
  min-height: 100px;
  transition: all 0.3s ease;
  box-shadow: 0 0 10px rgba(249, 200, 14, 0.3);
}

.answer-input textarea:focus {
  box-shadow: 0 0 20px rgba(249, 200, 14, 0.7);
  transform: scale(1.01);
}

.submit-button {
  background: linear-gradient(to right, #38ef7d, #11998e);
  border: none;
  padding: 0;
  font-size: 16px;
  color: white;
  border-radius: 10px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s ease;
  align-self: flex-end;
  position: relative;
  overflow: hidden;
  height: 45px;
  width: 100px;
}

.button-text {
  position: relative;
  z-index: 2;
  padding: 12px 0;
  display: block;
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

.modal-footer {
  padding: 15px;
  display: flex;
  justify-content: center;
  border-top: 1px solid rgba(255, 204, 0, 0.2);
  background: rgba(25, 25, 45, 0.8);
  position: relative;
  z-index: 2;
  flex-shrink: 0;
}

.give-up-button {
  background: linear-gradient(to right, #ff416c, #ff4b2b);
  border: none;
  padding: 0;
  font-size: 16px;
  color: white;
  border-radius: 50px;
  cursor: pointer;
  font-weight: bold;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  box-shadow: 0 5px 15px rgba(255, 65, 108, 0.4);
  position: relative;
  overflow: hidden;
  height: 45px;
  width: 100px;
}

.give-up-button .button-text {
  position: relative;
  z-index: 2;
  padding: 10px 0;
  display: block;
}

.give-up-button .button-glow {
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

.give-up-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(255, 65, 108, 0.6);
}

/* 滚动条样式 */
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

  .answer-input textarea {
    font-size: 14px;
    min-height: 80px;
  }

  .submit-button {
    font-size: 14px;
    width: 90px;
    height: 40px;
  }

  .give-up-button {
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
