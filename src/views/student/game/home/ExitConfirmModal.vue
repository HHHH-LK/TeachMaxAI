<template>
  <div v-if="visible" class="exit-confirm-overlay" @click="handleClose">
    <div class="exit-confirm-container" @click.stop>
      <CanvasRenderer 
        :width="500" 
        :height="350" 
        render-type="exit-confirm"
        canvas-class="exit-confirm-canvas"
      />
      <div class="exit-confirm-content">
        <h2 class="exit-confirm-title">⚠️ 退出警告</h2>
        <div class="exit-confirm-message">
          <p>你确定要离开这个神秘的世界吗？</p>
          <p class="warning-text">未保存的进度将会丢失...</p>
        </div>
        <div class="exit-confirm-buttons">
          <button class="confirm-btn confirm-exit" @click="handleConfirm">
            <span class="btn-icon">🚪</span>
            <span class="btn-text">确认退出</span>
          </button>
          <button class="confirm-btn cancel-exit" @click="handleClose">
            <span class="btn-icon">⚔️</span>
            <span class="btn-text">继续游戏</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CanvasRenderer from './CanvasRenderer.vue'

export default {
  name: 'ExitConfirmModal',
  components: {
    CanvasRenderer
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close', 'confirm'],
  methods: {
    handleClose() {
      this.$emit('close');
    },
    handleConfirm() {
      this.$emit('confirm');
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
@border-radius-sm: 6px;
@border-radius-md: 10px;
@border-radius-lg: 15px;

.exit-confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(@bg-black, 0.9);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(5px);
}

.exit-confirm-container {
  position: relative;
  width: 500px;
  min-height: 400px;
  border-radius: @border-radius-md;
  overflow: hidden;
  box-shadow: 0 0 50px rgba(@dark-red, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px;
  padding: 25px;
  background: rgba(@bg-black, 0.9);
}

.exit-confirm-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.exit-confirm-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: @white;
  text-shadow: 2px 2px 4px rgba(@bg-black, 0.8);
  background: rgba(@bg-black, 0.7);
  padding: 50px;

  .exit-confirm-title {
    font-size: 1.8em;
    margin-bottom: 12px;
    color: @light-red;
    text-shadow: 0 0 20px @shadow-red, 2px 2px 4px rgba(@bg-black, 0.9);
    font-weight: bold;
    letter-spacing: 2px;
    font-family: 'Times New Roman', serif;
    padding: 8px 16px;
    border-radius: @border-radius-md;
  }

  .exit-confirm-message {
    font-size: 1em;
    line-height: 1.4;
    margin-bottom: 20px;
    color: @white;
    text-shadow: 2px 2px 4px rgba(@bg-black, 0.9);
    padding: 12px;
    border-radius: @border-radius-md;
  }

  .warning-text {
    color: @light-red;
    font-weight: bold;
    text-shadow: 0 0 10px @shadow-red, 2px 2px 4px rgba(@bg-black, 0.9);
    padding: 5px 10px;
    border-radius: @border-radius-sm;
  }

  .exit-confirm-buttons {
    display: flex;
    justify-content: space-around;
    gap: 15px;
  }

  .confirm-btn {
    padding: 12px 25px;
    border: none;
    border-radius: @border-radius-lg;
    font-size: 1.1em;
    font-weight: bold;
    cursor: pointer;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    text-shadow: 1px 1px 2px rgba(@bg-black, 0.8);

    &.confirm-exit {
      background: @light-red;
      color: @white;
      border: 2px solid @light-red;
      box-shadow: 0 5px 15px @shadow-light-red;

      &:hover {
        background: @dark-red;
        border-color: @dark-red;
        box-shadow: 0 8px 20px @shadow-light-red;
      }
    }

    &.cancel-exit {
      background: rgba(@dark-gray, 0.9);
      color: @gray;
      border: 2px solid @gray;
      box-shadow: 0 5px 15px rgba(@bg-black, 0.5);

      &:hover {
        background: rgba(@dark-gray, 0.7);
        border-color: @gray;
        box-shadow: 0 8px 20px rgba(@bg-black, 0.5);
      }
    }

    .btn-icon {
      font-size: 1.2em;
    }

    .btn-text {
      font-family: 'Arial', sans-serif;
    }
  }
}

// 响应式设计
@media (max-width: 900px) {
  .exit-confirm-container {
    width: 90vw;
    height: 60vh;
    padding: 20px;
  }

  .exit-confirm-content {
    .exit-confirm-title {
      font-size: 1.5em;
      margin-bottom: 10px;
    }

    .exit-confirm-message {
      font-size: 1em;
      margin-bottom: 20px;
    }

    .exit-confirm-buttons {
      flex-direction: column;
      gap: 15px;
    }

    .confirm-btn {
      padding: 10px 20px;
      font-size: 1em;
    }
  }
}
</style>
