<template>
  <div v-if="visible" class="profile-overlay" @click="handleClose">
    <div class="profile-container" @click.stop>
      <CanvasRenderer
          :width="600"
          :height="550"
          render-type="profile"
          canvas-class="profile-canvas"
      />
      <div class="profile-content">
        <h2 class="profile-title">角色信息</h2>
        <div class="profile-info">
          <div class="info-item" v-for="(value, key) in userProfile" :key="key">
            <span class="info-label">{{ getLabel(key) }}</span>
            <span class="info-value">{{ value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import CanvasRenderer from './CanvasRenderer.vue'

// 定义组件名称
defineOptions({
  name: 'ProfileModal'
})

// 定义props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  userProfile: {
    type: Object,
    default: () => ({
      nickname: '牢大',
      level: 2,
      totalExp: 120,
      towerLevel: 23,
      hp: 100,
      attack: 50,
      defense: 50,
    })
  }
})

// 定义emits
const emit = defineEmits(['close'])

// 处理关闭事件
const handleClose = () => {
  emit('close')
}

// 获取标签映射
const getLabel = (key) => {
  const labelMap = {
    nickname: '昵称',
    level: '等级',
    totalExp: '总经验',
    towerLevel: '总塔数',
    hp: '生命值',
    attack: '攻击力',
    defense: '防御力'
  }
  return labelMap[key] || key
}
</script>

<style lang="less" scoped>
@bg-black: #000;
@white: #fff;
@border-radius-md: 10px;
@border-radius-lg: 15px;
@border-radius-sm: 4px;

.profile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(@bg-black, 0.8);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(5px);
}

.profile-container {
  position: relative;
  width: 600px;
  height: 550px;
  border-radius: @border-radius-md;
  overflow: hidden;
  box-shadow: 0 0 50px rgba(139, 69, 19, 0.8);

  .profile-canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
  }

  .profile-content {
    position: relative;
    z-index: 2;
    padding: 50px;
    color: @white;
    text-shadow: 2px 2px 4px rgba(@bg-black, 0.8);
    background: rgba(@bg-black, 0.7);
    border-radius: @border-radius-md;
    border: 2px solid rgba(218, 165, 32, 0.5);

    .profile-title {
      text-align: center;
      font-size: 2.5em;
      margin-bottom: 40px;
      color: #daa520;
      text-shadow: 0 0 20px rgba(218, 165, 32, 0.8), 2px 2px 4px rgba(@bg-black, 0.9);
      font-weight: bold;
      letter-spacing: 3px;
      font-family: 'Times New Roman', serif;
      background: rgba(@bg-black, 0.8);
      padding: 15px 30px;
      border-radius: @border-radius-md;
      border: 1px solid rgba(218, 165, 32, 0.3);
    }

    .profile-info {
      display: flex;
      flex-direction: column;
      gap: 25px;
      max-height: 300px;
      overflow-y: auto;
      padding-right: 10px;

      // 自定义滚动条样式
      &::-webkit-scrollbar {
        width: 8px;
      }
      &::-webkit-scrollbar-track {
        background: rgba(139, 69, 19, 0.3);
        border-radius: @border-radius-sm;
      }
      &::-webkit-scrollbar-thumb {
        background: #cd853f;
        border-radius: @border-radius-sm;

        &:hover {
          background: #daa520;
        }
      }

      .info-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 15px 20px;
        background: rgba(139, 69, 19, 0.6);
        border: 2px solid #cd853f;
        border-radius: @border-radius-lg;
        transition: all 0.3s ease;
        box-shadow: 0 2px 8px rgba(@bg-black, 0.5);

        &:hover {
          background: rgba(139, 69, 19, 0.8);
          border-color: #daa520;
          transform: translateX(5px);
          box-shadow: 0 5px 15px rgba(218, 165, 32, 0.5);
        }

        .info-label {
          font-size: 1.2em;
          color: #cd853f;
          font-weight: bold;
          text-shadow: 2px 2px 4px rgba(@bg-black, 0.9);
          background: rgba(@bg-black, 0.7);
          padding: 5px 10px;
          border-radius: @border-radius-sm;
          border: 1px solid rgba(205, 133, 63, 0.3);
        }

        .info-value {
          font-size: 1.3em;
          color: #daa520;
          font-weight: bold;
          text-shadow: 0 0 10px rgba(218, 165, 32, 0.6), 2px 2px 4px rgba(@bg-black, 0.9);
          font-family: 'Courier New', monospace;
          background: rgba(@bg-black, 0.7);
          padding: 5px 10px;
          border-radius: @border-radius-sm;
          border: 1px solid rgba(218, 165, 32, 0.3);
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 900px) {
  .profile-container {
    width: 90vw;
    height: 85vh;
  }

  .profile-content {
    padding: 30px;

    .profile-title {
      font-size: 2em;
      margin-bottom: 30px;
    }

    .profile-info {
      gap: 20px;

      .info-item {
        padding: 12px 15px;

        .info-label {
          font-size: 1.1em;
        }

        .info-value {
          font-size: 1.2em;
        }
      }
    }
  }
}
</style>
