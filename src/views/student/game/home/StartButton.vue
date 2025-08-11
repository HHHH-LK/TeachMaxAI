<template>
  <div class="start-button-container">
    <div class="start-button" @click="handleStart" :class="{ 'pulsing': !gameStarted }">
      <div class="button-glow"></div>
      <div class="button-content">
        <div class="button-icon">⚔️</div>
        <div class="button-text">
          <span class="main-text">开始闯关</span>
        </div>
      </div>
      <div class="button-particles">
        <div class="particle" v-for="n in 8" :key="n" :style="{ '--delay': n * 0.1 + 's' }"></div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StartButton',
  props: {
    gameStarted: {
      type: Boolean,
      default: false
    }
  },
  emits: ['start-game'],
  
  // 添加错误处理
  errorCaptured(err, vm, info) {
    console.error('StartButton 组件错误:', err, vm, info);
    return false; // 阻止错误继续传播
  },
  
  methods: {
    handleStart() {
      try {
        this.$emit('start-game');
      } catch (error) {
        console.error('开始按钮点击处理失败:', error);
        // 可以在这里添加错误UI反馈
      }
    }
  }
}
</script>

<style lang="less" scoped>
@bg-black: #000;
@dark-red: #8b0000;
@light-red: #ff4500;
@white: #fff;
@gray: #ccc;
@dark-gray: #1a1a1a;
@shadow-red: rgba(255, 69, 0, 0.8);
@shadow-light-red: rgba(255, 69, 0, 0.5);
@border-radius-full: 50%;

.start-button-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 100;
  width: 250px;
  height: 250px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.start-button {
  position: relative;
  width: 100%;
  height: 100%;
  background: rgba(@dark-red, 0.9);
  border: 3px solid @light-red;
  border-radius: @border-radius-full;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 0 30px rgba(@dark-red, 0.8);

  &.pulsing {
    animation: pulse 3s infinite;
  }

  .button-glow {
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(@light-red, 0.3) 0%, transparent 70%);
    border-radius: 50%;
    opacity: 0;
    transition: opacity 0.5s ease;
  }

  .button-content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    color: @white;
    text-shadow: 2px 2px 4px rgba(@bg-black, 0.8);

    .button-icon {
      font-size: 4em;
      margin-bottom: 10px;
      color: @light-red;
      text-shadow: 0 0 15px @shadow-red;
    }

    .button-text {
      text-align: center;

      .main-text {
        font-size: 1.5em;
        font-weight: bold;
        letter-spacing: 2px;
        color: @light-red;
        text-shadow: 0 0 15px @shadow-red;
      }
    }
  }

  .button-particles {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;

    .particle {
      position: absolute;
      background: @light-red;
      border-radius: 50%;
      opacity: 0.5;
      animation: float 2s infinite ease-in-out var(--delay);
      width: 4px;
      height: 4px;
      
      &:nth-child(1) { top: 20%; left: 20%; }
      &:nth-child(2) { top: 20%; right: 20%; }
      &:nth-child(3) { bottom: 20%; left: 20%; }
      &:nth-child(4) { bottom: 20%; right: 20%; }
      &:nth-child(5) { top: 50%; left: 10%; }
      &:nth-child(6) { top: 50%; right: 10%; }
      &:nth-child(7) { left: 50%; top: 10%; }
      &:nth-child(8) { left: 50%; bottom: 10%; }
    }
  }

  &:hover .button-glow {
    opacity: 1;
  }

  &:active {
    transform: scale(0.95);
    box-shadow: 0 5px 15px @shadow-light-red;
  }

  &:not(.pulsing) {
    background: rgba(@dark-gray, 0.9);
    border-color: @gray;
    
    .button-content .button-text .main-text {
      color: @gray;
    }
    
    .button-content .button-icon {
      color: @gray;
    }
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes float {
  0% {
    transform: translateY(0) translateX(0) scale(1) rotate(0deg);
    opacity: 0.3;
  }
  25% {
    transform: translateY(-8px) translateX(3px) scale(1.2) rotate(90deg);
    opacity: 0.6;
  }
  50% {
    transform: translateY(-12px) translateX(6px) scale(0.8) rotate(180deg);
    opacity: 0.8;
  }
  75% {
    transform: translateY(-6px) translateX(2px) scale(1.1) rotate(270deg);
    opacity: 0.5;
  }
  100% {
    transform: translateY(0) translateX(0) scale(1) rotate(360deg);
    opacity: 0.3;
  }
}
</style>
