<template>
  <!-- 遮罩层 -->
  <div class="modal-mask" v-if="isModalOpen"></div>

  <div class="modal-overlay" v-if="isModalOpen">
    <!-- 背景粒子效果 -->
    <div class="background-particles">
      <div
        v-for="(particle, index) in backgroundParticles"
        :key="index"
        class="particle"
        :style="particle.style"
      ></div>
    </div>

    <div
      class="card-img"
      :style="{
        backgroundImage: `url(${getCardImage})`,
        transform: cardTransform,
      }"
      @mousemove="handleCardMove"
      @mouseleave="handleCardLeave"
    ></div>

    <div class="modal-container" @click.stop>
      <!-- 顶部装饰 -->
      <div class="decoration-top">
        <div class="crystal crystal-left"></div>
        <div class="crystal crystal-right"></div>
      </div>

      <div class="modal-header">
        <h2>卡牌效果触发!</h2>
      </div>

      <div class="modal-body">
        <div class="difficulty-display">
          <span class="difficulty-label">难度:</span>
          <span :class="['difficulty-value', difficulty.toLowerCase()]">
            {{ difficulty }}
          </span>
        </div>

        <div class="effect-description">
          <p v-if="difficulty === 'easy'">这张卡牌效果较弱，但对新手友好</p>
          <p v-else-if="difficulty === 'medium'">标准卡牌效果，适合普通玩家</p>
          <p v-else-if="difficulty === 'hard'">强力卡牌效果，使用需谨慎!</p>
          <p v-else>未知卡牌效果，请小心使用</p>
        </div>

        <div class="damage-preview">
          <span>威力:</span>
          <div class="damage-meter">
            <div
              :class="['damage-fill', difficulty.toLowerCase()]"
              :style="{ width: damagePercentage + '%' }"
            ></div>
          </div>
          <span>{{ damageValue }} 点伤害</span>
        </div>
      </div>

      <div class="modal-footer">
        <button class="confirm-button" @click="startChallenge">
          <span class="button-text">开始挑战</span>
          <span class="button-glow"></span>
        </button>
      </div>

      <!-- 底部装饰 -->
      <div class="decoration-bottom">
        <div class="runes">
          <div class="rune rune-1">✦</div>
          <div class="rune rune-2">✧</div>
          <div class="rune rune-3">✤</div>
        </div>
      </div>
    </div>
  </div>

  <ChallengeModal
    v-if="showChallenge"
    :question="currentQuestion"
    :answers="currentAnswers"
    :correct-answers="correctAnswers"
    :explanation="explanation"
    :time-limit="timeLimit"
    :question-type="questionType"
    :question-id="selectedQuestionRef?.id"
    :floor-id="currentFloor"
    :change-count="changeCount"
    @close="closeChallenge"
    @completed="handleChallengeCompleted"
    @give-up="handleChallengeGiveUp"
    @damage="handleDamage"
  />
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import ChallengeModal from "./ChallengeModal.vue";
import { gameService } from "@/services/game";
import userConfig from "@/config/userConfig";

const props = defineProps({
  show: Boolean,
  difficulty: {
    type: String,
    default: "Normal", // 添加默认值
  },
  questionBank: Array, // 接收题目数据
  currentFloor: Number, // 当前楼层ID
  changeCount: Number, // 变化计数
  currentRound: Number,
});

const emit = defineEmits(["close", "correct-answer", "wrong-answer", "damage"]);

const damageMap = {
  easy: 15,
  medium: 30,
  hard: 60,
  Legendary: 100,
};

const showChallenge = ref(false);
const isModalOpen = ref(true);
const currentQuestion = ref("");
const currentAnswers = ref([]);
const correctAnswers = ref([]); // 存储正确答案
const questionType = ref("open_question"); // 题目类型
const answerLabelMap = ref({}); // 存储答案字符串到标签的映射
const explanation = ref([])
const selectedQuestionRef = ref(null); // 存储当前选中的题目
const timeLimit = ref(120);

// 背景粒子效果
const backgroundParticles = ref([]);
for (let i = 0; i < 20; i++) {
  backgroundParticles.value.push({
    style: {
      top: `${Math.random() * 100}%`,
      left: `${Math.random() * 100}%`,
      width: `${Math.random() * 10 + 5}px`,
      height: `${Math.random() * 10 + 5}px`,
      animationDuration: `${Math.random() * 5 + 3}s`,
      animationDelay: `${Math.random() * 2}s`,
      opacity: Math.random() * 0.5 + 0.2,
    },
  });
}

// 卡牌悬停效果
const cardRotation = ref({ x: 0, y: 0 });
const cardTransform = computed(() => {
  return `translateY(-50%) rotateX(${cardRotation.value.x}deg) rotateY(${cardRotation.value.y}deg)`;
});

// 根据难度获取卡牌图片
const getCardImage = computed(() => {
  const basePath = "/Image/GameCard";
  switch (props.questionBank[0]?.difficulty) {
    case "easy":
      return `${basePath}_Easy.png`;
    case "medium":
      return `${basePath}_Middle.png`;
    case "hard":
      return `${basePath}_Hard.png`;
    default:
      return `${basePath}.png`;
  }
});


const damageValue = ref(30); // 默认值30

// 创建一个异步函数获取伤害值
const fetchDamageValue = async () => {
  try {
    console.log("题目ID和当前楼层", props.questionBank[0]?.id, props.currentFloor)
    // 确保有题目ID和当前楼层
   
      const response = await gameService.fighting.getDamage(
        props.questionBank[0]?.id, 
        props.currentFloor
      );
      
      console.log("attack", response.data.data)
      // 更新伤害值，使用响应式变量
      damageValue.value = response.data.data || 30; // 如果API返回空则使用默认值
    }catch (error) {
    console.error('获取伤害值失败:', error);
    damageValue.value = 30; // 出错时使用默认值
  }
};

// 处理卡牌鼠标移动
const handleCardMove = (event) => {
  const card = event.currentTarget;
  const rect = card.getBoundingClientRect();

  // 计算鼠标在卡牌上的相对位置 (0-1)
  const x = (event.clientX - rect.left) / rect.width;
  const y = (event.clientY - rect.top) / rect.height;

  // 计算旋转角度 (最大15度)
  const rotateX = (0.5 - y) * 30;
  const rotateY = (x - 0.5) * 30;

  cardRotation.value = {
    x: rotateX,
    y: rotateY,
  };
};

// 处理鼠标离开卡牌
const handleCardLeave = () => {
  cardRotation.value = { x: 0, y: 0 };
};

const startChallenge = () => {
  // 重置映射
  answerLabelMap.value = {};

  // 根据难度筛选题目
  const filteredQuestions = props.questionBank.filter(
    (q) => q.difficulty === props.difficulty
  );

  if (filteredQuestions.length > 0) {
    const randomIndex = Math.floor(Math.random() * filteredQuestions.length);
    const selectedQuestion = filteredQuestions[randomIndex];
    selectedQuestionRef.value = selectedQuestion;

    // 构建答案字符串数组和映射
    const answerStrings = selectedQuestion.options.map((opt) => {
      const str = `${opt.label}. ${opt.content}`;
      answerLabelMap.value[str] = opt.label;
      return str;
    });

    currentQuestion.value = selectedQuestion.content;
    currentAnswers.value = answerStrings;
    correctAnswers.value = selectedQuestion.correctAnswer; // 设置正确答案
    questionType.value = selectedQuestion.type; // 设置题目类型
    explanation.value = selectedQuestion.explanation;

    // 根据难度调整时间限制
    switch (props.difficulty) {
      case "eeasy":
        timeLimit.value = 150;
        break;
      case "medium":
        timeLimit.value = 120;
        break;
      case "hard":
        timeLimit.value = 90;
        break;
      case "Legendary":
        timeLimit.value = 60;
        break;
      default:
        timeLimit.value = 120;
    }

    showChallenge.value = true;
  } else {
    // 如果没有匹配的题目，使用默认题目
    currentQuestion.value = "请简述水的三态变化过程";
    currentAnswers.value = ["固态、液态、气态之间的相互转化"];
    correctAnswers.value = ["固态、液态、气态之间的相互转化"]; // 设置默认正确答案
    questionType.value = "open_question"; // 设置默认题目类型
    showChallenge.value = true;
  }
};

const handleGiveUp = () => {
  // 触发全局放弃事件
  const event = new CustomEvent("global-give-up");
  document.dispatchEvent(event);

  // 关闭模态框
  closeModal();
};

// 关闭挑战模态框
const closeChallenge = () => {
  showChallenge.value = false;
};

// 处理挑战完成事件
const handleChallengeCompleted = (success) => {
  // 根据挑战结果触发不同事件
  if (success) {
    emit('correct-answer');
  } else {
    emit('wrong-answer');
  }
  
  // 关闭整个模态框
  closeModal();
};

// 处理挑战放弃事件
const handleChallengeGiveUp = () => {
  // 放弃挑战视为失败
  emit('wrong-answer');
  closeModal();
};

// 处理伤害事件
const handleDamage = (damageInfo) => {
  // 将伤害事件传递给父组件
  emit('damage', damageInfo);
};

const closeModal = () => {
  isModalOpen.value = false;
  emit("close");
};

// 监听全局放弃事件
const handleGlobalGiveUp = () => {
  closeModal();
};

onMounted(() => {
  fetchDamageValue();
  // 监听全局放弃事件
  document.addEventListener("global-give-up", handleGlobalGiveUp);
});

onBeforeUnmount(() => {
  // 移除事件监听
  document.removeEventListener("global-give-up", handleGlobalGiveUp);
});
</script>

<style scoped>
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    135deg,
    rgba(0, 0, 0, 0.5),
    rgba(30, 10, 60, 0.1)
  );
  z-index: 19999;
  backdrop-filter: blur(10px);
  animation: maskPulse 8s infinite alternate;
  pointer-events: none;
}

@keyframes maskPulse {
  0% {
    background: linear-gradient(
      135deg,
      rgba(0, 0, 0, 0.3),
      rgba(30, 10, 60, 0.4)
    );
  }
  50% {
    background: linear-gradient(
      135deg,
      rgba(10, 5, 30, 0.4),
      rgba(40, 15, 80, 0.5)
    );
  }
  100% {
    background: linear-gradient(
      135deg,
      rgba(20, 0, 40, 0.5),
      rgba(50, 20, 100, 0.6)
    );
  }
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 20000;
  pointer-events: none;
}

/* 背景粒子效果 */
.background-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.particle {
  position: absolute;
  background: rgba(255, 255, 255, 0.3);
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

.card-img {
  position: absolute;
  top: 50%;
  right: 10rem;
  transform: translateY(-50%);
  width: 220px;
  height: 300px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  border-radius: 18px;
  box-shadow: 0 5px 25px rgba(0, 0, 0, 0.6), 0 0 30px rgba(249, 200, 14, 0.5);
  z-index: 20001;
  border: 3px solid #f9c80e;
  pointer-events: auto; /* 允许卡牌图片接收事件 */
  transition: transform 0.3s ease;
  transform-style: preserve-3d;
  perspective: 1000px;
  animation: cardGlow 3s infinite alternate;
}

@keyframes cardGlow {
  0% {
    box-shadow: 0 5px 25px rgba(0, 0, 0, 0.6), 0 0 20px rgba(249, 200, 14, 0.4);
  }
  100% {
    box-shadow: 0 5px 25px rgba(0, 0, 0, 0.6), 0 0 40px rgba(249, 200, 14, 0.8);
  }
}

.modal-container {
  width: 85%;
  max-width: 500px;
  background: linear-gradient(
    145deg,
    rgba(26, 26, 46, 0.9),
    rgba(22, 33, 62, 0.9)
  );
  border-radius: 20px;
  box-shadow: 0 0 30px rgba(255, 204, 0, 0.5), 0 0 50px rgba(76, 201, 240, 0.3);
  overflow: hidden;
  font-family: "Arial", sans-serif;
  position: relative;
  z-index: 20002;
  border: 2px solid #f9c80e;
  animation: modalAppear 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  pointer-events: auto; /* 允许模态框内容接收事件 */
  backdrop-filter: blur(10px);
  overflow: visible;
}

@keyframes modalAppear {
  0% {
    transform: translateY(30px) scale(0.95);
    opacity: 0;
    filter: blur(10px);
  }
  100% {
    transform: translateY(0) scale(1);
    opacity: 1;
    filter: blur(0);
  }
}

/* 装饰元素 */
.decoration-top {
  position: absolute;
  top: -20px;
  left: 0;
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding: 0 30px;
  z-index: 10;
}

.crystal {
  width: 40px;
  height: 40px;
  background: rgba(249, 200, 14, 0.3);
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
  animation: crystalGlow 3s infinite alternate;
}

.crystal-left {
  transform: rotate(45deg);
}

.crystal-right {
  transform: rotate(-45deg);
}

@keyframes crystalGlow {
  0% {
    filter: drop-shadow(0 0 5px rgba(249, 200, 14, 0.5));
  }
  100% {
    filter: drop-shadow(0 0 15px rgba(249, 200, 14, 0.8));
  }
}

.modal-header {
  background: linear-gradient(
    to right,
    rgba(25, 25, 45, 0.9),
    rgba(40, 30, 70, 0.8)
  );
  padding: 20px;
  border-bottom: 2px solid #f9c80e;
  position: relative;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

.modal-header h2 {
  margin: 0;
  color: #f9c80e;
  text-align: center;
  font-size: 26px;
  text-shadow: 0 0 10px rgba(249, 200, 14, 0.7);
  letter-spacing: 2px;
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

/* 关闭按钮样式 */
.close-button {
  position: absolute;
  top: 10px;
  right: 15px;
  background: transparent;
  border: none;
  color: #f9c80e;
  font-size: 28px;
  cursor: pointer;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  transition: all 0.3s ease;
  z-index: 3;
}

.close-button:hover {
  background: rgba(255, 0, 0, 0.3);
  transform: scale(1.1);
  box-shadow: 0 0 15px rgba(255, 0, 0, 0.5);
}

.modal-body {
  padding: 25px;
  color: #e2e2e2;
  position: relative;
  z-index: 2;
}

.difficulty-display {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 25px;
  position: relative;
}

.difficulty-label {
  font-size: 20px;
  margin-right: 10px;
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.5);
}

.difficulty-value {
  font-size: 24px;
  font-weight: bold;
  padding: 8px 20px;
  border-radius: 10px;
  position: relative;
  overflow: hidden;
}

.difficulty-value::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    45deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  animation: shine 3s infinite linear;
}

@keyframes shine {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.easy {
  background: linear-gradient(to right, #4facfe, #5b9c9f);
  color: #fff;
  box-shadow: 0 0 15px rgba(79, 172, 254, 0.5);
}

.normal {
  background: linear-gradient(to right, #38ef7d, #11998e);
  color: #fff;
  box-shadow: 0 0 15px rgba(56, 239, 125, 0.5);
}

.hard {
  background: linear-gradient(to right, #ff416c, #ff4b2b);
  color: #fff;
  box-shadow: 0 0 15px rgba(255, 65, 108, 0.5);
}

@keyframes pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 15px rgba(142, 45, 226, 0.3);
  }
  10% {
    transform: scale(1.05);
    box-shadow: 0 0 30px rgba(142, 45, 226, 0.5);
  }
}

.effect-description {
  background: linear-gradient(
    to right,
    rgba(30, 30, 50, 0.7),
    rgba(40, 35, 70, 0.6)
  );
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 25px;
  font-size: 18px;
  line-height: 1.6;
  text-align: center;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 204, 0, 0.3);
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.5);
  position: relative;
}

.effect-description::after {
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

.damage-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  margin-bottom: 20px;
  position: relative;
}

.damage-meter {
  width: 50%;
  height: 20px;
  background: rgba(50, 50, 70, 0.7);
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.5);
}

.damage-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 1.5s ease-in-out;
  position: relative;
  overflow: hidden;
}

.damage-fill::after {
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

.damage-fill.easy {
  background: linear-gradient(to right, #4facfe, #00f2fe);
}

.damage-fill.normal {
  background: linear-gradient(to right, #38ef7d, #11998e);
}

.damage-fill.hard {
  background: linear-gradient(to right, #ff416c, #ff4b2b);
}

.modal-footer {
  padding: 20px;
  display: flex;
  justify-content: center;
  gap: 20px;
  border-top: 1px solid rgba(255, 204, 0, 0.2);
  background: rgba(25, 25, 45, 0.8);
  position: relative;
}

.confirm-button {
  background: linear-gradient(to right, #4facfe, #00f2fe);
  border: none;
  padding: 0;
  font-size: 18px;
  color: white;
  border-radius: 50px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  width: 180px;
  height: 50px;
}

.button-text {
  position: relative;
  z-index: 2;
  display: block;
  padding: 12px 35px;
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

.confirm-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(79, 172, 254, 0.6);
}

.confirm-button:active {
  transform: translateY(1px);
}

/* 底部装饰 */
.decoration-bottom {
  position: absolute;
  bottom: -20px;
  left: 0;
  width: 100%;
  display: flex;
  justify-content: center;
}

.runes {
  display: flex;
  gap: 30px;
}

.rune {
  font-size: 24px;
  color: #f9c80e;
  animation: runeGlow 3s infinite alternate;
  text-shadow: 0 0 10px rgba(249, 200, 14, 0.5);
}

.rune-1 {
  animation-delay: 0s;
}

.rune-2 {
  animation-delay: 0.5s;
}

.rune-3 {
  animation-delay: 1s;
}

@keyframes runeGlow {
  0% {
    opacity: 0.7;
    transform: translateY(0);
    text-shadow: 0 0 5px rgba(249, 200, 14, 0.5);
  }
  100% {
    opacity: 1;
    transform: translateY(-5px);
    text-shadow: 0 0 15px rgba(249, 200, 14, 0.8);
  }
}

@media (max-width: 768px) {
  .card-img {
    display: none;
  }

  .modal-container {
    width: 95%;
  }

  .problem {
    font-size: 32px;
  }

  .answer-input {
    flex-direction: column;
    align-items: center;
  }

  .answer-input input {
    width: 100%;
    max-width: 300px;
  }
}
</style>
