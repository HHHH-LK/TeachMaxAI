<template>
  <div class="exp-progress-overlay">
    <div class="exp-progress-container">
      <!-- 等级显示 -->
      <div class="level-display">
        <span class="level-text">Lv.{{ currentLevel }}</span>
        <div class="level-up-indicator" v-if="showLevelUp">
          <span class="level-up-text">升级!</span>
          <div class="level-up-flames"></div>
        </div>
      </div>
      
      <!-- 经验进度条 -->
      <div class="progress-bar">
        <div 
          class="progress-fill" 
          :style="{
            width: `${displayProgress}%`,
            transition: transitionEnabled ? 'width 0.8s ease-in-out' : 'none'
          }"
        >
          <div class="lava-effect"></div>
        </div>
        <div class="rune-overlay"></div>
      </div>
      
      <!-- 经验数值 -->
      <div class="exp-text">
        <span class="current-exp">{{ displayCurrentExp }}</span>
        <span class="exp-divider">/</span>
        <span class="max-exp">{{ expToNextLevel }}</span>
      </div>
      
      <!-- 经验增加动画 -->
      <div 
        v-for="(exp, index) in expAnimations" 
        :key="index"
        class="exp-gain-animation"
        :style="{
          top: `${exp.position.y}px`,
          left: `${exp.position.x}px`,
          opacity: exp.opacity
        }"
      >
        +{{ exp.value }}
      </div>
      
      <!-- 背景符文 -->
      <div class="background-runes">
        <div class="rune rune-1">ᛉ</div>
        <div class="rune rune-2">ᚢ</div>
        <div class="rune rune-3">ᛏ</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue';

const props = defineProps({
  currentExp: {
    type: Number,
    default: 0,
    required: true
  },
  expToNextLevel: {
    type: Number,
    default: 100,
    required: true
  },
  currentLevel: {
    type: Number,
    default: 1,
    required: true
  },
  expGain: {
    type: Number,
    default: 0
  },
  usedExp: {
    type: Number,
    default: 0
  },
  visible: {
    type: Boolean,
    default: false,
    required: true
  }
});

const emit = defineEmits(['level-up']);

// 响应式状态
const animatedExp = ref(props.currentExp);
const showLevelUp = ref(false);
const expAnimations = ref([]);
const transitionEnabled = ref(false); // 默认禁用过渡
const isLevelingUp = ref(false); // 标记是否正在升级
const progressAnimation = ref(null); // 存储动画计时器
const isOverflowing = ref(false); // 标记是否正在溢出动画

// 计算属性
const displayProgress = computed(() => {
  return Math.min(100, (animatedExp.value / props.expToNextLevel) * 100);
});

const displayCurrentExp = computed(() => animatedExp.value);

// 重置动画状态
const resetAnimationState = () => {
  console.log("重置动画状态");
  expAnimations.value = [];
  showLevelUp.value = false;
  transitionEnabled.value = false;
  isLevelingUp.value = false;
  isOverflowing.value = false;
  
  // 清除所有动画计时器
  if (progressAnimation.value) {
    clearTimeout(progressAnimation.value);
    progressAnimation.value = null;
  }
};

// 更新经验数据
const updateExpData = () => {
  console.log("更新经验数据:", props.currentExp);
  animatedExp.value = props.currentExp;
};

// 添加经验增加动画
const addExpGainAnimation = (expValue) => {
  console.log("添加经验增加动画:", expValue);
  
  // 随机位置
  const position = {
    x: Math.random() * 100,
    y: Math.random() * 30 - 15
  };
  
  const animation = {
    value: expValue,
    position: position,
    opacity: 1,
    id: Date.now() + Math.random()
  };
  
  expAnimations.value.push(animation);
  
  // 动画结束后移除
  progressAnimation.value = setTimeout(() => {
    expAnimations.value = expAnimations.value.filter(a => a.id !== animation.id);
  }, 3000);
};

// 触发经验增加动画
const triggerExpGainAnimation = () => {
  console.log("触发经验增加动画");
  
  // 添加经验增加动画
  addExpGainAnimation(props.expGain);
  
  // 启用CSS过渡
  transitionEnabled.value = true;
  animatedExp.value = props.currentExp;
  
  // 检查是否升级
  if (props.currentExp >= props.expToNextLevel) {
    handleLevelUp();
  }
  
  // 动画结束后禁用过渡
  progressAnimation.value = setTimeout(() => {
    transitionEnabled.value = false;
  }, 1000);
};

// 处理升级
const handleLevelUp = () => {
  console.log("处理升级");
  showLevelUp.value = true;
  emit('level-up');
  
  // 播放升级动画
  progressAnimation.value = setTimeout(() => {
    showLevelUp.value = false;
  }, 5000);
};

// 核心更新：监听visible变化，当变为true时更新经验数据
watch(() => props.visible, (newVal) => {
  console.log("组件可见状态变化:", newVal);
  
  if (newVal) {
    // 当visible变为true时，重置动画状态并更新经验数据
    resetAnimationState();
    updateExpData();
    
    // 如果有经验增加，触发动画
    if (props.expGain > 0) {
      triggerExpGainAnimation();
    }
  }
}, { immediate: true });

// 监听经验值变化
watch(() => props.currentExp, (newVal, oldVal) => {
  console.log("经验值变化监听触发", newVal, oldVal);
  
  if (!props.visible) {
    console.log("组件不可见，不处理经验变化");
    return;
  }
  
  if (newVal > oldVal) {
    // 计算经验增加值
    const expGain = newVal - oldVal;
    console.log("经验增加:", expGain);
    
    // 添加经验增加动画
    addExpGainAnimation(expGain);
    
    // 启用CSS过渡
    transitionEnabled.value = true;
    animatedExp.value = newVal;
    
    // 检查是否升级
    if (newVal >= props.expToNextLevel) {
      handleLevelUp();
    }
    
    // 动画结束后禁用过渡
    progressAnimation.value = setTimeout(() => {
      transitionEnabled.value = false;
    }, 2000);
  } else if (newVal < oldVal) {
    // 经验值减少，说明角色升级
    console.log("经验减少（升级）:", oldVal - newVal);
    
    // 标记正在升级
    isLevelingUp.value = true;
    
    // 1. 先将进度条动画到100%（满级）
    transitionEnabled.value = true;
    animatedExp.value = props.expToNextLevel;
    
    // 触发升级事件
    handleLevelUp();
    
    // 2. 等待进度条满级动画完成
    progressAnimation.value = setTimeout(() => {
      // 3. 创建溢出效果
      // isOverflowing.value = true;
      
      // 4. 动画到120%（溢出效果）
      transitionEnabled.value = true;
      animatedExp.value = props.expToNextLevel * 1;
      
      // 5. 等待溢出动画完成
      progressAnimation.value = setTimeout(() => {
        // 6. 重置进度条为0（禁用过渡）
        transitionEnabled.value = false;
        animatedExp.value = 0;
        isOverflowing.value = false;
        
        // 7. 等待DOM更新后，再动画到新经验值
        nextTick(() => {
          // 8. 启用过渡效果
          transitionEnabled.value = true;
          
          // 9. 动画到新经验值
          animatedExp.value = newVal;
          
          // 动画结束后禁用过渡
          progressAnimation.value = setTimeout(() => {
            transitionEnabled.value = false;
            isLevelingUp.value = false;
          }, 1000);
        });
      }, 500); // 溢出动画时间0.5秒
    }, 1000); // 满级动画时间1秒
  }
});

// 监听经验增加值变化
watch(() => props.expGain, (newVal) => {
  if (props.visible && newVal > 0 && !isLevelingUp.value) {
    console.log("检测到经验增加值:", newVal);
    triggerExpGainAnimation();
  }
});

// 初始化
onMounted(() => {
  console.log("组件挂载");
  if (props.visible) {
    updateExpData();
  }
});
</script>

<style scoped>
/* 原有样式保持不变 */
.exp-progress-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.85);
  z-index: 10000;
  backdrop-filter: blur(5px);
}

.exp-progress-container {
  position: relative;
  width: 80%;
  max-width: 600px;
  background: linear-gradient(to bottom, #1a0a0a, #0d0404);
  border: 2px solid #5a3e36;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 0 25px rgba(180, 60, 0, 0.4);
  overflow: hidden;
  z-index: 10;
}

.exp-progress-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    radial-gradient(circle at 20% 30%, rgba(180, 60, 0, 0.2) 0%, transparent 60%),
    radial-gradient(circle at 80% 70%, rgba(100, 40, 120, 0.2) 0%, transparent 60%);
  z-index: -1;
}

.level-display {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-bottom: 25px;
  position: relative;
}

.level-text {
  font-size: 2.2rem;
  color: #d4af37;
  text-shadow: 0 0 10px rgba(180, 150, 0, 0.7);
  font-family: 'Cinzel', serif;
  letter-spacing: 2px;
}

.level-up-indicator {
  position: relative;
  animation: levelUpPulse 1.5s ease-in-out;
}

.level-up-text {
  font-size: 1.8rem;
  font-weight: bold;
  color: #ff5722;
  background-color: rgba(255, 87, 34, 0.1);
  padding: 5px 15px;
  border-radius: 20px;
  animation: textPulse 1s infinite;
  font-family: 'Cinzel', serif;
  position: relative;
  z-index: 2;
}

.level-up-flames {
  position: absolute;
  top: -10px;
  left: 0;
  width: 100%;
  height: 40px;
  background: radial-gradient(circle, rgba(255, 100, 0, 0.8) 0%, transparent 70%);
  filter: blur(5px);
  animation: flamePulse 1.5s infinite alternate;
  z-index: 1;
}

.progress-bar {
  background: rgba(30, 20, 30, 0.7);
  border: 1px solid #5a3e36;
  border-radius: 10px;
  height: 30px;
  margin: 25px 0;
  position: relative;
  overflow: hidden;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.5);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #8b0000, #ff4500, #ff8c00);
  position: relative;
  transition: width 0.8s ease-in-out;
}

.lava-effect {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(255, 255, 255, 0.3) 50%, 
    transparent 100%);
  animation: lavaFlow 3s infinite linear;
}

.rune-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(circle at 20% 50%, rgba(180, 150, 0, 0.1) 0%, transparent 20%),
    radial-gradient(circle at 80% 50%, rgba(180, 150, 0, 0.1) 0%, transparent 20%);
  pointer-events: none;
}

.exp-text {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  font-size: 1.4rem;
  color: #c0a050;
  text-shadow: 0 0 8px rgba(180, 150, 0, 0.5);
  font-family: 'Cinzel', serif;
  margin-top: 20px;
}

.current-exp {
  color: #ff8c00;
  font-weight: bold;
}

.max-exp {
  color: #d4af37;
}

.exp-divider {
  color: #8b4513;
}

.exp-gain-animation {
  position: absolute;
  color: #ff8c00;
  font-weight: bold;
  font-size: 1.4rem;
  text-shadow: 0 0 5px rgba(255, 140, 0, 0.8);
  animation: expGain 1.5s ease-out forwards;
  pointer-events: none;
  z-index: 10;
  font-family: 'Cinzel', serif;
}

.background-runes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  opacity: 0.2;
}

.rune {
  position: absolute;
  font-size: 3rem;
  color: rgba(180, 150, 0, 0.3);
  font-family: 'Runic', sans-serif;
  animation: runeFloat 10s infinite linear;
}

.rune-1 {
  top: 20%;
  left: 15%;
  animation-delay: 0s;
}

.rune-2 {
  top: 60%;
  left: 70%;
  animation-delay: -3s;
}

.rune-3 {
  top: 40%;
  left: 40%;
  animation-delay: -6s;
}

@keyframes expGain {
  0% {
    transform: translateY(0);
    opacity: 1;
    text-shadow: 0 0 10px rgba(255, 140, 0, 1);
  }
  100% {
    transform: translateY(-50px);
    opacity: 0;
    text-shadow: 0 0 20px rgba(255, 140, 0, 0);
  }
}

@keyframes levelUpPulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes textPulse {
  0%, 100% {
    opacity: 0.8;
    text-shadow: 0 0 5px rgba(255, 87, 34, 0.8);
  }
  50% {
    opacity: 1;
    text-shadow: 0 0 15px rgba(255, 87, 34, 1);
  }
}

@keyframes flamePulse {
  0% {
    opacity: 0.6;
    transform: scale(1);
  }
  100% {
    opacity: 1;
    transform: scale(1.2);
  }
}

@keyframes lavaFlow {
  0% {
    background-position: -100% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

@keyframes runeFloat {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.2;
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
    opacity: 0.4;
  }
  100% {
    transform: translateY(0) rotate(360deg);
    opacity: 0.2;
  }
}
</style>