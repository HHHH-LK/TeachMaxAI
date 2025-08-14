<template>
  <div v-if="visible" class="inventory-overlay" @click="handleClose">
    <div class="inventory-container" @click.stop>
      <CanvasRenderer
          :width="800"
          :height="600"
          render-type="inventory"
          canvas-class="inventory-canvas"
      />
      <div class="inventory-content">
        <h2 class="inventory-title">道具图鉴</h2>
        <div class="items-grid">
          <div class="item" v-for="(item, index) in items" :key="index" @click="handleSelectItem(item)">
            <div class="item-quantity">{{ item.quantity }}</div>
            <div class="item-icon">
              <img :src="item.image" :alt="item.name" />
            </div>
            <div class="item-info">
              <h3>{{ item.name }}</h3>
              <p>{{ item.description }}</p>
              <div class="item-stats">
                <span v-if="item.attack">攻击: {{ item.attack }}</span>
                <span v-if="item.defense">防御: {{ item.defense }}</span>
                <span v-if="item.heal">回血: {{ item.heal }}</span>
                <span v-if="item.assist">速度: {{ item.assist }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import CanvasRenderer from './CanvasRenderer.vue';
import { defineProps, defineEmits, defineOptions } from 'vue';

// 声明组件接收的props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  items: {
    type: Array,
    default: () => []
  }
});

// 声明组件发出的事件
const emit = defineEmits(['close', 'select-item']);

// 处理关闭事件
const handleClose = () => {
  emit('close');
};

// 处理道具选择事件
const handleSelectItem = (item) => {
  emit('select-item', item);
};

// 定义组件名称
defineOptions({
  name: 'InventoryModal'
});
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
@border-radius-md: 10px;
@border-radius-lg: 15px;
@border-radius-sm: 4px;
@border-radius-full: 50%;

.inventory-overlay {
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

.inventory-container {
  position: relative;
  width: 800px;
  height: 600px;
  border-radius: @border-radius-md;
  overflow: hidden;
  box-shadow: 0 0 50px rgba(@dark-red, 0.8);

  .inventory-canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
  }

  .inventory-content {
    position: relative;
    z-index: 2;
    padding: 40px;
    color: @white;
    text-shadow: 2px 2px 4px rgba(@bg-black, 0.8);

    .inventory-title {
      text-align: center;
      font-size: 2.5em;
      margin-bottom: 30px;
      color: @light-red;
      text-shadow: 0 0 20px @shadow-red;
      font-weight: bold;
      letter-spacing: 3px;
    }

    .items-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 25px;
      margin-top: 40px;
      max-height: 400px;
      overflow-y: auto;
      padding-right: 10px;

      // 自定义滚动条
      &::-webkit-scrollbar {
        width: 8px;
      }
      &::-webkit-scrollbar-track {
        background: rgba(@dark-gray, 0.5);
        border-radius: @border-radius-sm;
      }
      &::-webkit-scrollbar-thumb {
        background: @dark-red;
        border-radius: @border-radius-sm;

        &:hover {
          background: @light-red;
        }
      }

      .item {
        background: rgba(@dark-gray, 0.9);
        border: 2px solid @dark-red;
        border-radius: @border-radius-lg;
        padding: 15px;
        text-align: center;
        cursor: pointer;
        transition: all 0.3s ease;
        position: relative;
        overflow: hidden;
        min-height: 200px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;

        // 数量标记
        .item-quantity {
          position: absolute;
          top: 10px;
          right: 10px;
          background: @light-red;
          color: @white;
          width: 24px;
          height: 24px;
          border-radius: @border-radius-full;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: bold;
          border: 2px solid @dark-red;
          z-index: 2;
        }

        // 图标容器
        .item-icon {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-bottom: 15px;

          img {
            width: 70px;
            height: 70px;
            filter: drop-shadow(0 0 10px @shadow-light-red);
          }
        }

        // 悬停光效
        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: linear-gradient(90deg, transparent, rgba(@light-red, 0.3), transparent);
          transition: left 0.5s ease;
        }

        // 悬停效果
        &:hover {
          transform: translateY(-5px);
          border-color: @light-red;
          box-shadow: 0 10px 30px @shadow-light-red;

          &::before {
            left: 100%;
          }
        }

        // 点击效果
        &:active {
          transform: scale(0.95);
          box-shadow: 0 5px 15px @shadow-light-red;
        }

        // 物品信息
        .item-info {
          h3 {
            color: @light-red;
            margin-bottom: 8px;
            font-size: 1.1em;
            text-shadow: 0 0 10px @shadow-red;
          }

          p {
            color: @gray;
            margin-bottom: 10px;
            font-size: 0.85em;
            line-height: 1.3;
            flex-grow: 1;
          }
        }

        // 物品属性
        .item-stats {
          display: flex;
          justify-content: space-around;
          flex-wrap: wrap;
          gap: 10px;

          span {
            background: rgba(@dark-red, 0.8);
            color: @white;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 0.8em;
            border: 1px solid @light-red;
          }
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 900px) {
  .inventory-container {
    width: 90vw;
    height: 80vh;
  }

  .items-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .inventory-content {
    padding: 20px;
  }
}
</style>
