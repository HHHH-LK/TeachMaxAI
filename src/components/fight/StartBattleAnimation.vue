<template>
  <transition name="fade">
    <div class="start-battle-animation" v-if="show">
      <!-- 背景元素 -->
      <div class="background-overlay"></div>
      
      <!-- 粒子效果 -->
      <div class="particles">
        <div
          v-for="(particle, index) in particles"
          :key="index"
          class="particle"
          :style="particle.style"
        ></div>
      </div>

      <!-- 主要动画内容 -->
      <div class="animation-container">
        <div class="battle-text">开始战斗</div>
        <!-- 双剑交叉效果 -->
        <div class="crossed-swords">
          <div class="sword sword-left"></div>
          <div class="sword sword-right"></div>
          <!-- 火花粒子 -->
          <div class="spark-container">
            <div 
              class="spark" 
              v-for="(spark, index) in sparks" 
              :key="'spark-'+index"
              :style="spark.style"
            ></div>
          </div>
        </div>
      </div>
      
      <!-- 能量光环效果 -->
      <div class="energy-rings">
        <div class="ring ring-1"></div>
        <div class="ring ring-2"></div>
        <div class="ring ring-3"></div>
      </div>
      
      <!-- 跳过按钮 -->
      <button class="skip-button" @click="skipAnimation">
        跳过动画
      </button>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import gsap from "gsap";

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  duration: {
    type: Number,
    default: 3500, // 默认3.5秒
  },
});

const emit = defineEmits(["animation-end"]);

// 粒子效果
const particles = ref([]);
// 火花效果
const sparks = ref([]);
// 动画结束定时器
const animationTimer = ref(null);

// 生成粒子
const generateParticles = () => {
  const newParticles = [];
  for (let i = 0; i < 100; i++) {
    newParticles.push({
      style: {
        top: `${Math.random() * 100}%`,
        left: `${Math.random() * 100}%`,
        width: `${Math.random() * 8 + 4}px`,
        height: `${Math.random() * 8 + 4}px`,
        opacity: Math.random() * 0.5 + 0.2,
        background: `rgba(${Math.floor(Math.random() * 100)}, ${Math.floor(Math.random() * 100)}, ${Math.floor(Math.random() * 255)}, 0.8)`,
      },
    });
  }
  particles.value = newParticles;
};

// 生成火花
const generateSparks = () => {
  const newSparks = [];
  for (let i = 0; i < 25; i++) {
    // 随机角度和距离
    const angle = Math.random() * Math.PI * 2;
    const distance = 20 + Math.random() * 60;
    
    // 随机大小
    const size = 3 + Math.random() * 8;
    
    // 随机颜色（黄色到橙色）
    const hue = 40 + Math.random() * 20;
    
    newSparks.push({
      style: {
        '--spark-distance': `${distance}px`,
        '--spark-angle': `${angle}rad`,
        '--spark-size': `${size}px`,
        '--spark-hue': `${hue}`
      }
    });
  }
  sparks.value = newSparks;
};

// 启动所有动画
const startAnimations = () => {
  // 容器出现动画
  gsap.fromTo('.animation-container', 
    { opacity: 0, scale: 0.8 },
    { 
      opacity: 1, 
      scale: 1, 
      duration: 1.2, 
      ease: "elastic.out(1, 0.75)" 
    }
  );
  
  // 文字脉动动画
  gsap.to('.battle-text', {
    scale: 1.05,
    rotate: -1,
    duration: 1.5,
    repeat: -1,
    yoyo: true,
    ease: "sine.inOut"
  });
  
  // 文字发光动画
  gsap.to('.battle-text', {
    keyframes: [
      {textShadow: "0 0 10px rgba(200, 0, 0, 0.8), 0 0 20px rgba(160, 0, 0, 0.6), 0 0 30px rgba(120, 0, 0, 0.4)"},
      {textShadow: "0 0 20px rgba(220, 0, 0, 1), 0 0 40px rgba(180, 0, 0, 0.8), 0 0 60px rgba(140, 0, 0, 0.6)"}
    ],
    duration: 2,
    repeat: -1,
    yoyo: true
  });
  
  // 粒子浮动动画
  particles.value.forEach((particle, i) => {
    const el = document.querySelectorAll('.particle')[i];
    if (el) {
      gsap.to(el, {
        y: -20,
        x: 10,
        duration: 3 + Math.random() * 3,
        repeat: -1,
        yoyo: true,
        delay: Math.random() * 2,
        ease: "sine.inOut"
      });
    }
  });
  
  // 双剑震动动画
  gsap.to('.sword-left', {
    keyframes: [
      {x: 1, y: -1, rotate: 46, duration: 0.5},
      {x: 3, y: -3, rotate: 50, duration: 0.5, 
       boxShadow: "0 0 25px rgba(212, 175, 55, 0.8), 0 0 35px rgba(255, 100, 0, 0.6)",
       filter: "brightness(1.3) contrast(1.2)"},
      {x: 1, y: -1, rotate: 46, duration: 0.5},
      {x: 0, y: 0, rotate: 45, duration: 0.5}
    ],
    repeat: -1,
    ease: "sine.inOut"
  });
  
  gsap.to('.sword-right', {
    keyframes: [
      {x: -1, y: 1, rotate: -46, duration: 0.5},
      {x: -3, y: 3, rotate: -50, duration: 0.5, 
       boxShadow: "0 0 25px rgba(212, 175, 55, 0.8), 0 0 35px rgba(255, 100, 0, 0.6)",
       filter: "brightness(1.3) contrast(1.2)"},
      {x: -1, y: 1, rotate: -46, duration: 0.5},
      {x: 0, y: 0, rotate: -45, duration: 0.5}
    ],
    repeat: -1,
    ease: "sine.inOut",
    delay: 0.25
  });
  
  // 剑身光效
  gsap.to('.sword::after', {
    x: "100%",
    duration: 2,
    repeat: -1,
    ease: "none"
  });
  
  // 火花动画
  sparks.value.forEach((spark, i) => {
    const el = document.querySelectorAll('.spark')[i];
    if (el) {
      const angle = parseFloat(spark.style['--spark-angle']);
      const distance = parseFloat(spark.style['--spark-distance']);
      const size = parseFloat(spark.style['--spark-size']);
      
      gsap.to(el, {
        opacity: 1,
        duration: 0.2,
        delay: Math.random() * 0.5
      });
      
      gsap.to(el, {
        x: Math.cos(angle) * distance * 1.5,
        y: Math.sin(angle) * distance * 1.5,
        width: size * 1.8,
        height: size * 1.8,
        duration: 0.8,
        delay: Math.random() * 0.5,
        onComplete: () => {
          gsap.to(el, {
            opacity: 0,
            width: size * 0.2,
            height: size * 0.2,
            duration: 0.4
          });
        }
      });
    }
  });
  
  // 能量光环动画
  gsap.to('.ring-1', {
    scale: 1.5,
    opacity: 0.8,
    duration: 3,
    repeat: -1,
    yoyo: true,
    ease: "sine.inOut"
  });
  
  gsap.to('.ring-2', {
    scale: 1.8,
    opacity: 0.6,
    duration: 3.5,
    repeat: -1,
    yoyo: true,
    ease: "sine.inOut",
    delay: 0.5
  });
  
  gsap.to('.ring-3', {
    scale: 2.1,
    opacity: 0.4,
    duration: 4,
    repeat: -1,
    yoyo: true,
    ease: "sine.inOut",
    delay: 1
  });
};

const startAnimationTimer = () => {
  // 清除之前的定时器
  if (animationTimer.value) {
    clearTimeout(animationTimer.value);
  }

  // 设置新的定时器
  animationTimer.value = setTimeout(() => {
    emit("animation-end");
  }, props.duration);
};

// 跳过动画
const skipAnimation = () => {
  emit("animation-end");
};

// 监听show属性变化
watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      generateParticles();
      generateSparks();
      startAnimationTimer();
      
      // 使用nextTick确保DOM已更新
      setTimeout(() => {
        startAnimations();
      }, 50);
    }
  },
  { immediate: true }
);

// 组件挂载时启动计时器
onMounted(() => {
  if (props.show) {
    startAnimationTimer();
  }
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@700&family=Press+Start+2P&display=swap');

.start-battle-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 20000;
  display: flex;
  justify-content: center;
  align-items: center;
  pointer-events: none;
  overflow: hidden;
  background: radial-gradient(circle at center, #0a050f 0%, #000000 100%);
}

.background-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    radial-gradient(circle at center, rgba(26, 10, 26, 0.8) 0%, rgba(0, 0, 0, 0) 70%),
    linear-gradient(135deg, #1a0a1a 0%, #0a050f 100%);
  backdrop-filter: blur(5px);
  z-index: 1;
}

.particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 2;
}

.particle {
  position: absolute;
  border-radius: 50%;
  filter: blur(1px);
  box-shadow: 0 0 10px rgba(100, 200, 255, 0.5);
  background: radial-gradient(circle, rgba(100, 200, 255, 0.8), rgba(50, 150, 255, 0.6));
}

.animation-container {
  position: relative;
  z-index: 4;
  text-align: center;
  opacity: 0;
  transform: scale(0.8);
}

.battle-text {
  font-size: 82px;
  font-weight: bold;
  color: #ff0055;
  text-shadow: 
    0 0 20px rgba(255, 0, 85, 0.8),
    0 0 40px rgba(200, 0, 100, 0.6), 
    0 0 60px rgba(180, 0, 120, 0.4);
  letter-spacing: 8px;
  font-family: 'Orbitron', sans-serif;
  position: relative;
  text-transform: uppercase;
  animation: textGlow 2s infinite alternate;
}

@keyframes textGlow {
  0% {
    text-shadow: 
      0 0 10px rgba(255, 0, 85, 0.8),
      0 0 20px rgba(200, 0, 100, 0.6);
  }
  100% {
    text-shadow: 
      0 0 30px rgba(255, 0, 85, 1),
      0 0 60px rgba(200, 0, 100, 0.8),
      0 0 90px rgba(180, 0, 120, 0.6);
  }
}

.battle-text::after {
  content: "";
  position: absolute;
  bottom: -15px;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  height: 3px;
  background: linear-gradient(to right, transparent, #ff0055, transparent);
  box-shadow: 0 0 10px rgba(255, 0, 85, 0.8);
  filter: blur(1px);
}

/* 双剑交叉效果 */
.crossed-swords {
  position: relative;
  width: 300px;
  height: 300px;
  margin: 30px auto;
  perspective: 1000px;
}

.sword {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 220px;
  height: 25px;
  background: 
    linear-gradient(
      to right,
      rgba(200, 200, 220, 0.9),
      rgba(230, 230, 255, 1),
      rgba(200, 200, 220, 0.9)
    );
  border-radius: 4px;
  box-shadow: 
    0 0 25px rgba(135, 121, 34, 0.8),
    inset 0 0 15px rgba(255, 255, 255, 0.5);
  transform-origin: center;
  z-index: 10;
  filter: drop-shadow(0 0 10px rgba(100, 200, 255, 0.8));
}

.sword-left {
  transform: translate(-50%, -50%) rotate(45deg);
}

.sword-right {
  transform: translate(-50%, -50%) rotate(-45deg);
}

.sword::before {
  content: "";
  position: absolute;
  top: 50%;
  left: 0;
  width: 45px;
  height: 45px;
  background: 
    radial-gradient(
      circle at center,
      rgba(100, 200, 255, 0.9),
      rgba(50, 150, 255, 0.8)
    );
  border-radius: 50%;
  transform: translateY(-50%);
  box-shadow: 
    0 0 15px rgba(100, 200, 255, 0.8),
    inset 0 0 8px rgba(255, 255, 255, 0.5);
}

.sword::after {
  content: "";
  position: absolute;
  top: 50%;
  right: 0;
  width: 35px;
  height: 35px;
  background: 
    radial-gradient(
      circle at center,
      rgba(100, 200, 255, 0.9),
      rgba(50, 150, 255, 0.8)
    );
  border-radius: 50%;
  transform: translateY(-50%);
  box-shadow: 
    0 0 10px rgba(100, 200, 255, 0.8),
    inset 0 0 6px rgba(255, 255, 255, 0.5);
}

/* 剑身动态光效 */
.sword::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    linear-gradient(
      to right,
      transparent,
      rgba(150, 200, 255, 0.5),
      transparent
    );
  pointer-events: none;
}

/* 火花粒子效果 */
.spark-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  z-index: 5;
  pointer-events: none;
}

.spark {
  position: absolute;
  width: var(--spark-size);
  height: var(--spark-size);
  border-radius: 50%;
  background: radial-gradient(circle, 
    hsl(var(--spark-hue), 100%, 80%), 
    hsl(var(--spark-hue), 100%, 50%)
  );
  box-shadow: 0 0 15px hsl(var(--spark-hue), 100%, 70%);
  opacity: 0;
  filter: blur(1px);
}

/* 能量光环效果 */
.energy-rings {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400px;
  height: 400px;
  z-index: 3;
  pointer-events: none;
}

.ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid transparent;
  opacity: 0.5;
}

.ring-1 {
  border-color: rgba(255, 227, 100, 0.6);
  box-shadow: 
    0 0 20px rgba(100, 200, 255, 0.4),
    inset 0 0 20px rgba(100, 200, 255, 0.4);
  filter: blur(1px);
}

.ring-2 {
  border-color: rgba(150, 100, 255, 0.5);
  box-shadow: 
    0 0 30px rgba(150, 100, 255, 0.3),
    inset 0 0 30px rgba(150, 100, 255, 0.3);
  filter: blur(2px);
}

.ring-3 {
  border-color: rgba(255, 100, 200, 0.4);
  box-shadow: 
    0 0 40px rgba(255, 100, 200, 0.2),
    inset 0 0 40px rgba(255, 100, 200, 0.2);
  filter: blur(3px);
}

/* 跳过按钮样式 */
.skip-button {
  position: absolute;
  bottom: 40px;
  right: 40px;
  padding: 12px 30px;
  background: linear-gradient(to bottom, #8b4513, #5d2906);
  color: #ffd700;
  border: 2px solid #d4af37;
  border-radius: 8px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  text-shadow: 1px 1px 2px #000;
  box-shadow: 
    0 5px 0 #5d2906, 
    inset 0 0 15px rgba(255, 215, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 100;
  pointer-events: auto;
  font-family: 'Orbitron', sans-serif;
}

.skip-button:hover {
  background: linear-gradient(to bottom, #a0522d, #8b4513);
  transform: translateY(-3px);
  box-shadow: 
    0 8px 0 #5d2906, 
    inset 0 0 20px rgba(255, 215, 0, 0.5);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 1.2s cubic-bezier(0.2,0.8,0.2,1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 无障碍优化 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation: none !important;
    transition: none !important;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .battle-text {
    font-size: 48px;
    letter-spacing: 4px;
  }
  
  .crossed-swords {
    width: 200px;
    height: 200px;
  }
  
  .sword {
    width: 160px;
    height: 20px;
  }
  
  .sword::before {
    width: 35px;
    height: 35px;
  }
  
  .sword::after {
    width: 28px;
    height: 28px;
  }
  
  .energy-rings {
    width: 300px;
    height: 300px;
  }
  
  .skip-button {
    padding: 10px 20px;
    font-size: 16px;
    bottom: 20px;
    right: 20px;
  }
}
</style>