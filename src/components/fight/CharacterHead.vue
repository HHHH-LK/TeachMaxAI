<template>
  <div class="top-bar">
    <!-- 左侧玩家信息 -->
    <div class="player-info">
      <div class="player-name-container">
        <span class="player-name">{{ playerName }}</span>
        <div class="crown-icon"></div>
      </div>
      <div class="player-level-container">
        <div class="level-and-exp">
          <span class="player-level">Lv.{{ playerLevel }}</span>
          <div class="experience-bar">
            <div class="exp-background">
              <div
                class="exp-fill"
                :style="{ width: expPercentage + '%' }"
              ></div>
            </div>
            <div class="exp-text">{{ currentExp }}/{{ maxExp }}</div>
          </div>
        </div>
        <div class="level-decoration"></div>
      </div>
    </div>

    <!-- 中间战斗信息 -->
    <div class="battle-info">
      <div class="turn-container">
        <span class="turn-count">回合 {{ turnCount }}</span>
        <div class="hourglass-icon"></div>
      </div>
      <div class="floor-container">
        <span class="floor-info">深渊 {{ floorLevel }}层</span>
        <div class="stairs-icon"></div>
      </div>
    </div>

    <!-- 右侧退出按钮 -->
    <div class="exit-control">
      <button class="exit-button" @click="handleExitClick">
        <span class="button-text">退出</span>
        <div class="skull-icon"></div>
      </button>
    </div>

    <!-- 动态效果元素 -->
    <div class="floating-runes"></div>
    <div class="shadow-wraiths"></div>
    <div class="dragon-breath"></div>

    <!-- 加载指示器 -->
    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
    </div>
  </div>

  <ExitConfirmationModal
    v-model:visible="showExpProgress"
    @confirm="handleConfirmExit"
    @cancel="handleCancelExit"
  />
</template>

<script setup>
import { ref, onMounted, computed, defineProps, watch } from "vue";
import { ElMessage } from "element-plus";
import ExitConfirmationModal from "./ExitConfirmationModal.vue";
import { gameService } from "@/services/game";
import userConfig from "@/config/userConfig.js";
import router from "@/router";
import { useRoute } from "vue-router";

const route = useRoute();

const props = defineProps({
  currentRound: {
    type: Number,
  },
  towerLevelCur: {
    type: Number,
  },
  playerLevel: {
    type: Number,
    default: 1,
  },
  currentExp: {
    type: Number,
    default: 0,
  },
  maxExp: {
    type: Number,
    default: 100,
  },
});

// 玩家信息
const playerName = ref("加载中...");
const playerLevel = ref(0);
const currentExp = ref(0);
const maxExp = ref(100);
const turnCount = ref(0);
const floorLevel = ref(0);
const emit = defineEmits(["exp-update"]);

// 状态控制
const loading = ref(true);
const showExitModal = ref(false);

floorLevel.value = props.towerLevelCur;

// 监听路由参数变化
watch(
  () => route.params.towerLevel,
  (newVal) => {
    floorLevel.value = newVal ? Number(newVal) : 0;
  }
);

watch(
  () => props.currentRound,
  (newValue) => {
    console.log(
      `检测到 currentRound 变化: ${props.currentRound} → ${newValue}`
    );
    turnCount.value = newValue;
  }
);

// 计算经验百分比
const expPercentage = computed(() => {
  return props.maxExp > 0
    ? Math.round((props.currentExp / props.maxExp) * 100)
    : 0;
});

// 获取玩家信息
const fetchPlayerInfo = async () => {
  try {
    turnCount.value = props.currentRound;
    console.log("round", props.currentRound);
    console.log("turn", turnCount.value);
    loading.value = true;

    const infoResponse = await gameService.gameUser.getPlayerInfo(
      userConfig.studentId
    );
    if (infoResponse.data && infoResponse.data.success) {
      const playerData = infoResponse.data.data;
      playerName.value = playerData.gameName || "无名玩家";
    } else {
      throw new Error(infoResponse.data.message || "获取玩家信息失败");
    }
    playerLevel.value = props.playerLevel;
    currentExp.value = props.currentExp;
    maxExp.value = props.maxExp;

    fetchExp();
  } catch (error) {
    console.error("获取玩家信息失败:", error);
    ElMessage.error(`获取玩家信息失败: ${error.message}`);
  } finally {
    loading.value = false;
  }
};

const handleExitClick = () => {
  showExitModal.value = true;

  // 添加按钮点击反馈
  const button = document.querySelector(".exit-button");
  if (button) {
    button.classList.add("clicked");
    setTimeout(() => {
      button.classList.remove("clicked");
    }, 300);
  }
};

// 处理确认退出
const handleConfirmExit = () => {
  console.log("确认退出");
  showExitModal.value = false;
  router
    .push("/game")
    .then(() => {
      console.log("跳转成功");
      window.location.reload(); // 在跳转完成后刷新页面
    })
    .catch((err) => {
      console.error("路由跳转失败:", err);
    });
};

// 处理取消退出
const handleCancelExit = () => {
  console.log("取消退出");
  showExitModal.value = false;
};

const fetchExp = async () => {
  const response = await gameService.fighting.getExp(playerLevel.value);
  console.log(response.data);
  if (response.data) {
    maxExp.value = response.data.data;
  }
};

// 初始化时获取玩家信息
onMounted(() => {
  fetchPlayerInfo();

  // 动画效果
  const topBar = document.querySelector(".top-bar");
  topBar.classList.add("animated");
});
</script>

<style scoped>
@import url("https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css");

.top-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 70px;
  background: linear-gradient(to bottom, #0a0805, #1a140f),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect width="100" height="100" fill="%230a0805"/><path d="M0,0 L100,100 M100,0 L0,100" stroke="%23150f0a" stroke-width="1"/></svg>');
  background-size: auto, 100px 100px;
  border-bottom: 3px solid #3a2a1a;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  box-shadow: 0 6极px 12px rgba(0, 0, 0, 0.8), inset 0 0 20px rgba(0, 0, 0, 0.7);
  z-index: 1000;
  font-family: "MedievalSharp", "Courier New", monospace;
  color: #d4af37;
  text-shadow: 0 0 5px rgba(180, 150, 50, 0.8), 2px 2px 3px rgba(0, 0, 0, 0.9);
  overflow: hidden;
}

.top-bar::before,
.top-bar::after {
  content: "";
  position: absolute;
  height: 100%;
  top: 0;
  width: 30px;
  background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 70"><path d="M0,0 L30,0 L30,10 C25,15 25,25 30,30 C25,35 25,45 30,50 C25,55 25,65 30,70 L0,70 Z" fill="%233a2a1a"/><path d="M5,5 L25,5 L25,15 C22,17 22,23 25,25 C22,27 22,33 25,35 C22,37 22,43 25,45 C22,47 22,53 25,55 L5,55 Z" fill="%235a3a1a"/></svg>');
  background-repeat: repeat-y;
  z-index: 2;
}

.top-bar::before {
  left: 0;
}

.top-bar::after {
  right: 0;
  transform: scaleX(-1);
}

.player-info {
  display: flex;
  gap: 25px;
  align-items: center;
  position: relative;
  z-index: 3;
}

.player-name-container {
  position: relative;
  display: flex;
  align-items: center;
}

.player-name {
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 2px;
  background: linear-gradient(to bottom, #d4af37, #8e6c1f);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  padding: 8px 20px;
  border: 2px solid #5a3a1a;
  border-radius: 10px;
  box-shadow: inset 0 0 15px rgba(0, 0, 0, 0.7), 0 5px 10px rgba(0, 0, 0, 0.8);
  position: relative;
  overflow: hidden;
}

.player-name::before {
  content: "";
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 215, 0, 0.3),
    transparent
  );
  animation: nameShine 3s infinite;
}

.crown-icon {
  position: absolute;
  top: -20px;
  right: -15px;
  width: 35px;
  height: 25px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 35 25"><path d="M0,20 L5,10 L10,15 L15,5 L20,15 L25,10 L30,20 Z" fill="%23d4af37"/><circle cx="7" cy="10" r="2" fill="%23ff0000"/><circle cx="17.5" cy="5" r="2" fill="%23ff0000"/><circle cx="28" cy="10" r="2" fill="%23ff0000"/></svg>');
  background-size: contain;
  filter: drop-shadow(0 3px 3px rgba(0, 0, 0, 0.7));
  animation: float 3s infinite ease-in-out;
}

.player-level-container {
  position: relative;
  display: flex;
  flex-direction: column;
}

/* 等级和经验条容器 */
.level-and-exp {
  display: flex;
  align-items: center;
  gap: 15px;
}

.player-level {
  font-size: 20px;
  padding: 8px 15px;
  background: linear-gradient(to bottom, #3a2a1a, #1a140f);
  border: 2px solid #5a3a1a;
  border-radius: 8px;
  box-shadow: inset 0 0 15px rgba(0, 0, 0, 0.8), 0 5px 10px rgba(0, 0, 0, 0.8);
  position: relative;
  z-index: 2;
}

.level-decoration {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 50px;
  height: 50px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50"><path d="M25,0 Q40,10 50,25 Q40,40 25,50 Q10,40 0,25 Q10,10 25,0 Z" fill="none" stroke="%23d4af37" stroke-width="1.5"/></svg>');
  background-size: contain;
  transform: translate(-50%, -50%);
  z-index: 1;
  opacity: 0.8;
  animation: rotate 10s linear infinite;
}

/* 调整经验条样式 */
.experience-bar {
  position: relative;
  width: 180px;
  margin-top: 0;
  z-index: 3;
}

.exp-background {
  width: 100%;
  height: 12px;
  background: rgba(30, 20, 10, 0.7);
  border: 1px solid #5a3a1a;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.8), 0 2px 4px rgba(0, 0, 0, 0.6);
}

.exp-fill {
  height: 100%;
  background: linear-gradient(to right, #d4af37, #b8860b, #d4af37);
  border-radius: 6px;
  transition: width 0.5s ease;
  box-shadow: inset 0 0 10px rgba(255, 215, 0, 0.5),
    0 0 5px rgba(212, 175, 55, 0.5);
  position: relative;
  overflow: hidden;
}

.exp-fill::after {
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
  animation: shine 3s infinite linear;
}

.exp-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  color: #ffd700;
  text-shadow: 0 0 3px rgba(0, 0, 0, 0.8);
  font-weight: bold;
  z-index: 2;
}

.battle-info {
  display: flex;
  gap: 30px;
  position: relative;
  z-index: 3;
}

.turn-container,
.floor-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  padding: 10px 25px;
  background: rgba(30, 20, 10, 0.7);
  border: 2px solid #5a3a1a;
  border-radius: 10px;
  box-shadow: inset 0 0 15px rgba(0, 0, 0, 0.8), 0 5px 10px rgba(0, 0, 0, 0.8);
  overflow: hidden;
}

.turn-container::before,
.floor-container::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    45deg,
    transparent,
    rgba(255, 215, 0, 0.1),
    transparent
  );
  animation: shine 5s infinite linear;
}

.turn-count {
  font-size: 22px;
  letter-spacing: 2px;
  margin-bottom: 5px;
  position: relative;
  z-index: 2;
}

.floor-info {
  font-size: 20px;
  color: #a08060;
  text-shadow: 0 0 8px rgba(160, 128, 96, 0.9);
  position: relative;
  z-index: 2;
}

.hourglass-icon {
  position: absolute;
  top: -15px;
  right: -12px;
  width: 30px;
  height: 30px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="极0 0 30 30"><path d="M15,0 L25,8 L25,12 L15,20 L5,12 L5,8 Z M5,18 L5,22 L15,30 L25,22 L25,18 L15,25 Z" fill="%23d4af37"/></svg>');
  background-size: contain;
  filter: drop-shadow(0 3px 3px rgba(0, 0, 0, 0.7));
  animation: pulse 2s infinite;
}

.stairs-icon {
  position: absolute;
  bottom: -15px;
  left: -12px;
  width: 30px;
  height: 30px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 30"><path d="M0,30 L5,25 L10,30 L15,25 L20,30 L25,25 L30,30 L30,25 L25,20 L20,25 L15,20 L10,25 L5,20 L0,25 Z M0,20 L5,15 L10,20 L15,15 L20,20 L25,15 L30,20 L30,15 L25,10 L20,15 L15,10 L10,15 L5,10 L0,15 Z M0,10 L5,5 L10,10 L15,5 L20,10 L25,5 L30,10 L30,5 L25,0 L20,5 L15,0 L10,5 L5,0 L0,5 Z" fill="%23a08060"/></svg>');
  background-size: contain;
  filter: drop-shadow(0 3px 3px rgba(0, 0, 0, 0.7));
  animation: pulse 2s infinite 0.5s;
}

.exit-control {
  position: relative;
  z-index: 3;
}

.exit-button {
  position: relative;
  background: linear-gradient(to bottom, #5a0a0a, #3a0000),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" fill="%235a0a0a"/><path d="M0,0 L20,20 M20,0 L0,20" stroke="%237a0a0a" stroke-width="1"/></svg>');
  background-size: auto, 10px 10px;
  color: #d0a050;
  border: 2px solid #7a0a0a;
  border-radius: 10px;
  padding: 12px 25px 12px 45px;
  font-family: inherit;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  text-shadow: 1px 1px 2px #000;
  box-shadow: 0 5px 0 #3a0000, inset 0 0 15px rgba(0, 0, 0, 0.8);
  transition: all 0.2s ease;
  overflow: hidden;
}

.exit-button:hover {
  background: linear-gradient(to bottom, #7a0a0a, #5a0000),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" fill="%237a0a0a"/><path d="M0,0 L20,20 M20,0 L0,20" stroke="%239a0a0a" stroke-width="1"/></svg>');
  background-size: auto, 10px 10px;
  transform: translateY(-3px);
  box-shadow: 0 8px 0 #3a0000, inset 0 0 20px rgba(255, 0, 0, 0.5);
}

.exit-button:active {
  transform: translateY(3px);
  box-shadow: 0 2px 0 #3a0000, inset 0 0 15px rgba(0, 0, 0, 0.9);
}

.skull-icon {
  position: absolute;
  top: 50%;
  left: 15px;
  width: 30px;
  height: 30px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 30 30"><circle cx="15" cy="12" r="8" fill="%23aaa"/><path d="M10,18 Q15,23 20,18 Q18,21 15,21 Q12,21 10,18 Z" fill="%23aaa"/><circle cx="11" cy="10" r="2.5" fill="%23000"/><circle cx="19" cy="10" r="2.5" fill="%23000"/></svg>');
  background-size: contain;
  transform: translateY(-50%);
  filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.8));
  animation: skullGlow 2s infinite;
}

.exit-button::before {
  content: "";
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10,0 Q15,8 20,20 Q8,15 0,20 Q5,8 10,0 Z" fill="%23a00"/></svg>');
  background-size: contain;
  opacity: 0.8;
  animation: bloodDrip 3s infinite;
}

/* 点击效果 */
.exit-button.clicked {
  transform: translateY(4px);
  box-shadow: 0 1px 0 #3a0000, inset 0 0 20px rgba(255, 0, 0, 0.7);
}

.exit-button.clicked .skull-icon {
  animation: skullShake 0.3s;
}

/* 动态效果元素 */
.floating-runes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.floating-runes::before {
  content: "";
  position: absolute;
  top: 15px;
  left: 0;
  width: 100%;
  height: 40px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 40"><text x="0" y="20" font-family="Medieval" font-size="20" fill="%23d4af37" opacity="0.3">ᚠ ᚢ ᚦ ᚨ ᚱ ᚲ ᚷ ᚹ ᚺ ᚾ ᛁ ᛃ ᛇ ᛈ ᛉ ᛊ ᛏ ᛒ ᛖ ᛗ ᛚ ᛜ ᛟ ᛞ</text></svg>');
  background-size: contain;
  animation: runesFloat 20s infinite linear;
}

.shadow-wraiths {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.shadow-wraiths::before,
.shadow-wraiths::after {
  content: "";
  position: absolute;
  width: 80px;
  height: 100px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 100"><path d="M40,0 Q60,20 80,40 Q60,60 40,80 Q20,60 0,40 Q20,20 40,0 Z" fill="%23000" opacity="0.3"/></svg>');
  background-size: contain;
  background-repeat: no-repeat;
  filter: blur(2px);
  animation: wraithFloat 15s infinite linear;
}

.shadow-wraiths::before {
  top: -20px;
  left: 10%;
  animation-delay: 0s;
}

.shadow-wraiths::after {
  top: -20px;
  left: 70%;
  animation-delay: 5s;
}

.dragon-breath {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.dragon-breath::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 5px;
  background: linear-gradient(to right, transparent, #ff4500, transparent);
  animation: dragonBreath 5s infinite;
}

@keyframes rotate {
  0% {
    transform: translate(-50%, -50%) rotate(0deg);
  }
  100% {
    transform: translate(-50%, -50%) rotate(360deg);
  }
}

@keyframes bloodDrip {
  0% {
    transform: translateY(0);
    opacity: 0.8;
  }
  50% {
    transform: translateY(10px);
    opacity: 0.4;
  }
  100% {
    transform: translateY(20px);
    opacity: 0;
  }
}

@keyframes nameShine {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

@keyframes skullGlow {
  0%,
  100% {
    filter: drop-shadow(0 0 0 rgba(255, 0, 0, 0));
  }
  50% {
    filter: drop-shadow(0 0 5px rgba(255, 0, 0, 0.8));
  }
}

@keyframes shine {
  0% {
    background-position: -100% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

@keyframes runesFloat {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(-100%);
  }
}

@keyframes wraithFloat {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.3;
  }
  25% {
    transform: translateY(20px) rotate(5deg);
    opacity: 0.5;
  }
  50% {
    transform: translateY(40px) rotate(0deg);
    opacity: 0.3;
  }
  75% {
    transform: translateY(20px) rotate(-5deg);
    opacity: 0.5;
  }
  100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.3;
  }
}

@keyframes dragonBreath {
  0% {
    opacity: 0;
    transform: scaleX(0);
  }
  50% {
    opacity: 1;
    transform: scaleX(1);
  }
  100% {
    opacity: 0;
    transform: scaleX(0);
  }
}

@keyframes skullShake {
  0% {
    transform: translateY(-50%) rotate(0deg);
  }
  25% {
    transform: translateY(-50%) rotate(10deg);
  }
  50% {
    transform: translateY(-50%) rotate(0deg);
  }
  75% {
    transform: translateY(-50%) rotate(-10deg);
  }
  100% {
    transform: translateY(-50%) rotate(0deg);
  }
}

/* 整体进入动画 */
.top-bar.animated {
  animation: barAppear 1s ease-out;
}

@keyframes barAppear {
  0% {
    transform: translateY(-100%);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 呼吸光效 */
.player-name,
.player-level,
.turn-count,
.floor-info,
.exit-button {
  animation: glow 3s infinite;
}

@keyframes glow {
  0% {
    box-shadow: 0 0 5px rgba(180, 150, 50, 0.5),
      inset 0 0 5px rgba(180, 150, 50, 0.3);
  }
  50% {
    box-shadow: 0 0 15px rgba(180, 150, 50, 0.8),
      inset 0 0 10px rgba(180, 150, 50, 0.5);
  }
  100% {
    box-shadow: 0 0 5px rgba(180, 150, 50, 0.5),
      inset 极0 0 5px rgba(180, 150, 50, 0.3);
  }
}
</style>
