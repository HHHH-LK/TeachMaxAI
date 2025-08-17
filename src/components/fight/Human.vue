<template>
  <div class="characters">
    <!-- 主角 -->
    <div
      class="main-character"
      :class="{
        'attack-animation': isPlayerAttacking,
        'hit-animation': isPlayerHit,
      }"
    >
      <img :src="humanImage" alt="Main Character" />

      <!-- 主角血条  -->
      <div class="health-bar">
        <div class="health-background">
          <div class="metal-frame"></div>
          <div class="rivets">
            <div class="rivet" v-for="i in 5" :key="'rivet-player-'+i"></div>
          </div>
        </div>
        <div class="damage-indicator" :style="{ width: damageIndicatorWidth + '%' }"></div>
        <div
          class="health-fill"
          :style="{ width: mainHealthPercentage + '%' }"
        >
          <div class="blood-texture"></div>
        </div>
        <div class="health-text">
          <span class="health-value">{{ mainHealth }}</span>
          <span class="health-divider">/</span>
          <span class="health-max">{{ mainMaxHealth }}</span>
        </div>
        <div class="blood-drips" v-if="isPlayerHit">
          <div class="blood-drip" v-for="i in 3" :key="'drip-player-'+i"></div>
        </div>
      </div>

      <!-- 主角受击特效 -->
      <div class="hit-particles" v-if="isPlayerHit">
        <div
          v-for="(particle, index) in playerParticles"
          :key="'player-particle-' + index"
          class="particle"
          :style="particle.style"
        ></div>
      </div>

      <!-- 流血效果 -->
      <div class="blood-effect" v-if="isPlayerHit">
        <div class="blood-splatter"></div>
      </div>
    </div>

    <!-- 敌人 -->
    <div
      class="enemy-character"
      :class="{
        'show-bracket': showBracket,
        'attack-animation': isEnemyAttacking,
        'hit-animation': isEnemyHit,
      }"
      ref="enemyContainer"
    >
      <div class="bracket-container" :style="bracketTransform">
        <div class="bracket bracket-left"></div>
        <div class="bracket bracket-right"></div>
      </div>
      <img :src="enemyImage" alt="Enemy Character" ref="enemyImg" />

      <!-- 敌人血条  -->
      <div class="health-bar">
        <div class="health-background">
          <div class="metal-frame"></div>
          <div class="rivets">
            <div class="rivet" v-for="i in 5" :key="'rivet-enemy-'+i"></div>
          </div>
        </div>
        <div class="damage-indicator" :style="{ width: enemyDamageIndicatorWidth + '%' }"></div>
        <div
          class="health-fill"
          :style="{ width: enemyHealthPercentage + '%' }"
        >
          <div class="blood-texture"></div>
        </div>
        <div class="health-text">
          <span class="health-value">{{ enemyHealth }}</span>
          <span class="health-divider">/</span>
          <span class="health-max">{{ enemyMaxHealth }}</span>
        </div>
        <div class="blood-drips" v-if="isEnemyHit">
          <div class="blood-drip" v-for="i in 3" :key="'drip-enemy-'+i"></div>
        </div>
      </div>

      <!-- 敌人受击特效 -->
      <div class="hit-particles" v-if="isEnemyHit">
        <div
          v-for="(particle, index) in enemyParticles"
          :key="'enemy-particle-' + index"
          class="particle"
          :style="particle.style"
        ></div>
      </div>

      <!-- 流血效果 -->
      <div class="blood-effect" v-if="isEnemyHit">
        <div class="blood-splatter"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from "vue";
import humanImage from "/Image/Human.png";
import enemyImage from "/Image/Enemy.png";

const props = defineProps({
  mainHealth: {
    type: Number,
    default: 100,
  },
  mainMaxHealth: {
    type: Number,
    default: 100,
  },
  enemyHealth: {
    type: Number,
    default: 100,
  },
  enemyMaxHealth: {
    type: Number,
    default: 150,
  },
});

// 定义emits
const emit = defineEmits(["player-hit", "enemy-hit"]);

// 使用computed属性确保响应式
const mainHealth = computed(() => props.mainHealth);
const mainMaxHealth = computed(() => props.mainMaxHealth);
const enemyHealth = computed(() => props.enemyHealth);
const enemyMaxHealth = computed(() => props.enemyMaxHealth);

// 其他状态
const showBracket = ref(false);
const bracketPosition = ref({ x: 0, y: 0 });
const enemyImg = ref(null);
const enemyContainer = ref(null);

// 受击状态
const isPlayerHit = ref(false);
const isEnemyHit = ref(false);
const isPlayerAttacking = ref(false);
const isEnemyAttacking = ref(false);
const attackTimeout = ref(null);

// 粒子数据
const playerParticles = ref([]);
const enemyParticles = ref([]);

// 伤害指示器宽度
const damageIndicatorWidth = ref(0);
const enemyDamageIndicatorWidth = ref(0);

// 计算血量百分比 - 使用各自的最大血量
const mainHealthPercentage = computed(() => {
  return (mainHealth.value / mainMaxHealth.value) * 100;
});

const enemyHealthPercentage = computed(() => {
  return (enemyHealth.value / enemyMaxHealth.value) * 100;
});

// 生成粒子数据
const generateParticles = (count, isEnemy = false) => {
  const particles = [];
  const baseColor = isEnemy ? "#8b0000" : "#ff0000";

  for (let i = 0; i < count; i++) {
    // 随机角度和距离
    const angle = Math.random() * Math.PI * 2;
    const distance = 100 + Math.random() * 150;

    // 随机起始位置偏移
    const startX = (Math.random() - 0.5) * 30;
    const startY = (Math.random() - 0.5) * 30;

    // 最终位置
    const endX = Math.cos(angle) * distance;
    const endY = Math.sin(angle) * distance;

    // 随机大小
    const size = Math.random() * 8 + 6;

    // 随机动画延迟
    const delay = Math.random() * 0.5;

    // 随机颜色变化
    const hueShift = Math.random() * 30 - 15;
    const color = adjustHue(baseColor, hueShift);

    particles.push({
      style: {
        "--x": `${startX}px`,
        "--y": `${startY}px`,
        "--x-2": `${endX}px`,
        "--y-2": `${endY}px`,
        "--delay": `${delay}s`,
        "--size": `${size}px`,
        "--color": color,
      },
    });
  }

  return particles;
};

// 辅助函数：调整颜色色调
const adjustHue = (hex, degrees) => {
  if (degrees === 0) return hex;

  if (hex === "#ff0000") {
    return degrees > 0 ? "#ff3333" : "#cc0000";
  } else {
    return degrees > 0 ? "#a52a2a" : "#660000";
  }
};

// 更新括号位置
const updatePosition = () => {
  if (enemyContainer.value) {
    const rect = enemyContainer.value.getBoundingClientRect();
    bracketPosition.value = {
      x: rect.width / 2,
      y: rect.height / 2,
    };
  }
};

// 响应式变换
const bracketTransform = computed(() => {
  return {
    transform: `translate(${bracketPosition.value.x}px, ${
      bracketPosition.value.y
    }px) scale(${showBracket.value ? 1 : 0})`,
    opacity: showBracket.value ? 0.9 : 0,
  };
});

// 响应全局事件
const handleBracketVisibility = (event) => {
  showBracket.value = event.detail;
  if (event.detail) {
    nextTick(updatePosition);
  }
};

// 处理攻击事件
const handleGlobalAttack = (event) => {
  const { success } = event.detail;

  // 清理之前的攻击状态
  if (attackTimeout.value) clearTimeout(attackTimeout.value);

  if (success) {
    // 玩家攻击敌人
    setTimeout(() => {
      enemyParticles.value = generateParticles(50, true);
      isPlayerAttacking.value = true;
      isEnemyHit.value = true;
    }, 2800);

    // 通知父组件敌人受到伤害
    emit('enemy-hit', 20);
    
    // 重置攻击状态
    setTimeout(() => {
      isPlayerAttacking.value = false;
    }, 500);

    setTimeout(() => {
      isEnemyHit.value = false;
    }, 800);
  } else {
    // 敌人攻击玩家
    setTimeout(() => {
      playerParticles.value = generateParticles(40);
      isEnemyAttacking.value = true;
      isPlayerHit.value = true;
    }, 2800);

    // 通知父组件玩家受到伤害
    emit('player-hit', 15);
    
    // 重置攻击状态
    setTimeout(() => {
      isEnemyAttacking.value = false;
    }, 500);

    setTimeout(() => {
      isPlayerHit.value = false;
    }, 800);
  }
};

onMounted(() => {
  // 初始位置更新
  updatePosition();

  // 窗口大小改变时更新位置
  window.addEventListener("resize", updatePosition);

  // 监听全局事件
  document.addEventListener(
    "global-bracket-visibility",
    handleBracketVisibility
  );
  document.addEventListener("player-attack", handleGlobalAttack);
});

onBeforeUnmount(() => {
  // 清理订阅
  if (attackTimeout.value) {
    clearTimeout(attackTimeout.value);
  }
  window.removeEventListener("resize", updatePosition);
  document.removeEventListener(
    "global-bracket-visibility",
    handleBracketVisibility
  );
  document.removeEventListener("player-attack", handleGlobalAttack);
});
</script>

<style scoped>
.characters {
  background: url("/Image/FightBackground.png") center/cover;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
  pointer-events: none;
}

.characters > * {
  pointer-events: none;
}

.main-character {
  position: absolute;
  left: 20%;
  bottom: 30%;
  transform: translateY(0);
  transition: transform 0.3s ease;
  z-index: 10;
  pointer-events: none;
}

.main-character img {
  width: 250px;
  height: auto;
  filter: drop-shadow(0 0 15px rgba(255, 255, 255, 0.3));
  pointer-events: none;
}

.enemy-character {
  position: absolute;
  right: 10%; 
  bottom: 30%; 
  transform: translateY(0);
  transition: transform 0.3s ease;
  z-index: 10;
  pointer-events: none;
}

.enemy-character img {
  width: 400px; 
  height: auto;
  filter: drop-shadow(0 0 15px rgba(255, 0, 0, 0.3)); 
  pointer-events: none;
}

/* 血条样式  */
.health-bar {
  position: absolute;
  bottom: -40px;
  left: 0;
  width: 100%;
  height: 30px;
  z-index: 10;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  overflow: visible;
}

.health-background {
  position: absolute;
  width: 100%;
  height: 100%;
  background: 
    linear-gradient(to bottom, #1a1a1a, #0d0d0d),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect width="100" height="100" fill="%231a1a1a"/><path d="M0,0 L100,100 M100,0 L0,100" stroke="%23333" stroke-width="1"/></svg>');
  background-size: auto, 20px 20px;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 
    inset 0 0 10px rgba(0, 0, 0, 0.7),
    0 3px 5px rgba(0, 0, 0, 0.5);
}

.metal-frame {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 2px solid #5a3a1a;
  border-radius: 4px;
  box-shadow: 
    inset 0 0 10px rgba(90, 58, 26, 0.5),
    0 0 5px rgba(90, 58, 26, 0.8);
  box-sizing: border-box;
}

.rivets {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8px;
  box-sizing: border-box;
}

/* .rivet {
  width: 6px;
  height: 6px;
  background: #8b7d6b;
  border-radius: 50%;
  box-shadow: 
    0 0 3px rgba(139, 125, 107, 0.8),
    inset 0 0 3px rgba(0, 0, 0, 0.5);
} */

.damage-indicator {
  position: absolute;
  height: 100%;
  background: linear-gradient(to right, 
    rgba(139, 0, 0, 0.7), 
    rgba(100, 0, 0, 0.4),
    rgba(70, 0, 0, 0.2)
  );
  transition: width 0.8s ease-out;
  z-index: 1;
  animation: damagePulse 0.8s infinite alternate;
}

@keyframes damagePulse {
  0% {
    opacity: 0.8;
  }
  100% {
    opacity: 0.4;
  }
}

.health-fill {
  position: absolute;
  height: 100%;
  transition: width 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  z-index: 2;
  overflow: hidden;
}

.main-character .health-fill {
  background: linear-gradient(to bottom, 
    #8b0000, 
    #6b0000,
    #4b0000
  );
  box-shadow: 
    inset 0 0 10px rgba(139, 0, 0, 0.5),
    0 0 5px rgba(139, 0, 0, 0.3);
}

.enemy-character .health-fill {
  background: linear-gradient(to bottom, 
    #8b0000, 
    #6b0000,
    #4b0000
  );
  box-shadow: 
    inset 0 0 10px rgba(139, 0, 0, 0.5),
    0 0 5px rgba(139, 0, 0, 0.3);
}

.blood-texture {
  position: absolute;
  top: 0;
  left: 极0;
  width: 100%;
  height: 100%;
  background: 
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><circle cx="5" cy="5" r="1" fill="%23600" opacity="0.3"/><circle cx="15" cy="10" r="1.5" fill="%23600" opacity="0.3"/><circle cx="10" cy="15" r极="1" fill="%23600" opacity="0.3"/></svg>');
  opacity: 0.5;
}

.health-text {
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: "Medieval", "Courier New", monospace;
  font-weight: bold;
  text-shadow: 
    0 0 3px #000,
    0 0 5px rgba(139, 0, 0, 0.8);
  z-index: 3;
}

.health-value {
  color: #ffd700;
  font-size: 16px;
  letter-spacing: 1px;
}

.health-divider {
  color: #8b7d6b;
  font-size: 14px;
  margin: 0 5px;
}

.health-max {
  color: #8b7d6b;
  font-size: 14px;
}

.blood-drips {
  position: absolute;
  bottom: -10px;
  left: 0;
  width: 100%;
  height: 15px;
  display: flex;
  justify-content: space-around;
  z-index: 4;
}

.blood-drip {
  width: 3px;
  height: 10px;
  background: linear-gradient(to bottom, #8b0000, transparent);
  border-radius: 0 0 2px 2px;
  animation: dripFall 1.5s ease-out forwards;
}

@keyframes dripFall {
  0% {
    height: 0;
    opacity: 0;
  }
  30% {
    height: 10px;
    opacity: 1;
  }
  100% {
    height: 0;
    opacity: 0;
    transform: translateY(15px);
  }
}

/* 受击时血条效果 */
.hit-animation .health-bar .health-fill {
  animation: bloodSplash 0.8s ease;
}

@keyframes bloodSplash {
  0% {
    transform: translateX(0);
  }
  20% {
    transform: translateX(-5px);
    box-shadow: 
      inset 0 0 15px rgba(255, 0, 0, 0.8),
      0 0 10px rgba(255, 0, 0, 0.8);
  }
  40% {
    transform: translateX(5px);
    box-shadow: 
      inset 0 0 20px rgba(255, 0, 0, 1),
      0 0 15px rgba(255, 0, 0, 1);
  }
  60% {
    transform: translateX(-3px);
    box-shadow: 
      inset 0 0 15px rgba(255, 0, 0, 0.8),
      0 0 10px rgba(255, 0, 0, 0.8);
  }
  80% {
    transform: translateX(3px);
  }
  100% {
    transform: translateX(0);
  }
}

@font-face {
  font-family: "Medieval";
  src: url("https://fonts.googleapis.com/css2?family=MedievalSharp&display=swap");
}

.bracket-container {
  position: absolute;
  width: 110%;
  height: 110%;
  top: -5%;
  left: -5%;
  pointer-events: none;
  display: flex;
  justify-content: space-between;
  transition: transform 0.3s ease, opacity 0.3s ease;
  opacity: 0.9;
  z-index: 10000; 
}

/* 基本括号样式 */
.bracket {
  position: relative;
  width: 20px;
  height: 100%;
  opacity: 0;
  transition: opacity 0.3s ease;
}

/* 左括号样式 */
.bracket-left::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  width: 20px;
  height: 50%;
  border: 4px solid #ff0000;
  border-right: none;
  border-radius: 20px 0 0 20px;
  box-shadow: 0 0 15px #ff0000, inset 0 0 10px rgba(255, 0, 0, 0.8);
}

/* 右括号样式 */
.bracket-right::before {
  content: "";
  position: absolute;
  top: 50%;
  right: 0;
  width: 20px;
  height: 50%;
  border: 4px solid #ff0000;
  border-left: none;
  border-radius: 0 20px 20px 0;
  box-shadow: 0 0 15px #ff0000, inset 0 0 10px rgba(255, 0, 0, 0.8);
}

/* 动画效果 */
.show-bracket .bracket {
  opacity: 1;
}

@keyframes bracketPulse {
  0% {
    opacity: 0.7;
    transform: scale(0.95);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0.7;
    transform: scale(0.95);
  }
}

.bracket-container {
  animation: bracketPulse 1.5s infinite ease-in-out;
}

/* 攻击移动效果 */
.attack-animation {
  animation: attackMove 0.5s forwards;
}

@keyframes attackMove {
  0% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(30px);
  }
  100% {
    transform: translateX(0);
  }
}

.enemy-character.attack-animation {
  animation: enemyAttackMove 0.5s forwards;
}

@keyframes enemyAttackMove {
  0% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(-30px);
  }
  100% {
    transform: translateX(0);
  }
}

/* 受击效果 */
.hit-animation {
  animation: hitShake 0.5s forwards;
}

@keyframes hitShake {
  0%,
  100% {
    transform: translateX(0) scale(1);
    filter: brightness(1);
  }
  10%,
  30%,
  50%,
  70%,
  90% {
    transform: translateX(-5px) scale(1.05);
    filter: brightness(1.5);
  }
  20%,
  40%,
  60%,
  80% {
    transform: translateX(5px) scale(1.05);
    filter: brightness(1.5);
  }
}

.hit-particles {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  z-index: 100;
  pointer-events: none;
}

.particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: var(--size, 8px);
  height: var(--size, 8px);
  border-radius: 50%;
  background-color: var(--color, #ff0000);
  animation: explode 1.2s ease-in-out forwards;
  animation-delay: var(--delay, 0s);
  opacity: 0;
  transform: translate(0, 0) scale(0.3);
}

@keyframes explode {
  0% {
    transform: translate(0, 0) scale(0.3);
    opacity: 1;
  }
  20% {
    transform: translate(var(--x), var(--y)) scale(0.7);
    opacity: 0.9;
  }
  50% {
    opacity: 0.7;
  }
  100% {
    transform: translate(var(--x-2), var(--y-2)) scale(0);
    opacity: 0;
  }
}

/* 敌人粒子颜色 */
.enemy-character .hit-particles .particle {
  background-color: var(--color, #8b0000);
}

/* 流血效果 */
.blood-effect {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 99;
  pointer-events: none;
}

.blood-splatter {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 150px;
  height: 150px;
  background: radial-gradient(
    circle,
    rgba(139, 0, 0, 0.8) 0%,
    rgba(139, 0, 0, 0) 70%
  );
  transform: translate(-50%, -50%);
  opacity: 0;
  animation: bloodSplatter 1s ease-out forwards;
}

@keyframes bloodSplatter {
  0% {
    transform: translate(-50%, -50%) scale(0.2);
    opacity: 0.8;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.6;
  }
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0;
  }
}
</style>