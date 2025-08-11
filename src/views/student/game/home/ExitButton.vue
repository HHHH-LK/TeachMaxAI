<template>
  <div class="exit-game" @click="handleExit">
    <div class="exit-icon">🚪</div>
    <span class="exit-text">退出</span>
  </div>
</template>

<script>
export default {
  name: 'ExitButton',
  emits: ['exit-game'],
  
  // 添加错误处理
  errorCaptured(err, vm, info) {
    console.error('ExitButton 组件错误:', err, vm, info);
    return false; // 阻止错误继续传播
  },
  
  methods: {
    handleExit() {
      try {
        this.$emit('exit-game');
      } catch (error) {
        console.error('退出按钮点击处理失败:', error);
        // 可以在这里添加错误UI反馈
      }
    }
  }
}
</script>

<style lang="less" scoped>
@bg-black: #000;
@light-red: #ff4500;
@white: #fff;
@shadow-red: rgba(255, 69, 0, 0.8);

.exit-game {
  position: absolute;
  bottom: 20px;
  left: 20px;
  z-index: 10;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  color: @white;
  text-shadow: 2px 2px 4px rgba(@bg-black, 0.8);
  font-size: 1.2em;
  font-weight: bold;
  letter-spacing: 1px;

  .exit-icon {
    font-size: 1.5em;
    color: @light-red;
    text-shadow: 0 0 15px @shadow-red;
  }

  .exit-text {
    font-family: 'Arial', sans-serif;
  }

  &:hover {
    transform: scale(1.05);
    text-shadow: 3px 3px 6px rgba(@light-red, 0.8);
    
    .exit-icon {
      text-shadow: 0 0 20px @shadow-red;
    }
  }

  &:active {
    transform: scale(0.95);
    box-shadow: 0 5px 15px rgba(255, 69, 0, 0.5);
  }
}
</style>
