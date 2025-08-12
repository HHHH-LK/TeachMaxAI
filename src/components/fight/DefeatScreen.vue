<template>
  <transition name="fade">
    <div class="defeat-screen" v-if="show">
      <!-- 背景 -->
      <div class="background-overlay"></div>

      <div class="fog-animation">
        <div class="fog-layer fog-layer-1"></div>
        <div class="fog-layer fog-layer-2"></div>
        <div class极="fog-layer fog-layer-3"></div>
        <div class="fog-layer fog-layer-4"></div>
      </div>

      <div class="scroll-banner">
        <div class="defeat-text">败北</div>
      </div>

      <div class="character-container">
        <div class="fallen-hero">
          <!-- 根据状态变化 -->
          <img
            :src="currentHumanImage"
            alt="角色"
            class="human-image"
            :class="currentState"
          />
        </div>

        <div class="demon">
          <img :src="enemyImage" class="enemy-image" />
        </div>
      </div>

      <div class="message">
        <div class="scroll-divider"></div>
        <div class="message-text">下次加油……</div>
        <div class="scroll-divider"></div>
      </div>

      <button class="continue-btn" @click="handleContinue">
        <span class="button-text">退出</span>
      </button>
    </div>
  </transition>
</template>

<script setup>
import { defineProps, defineEmits, ref, watch, computed } from "vue";
import humanStandImage from "/Image/Human.png";
import humanDownImage from "/Image/Human_Down.png";
import humanFallImage from "/Image/Human_Fall.png";
import enemyImage from "/Image/Enemy.png";

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["continue"]);

const currentState = ref("stand");

// 计算当前显示的图片
const currentHumanImage = computed(() => {
  switch (currentState.value) {
    case "stand":
      return humanStandImage;
    case "down":
      return humanDownImage;
    case "fall":
      return humanFallImage;
    default:
      return humanStandImage;
  }
});

const handleContinue = () => {
  emit("continue");
};

watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      // 重置状态
      currentState.value = "stand";

      setTimeout(() => {
        setTimeout(() => {
          currentState.value = "down";

          setTimeout(() => {
            currentState.value = "fall";
          }, 1000);
        }, 1000);
      }, 500);
    } else {
      currentState.value = "stand";
    }
  }
);
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.8s ease;
}
.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.defeat-screen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  overflow: hidden;
  font-family: "Medieval", "Times New Roman", serif;
}

/* 纯黑色半透明背景 */
.background-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgb(0, 0, 0); /* 纯黑色，透明度0.7 */
  z-index: -1;
}

/* 淡紫色雾气效果 */
.fog-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.fog-layer {
  position: absolute;
  width: 200%;
  height: 100%;
  background: linear-gradient(
    to right,
    rgba(180, 130, 230, 0.6),
    transparent 70%
  );
  filter: blur(40px);
  opacity: 0.4;
  animation: fog-drift 30s linear infinite;
}

.fog-layer-1 {
  top: 0;
  left: -100%;
  animation: fog-drift 30s linear infinite;
  animation-delay: 0s;
}

.fog-layer-2 {
  top: 20%;
  left: -50%;
  animation: fog-drift 40s linear infinite reverse;
  animation-delay: 5s;
}

.fog-layer-3 {
  top: 40%;
  left: -150%;
  animation: fog-drift 50s linear infinite;
  animation-delay: 10s;
}

.fog-layer-4 {
  top: 60%;
  left: -200%;
  animation: fog-drift 60s linear infinite reverse;
  animation-delay: 15s;
}

@keyframes fog-drift {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(100%);
  }
}

/* 古旧羊皮卷轴横幅  */
.scroll-banner {
  position: relative;
  width: 300px;
  height: 100px;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" opacity="0.3"><rect width="100" height="100" fill="%23f5e9c9" stroke="%23d4b86a" stroke-width="2"/></svg>'),
    linear-gradient(to bottom, #e8d3a0, #c9b07e, #e8d3a0);
  border-radius: 8px;
  margin-top: 80px;
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.7),
    inset 0 0 30px rgba(150, 110, 50, 0.5);
  border: 3px solid #9c7c48;
  z-index: 2;
  animation: scroll-unroll 1.2s ease-out forwards,
    banner-pulse 3s infinite ease-in-out;
  transform-style: preserve-3d;
  perspective: 500px;
}

/* 
.scroll-banner::before,
.scroll-banner::after {
  content: "";
  position: absolute;
  top: 100%;
  width: 50px;
  height: 70%;
  background: linear-gradient(to right, #c9b07e, #b39a7b, #c9b07e);
  border-radius: 50%;
  box-shadow: inset 0 0 20px rgba(100, 70, 20, 0.5), 0 0 10px rgba(0, 0, 0, 0.5);
} */

.scroll-banner::before {
  left: -25px;
}

.scroll-banner::after {
  right: -25px;
}

@keyframes scroll-unroll {
  0% {
    width: 0;
    opacity: 0;
    transform: rotateX(30deg) rotateY(10deg);
  }
  60% {
    width: 300px;
    transform: rotateX(0) rotateY(0);
  }
  100% {
    width: 300px;
    opacity: 1;
  }
}

@keyframes banner-pulse {
  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.7),
      inset 0 0 30px rgba(150, 110, 50, 0.5);
  }
  50% {
    transform: scale(1.02);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.8),
      inset 0 0 35px rgba(150, 110, 50, 0.6);
  }
}

.defeat-text {
  font-size: 60px;
  font-weight: bold;
  color: #6b1a1a;
  text-shadow: 3px 3px 5px rgba(0, 0, 0, 0.5), 0 0 10px rgba(139, 0, 0, 0.8);
  letter-spacing: 6px;
  font-family: "Blackletter", "Old English Text MT", serif;
  transform: translateZ(20px);
  animation: text-glowing 2s infinite alternate;
}

@keyframes text-glowing {
  0% {
    text-shadow: 3px 3px 5px rgba(0, 0, 0, 0.5), 0 0 10px rgba(139, 0, 0, 0.8);
  }
  100% {
    text-shadow: 3px 3px 5px rgba(0, 0, 0, 0.5), 0 0 15px rgba(255, 0, 0, 0.9),
      0 0 20px rgba(139, 0, 0, 0.8);
  }
}

/* 角色容器 */
.character-container {
  display: flex;
  width: 80%;
  max-width: 900px;
  justify-content: space-between;
  margin-bottom: 40px;
  z-index: 2;
  perspective: 1000px;
}

.fallen-hero {
  position: relative;
  width: 280px;
  height: 250px;
  animation: hero-fall 2s ease-out forwards;
  animation-delay: 0.8s;
  opacity: 0;
  filter: drop-shadow(5px 5px 10px rgba(0, 0, 0, 0.7));
}

.human-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: all 0.5s ease;
}

.human-image.stand {
  transform: translateY(0) rotate(0);
}

.human-image.down {
  transform: translateY(30px) rotate(-5deg);
}

.human-image.fall {
  transform: translateY(50px) rotate(-15deg);
}

@keyframes hero-fall {
  0% {
    opacity: 0;
    transform: translateY(150px) rotate(20deg);
  }
  40% {
    opacity: 0.8;
  }
  70% {
    transform: translateY(0) rotate(0);
  }
  100% {
    opacity: 1;
    transform: translateY(0) rotate(0);
  }
}

.demon {
  position: relative;
  width: 320px;
  height: 320px;
  animation: demon-rise 1.5s ease-out forwards;
  animation-delay: 1.2s;
  opacity: 0;
  filter: drop-shadow(5px 5px 10px rgba(139, 0, 0, 0.5));
}

.enemy-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transform: scale(1.1) translateX(20px);
}

@keyframes demon-rise {
  0% {
    opacity: 0;
    transform: translateY(100px) scale(0.8);
  }
  60% {
    transform: translateY(-20px) scale(1.1);
  }
  80% {
    transform: translateY(0) scale(1);
    opacity: 0.8;
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30px;
  margin: 20px 0;
  z-index: 2;
}

.scroll-divider {
  width: 150px;
  height: 3px;
  background: linear-gradient(
    to right,
    transparent,
    rgba(212, 175, 55, 0.8),
    transparent
  );
  box-shadow: 0 0 10px rgba(212, 175, 55, 0.5);
  animation: divider-glow 3s infinite alternate;
}

@keyframes divider-glow {
  0% {
    box-shadow: 0 0 10px rgba(212, 175, 55, 0.5);
  }
  100% {
    box-shadow: 0 0 15px rgba(255, 215, 0, 0.8);
  }
}

.message-text {
  font-size: 42px;
  color: #e0d0a0;
  text-shadow: 0 0 10px rgba(0, 0, 0, 0.7), 0 0 20px rgba(212, 175, 55, 0.5);
  padding: 5px 30px;
  font-style: italic;
  font-family: "KaiTi", sans-serif;
  letter-spacing: 3px;
  animation: text-flicker 5s infinite alternate;
}

@keyframes text-flicker {
  0%,
  18%,
  22%,
  25%,
  53%,
  57%,
  100% {
    text-shadow: 0 0 5px rgba(212, 175, 55, 0.8),
      0 0 10px rgba(212, 175, 55, 0.6), 0 0 20px rgba(212, 175, 55, 0.4);
  }
  20%,
  24%,
  55% {
    text-shadow: none;
  }
}

.continue-btn {
  margin-top: 10px;
  margin-bottom: 20px;
  padding: 18px 60px;
  font-size: 26px;
  color: #e0c070;
  background: linear-gradient(
    to bottom,
    rgba(30, 15, 5, 0.9),
    rgba(15, 5, 0, 0.95),
    rgba(30, 15, 5, 0.9)
  );
  border: 3px solid #8a6d3b;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.9), 0 0 15px rgba(160, 120, 40, 0.5),
    inset 0 0 15px rgba(100, 70, 20, 0.5);
  transition: all 0.3s ease;
  font-family: "Blackletter", "Old English Text MT", serif;
  font-weight: bold;
  z-index: 2;
  position: relative;
  animation: button-appear 1.5s ease-out forwards,
    button-pulse 3s infinite ease-in-out;
  animation-delay: 0s, 2.5s;
  opacity: 0;
  transform-style: preserve-3d;
  perspective: 500px;
  text-transform: uppercase;
  letter-spacing: 3px;
}

.continue-btn::before {
  content: "";
  position: absolute;
  top: 5px;
  left: 5px;
  right: 5px;
  bottom: 5px;
  border: 1px solid rgba(160, 120, 40, 0.5);
  border-radius: 7px;
  pointer-events: none;
}

.button-text {
  position: relative;
  z-index: 2;
  text-shadow: 2px 2px 3px rgba(0, 0, 0, 0.8), 0 0 8px rgba(160, 120, 40, 0.7);
  letter-spacing: 2px;
  animation: text-pulse 2s infinite alternate;
}

@keyframes text-pulse {
  0% {
    text-shadow: 2px 2px 3px rgba(0, 0, 0, 0.8), 0 0 8px rgba(160, 120, 40, 0.7);
  }
  100% {
    text-shadow: 2px 2px 3px rgba(0, 0, 0, 0.8),
      0 0 12px rgba(168, 137, 75, 0.9), 0 0 15px rgba(160, 120, 40, 0.7);
  }
}

@keyframes button-appear {
  0% {
    opacity: 0;
    transform: scale(0.7) rotateX(-15deg);
  }
  70% {
    transform: scale(1.1) rotateX(0);
    opacity: 1;
  }
  100% {
    transform: scale(1) rotateX(0);
    opacity: 1;
  }
}

@keyframes button-pulse {
  0%,
  100% {
    transform: scale(1);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.9), 0 0 15px rgba(160, 120, 40, 0.5),
      inset 0 0 15px rgba(100, 70, 20, 0.5);
  }
  50% {
    transform: scale(1.03);
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.95),
      0 0 20px rgba(180, 140, 60, 0.7), inset 0 0 20px rgba(120, 90, 30, 0.6);
  }
}

.continue-btn:hover {
  transform: translateY(-5px) rotateX(5deg);
  box-shadow: 0 12px 25px rgba(0, 0, 0, 0.8), 0 0 30px rgba(255, 215, 0, 0.7),
    inset 0 0 20px rgba(255, 255, 255, 0.4);
  background: linear-gradient(to bottom, #834242, #6c1c1c, #6b1a1a);
}

.continue-btn:active {
  transform: translateY(2px) scale(0.98);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.6), 0 0 15px rgba(255, 215, 0, 0.3),
    inset 0 0 20px rgba(0, 0, 0, 0.3);
}
</style>
