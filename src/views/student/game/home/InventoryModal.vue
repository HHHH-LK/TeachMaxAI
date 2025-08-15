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
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>正在加载道具图鉴...</p>
        </div>
        
        <!-- 错误状态 -->
        <div v-else-if="error" class="error-container">
          <div class="error-icon">⚠️</div>
          <p class="error-message">{{ error }}</p>
          <button @click="fetchIllustratedBook" class="retry-btn">重试</button>
        </div>
        
        <!-- 道具列表 -->
        <div v-else class="items-grid">
          <div class="item" v-for="(item, index) in illustratedBookItems" :key="item.itemId || index" @click="handleSelectItem(item)">
            <div class="item-icon">
              <img :src="item.image" :alt="item.name" @error="handleImageError" />
            </div>
            <div class="item-info">
              <h3>{{ item.name || '未命名道具' }}</h3>
              <p>{{ item.description || '暂无描述' }}</p>
              <div class="item-stats">
                <span v-if="item.attack" class="stat-attack">攻击: {{ item.attack }}</span>
                <span v-if="item.defense" class="stat-defense">防御: {{ item.defense }}</span>
                <span v-if="item.heal" class="stat-heal">回血: {{ item.heal }}</span>
                <span v-if="item.assist" class="stat-assist">速度: {{ item.assist }}</span>
              </div>
              <div class="item-rarity">
                <span class="rarity-badge rarity-{{ item.rarity }}">
                  {{ getRarityText(item.rarity) }}
                </span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="!loading && !error && illustratedBookItems.length === 0" class="empty-container">
          <div class="empty-icon">📦</div>
          <p>暂无道具数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import CanvasRenderer from './CanvasRenderer.vue';
import { defineProps, defineEmits, defineOptions, ref, onMounted, watch } from 'vue';
import { gameService } from '@/services/game.js';

// 导入道具图片
import attackImg from '../assets/攻击道具（无背景）.png';
import defenseImg from '../assets/防御道具（无背景）.png';
import healImg from '../assets/回血道具（无背景）.png';
import assistImg from '../assets/护符.png';

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

// 响应式数据
const illustratedBookItems = ref([]);
const loading = ref(false);
const error = ref(null);

// 获取道具图鉴数据
const fetchIllustratedBook = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    const response = await gameService.items.getIllustratedBook();
    
    if (response.data && response.data.success) {
      console.log('API返回的原始数据:', response.data.data); // 调试信息
      
      // 处理道具数据，添加默认图片和数量
      illustratedBookItems.value = response.data.data.map(item => {
        console.log('处理道具:', item); // 调试信息
        
        const processedItem = {
          ...item,
          image: getItemImage(item.type), // 根据道具类型获取对应图片
          quantity: 0, // 默认数量为0
          attack: item.type === 'attack' ? item.effectValue : null,
          defense: item.type === 'defense' ? item.effectValue : null,
          heal: item.type === 'heal' ? item.effectValue : null,
          assist: item.type === 'assist' ? item.effectValue : null
        };
        
        console.log('处理后的道具:', processedItem); // 调试信息
        return processedItem;
      });
      
      console.log('最终的道具列表:', illustratedBookItems.value); // 调试信息
    } else {
      throw new Error(response.data?.message || '获取道具图鉴失败');
    }
  } catch (err) {
    console.error('获取道具图鉴失败:', err);
    error.value = err.message || '获取道具图鉴失败';
  } finally {
    loading.value = false;
  }
};

// 根据道具类型获取对应图片
const getItemImage = (itemType) => {
  console.log('道具类型:', itemType); // 调试信息
  
  const imageMap = {
    'attack': attackImg,
    'defense': defenseImg,
    'heal': healImg,
    'assist': assistImg
  };
  
  const imagePath = imageMap[itemType] || defaultImg;
  console.log('选择的图片路径:', imagePath); // 调试信息
  
  return imagePath;
};

// 获取稀有度文本
const getRarityText = (rarity) => {
  const rarityMap = {
    0: '普通',
    1: '稀有',
    2: '史诗',
    3: '传说',
    4: '神话'
  };
  
  return rarityMap[rarity] || '未知';
};

// 处理图片加载错误
const handleImageError = (event) => {
  event.target.src = '/Image/Attach.png'; // 使用默认图片
};

// 处理关闭事件
const handleClose = () => {
  emit('close');
};

// 处理道具选择事件
const handleSelectItem = (item) => {
  emit('select-item', item);
};

// 监听visible变化，当弹窗显示时获取数据
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    fetchIllustratedBook();
  }
});

// 组件挂载时获取数据
onMounted(() => {
  if (props.visible) {
    fetchIllustratedBook();
  }
});

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
          margin-bottom: 10px;

          span {
            background: rgba(@dark-red, 0.8);
            color: @white;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 0.8em;
            border: 1px solid @light-red;
            
            &.stat-attack {
              background: rgba(255, 69, 0, 0.8);
              border-color: #ff4500;
            }
            
            &.stat-defense {
              background: rgba(0, 128, 0, 0.8);
              border-color: #008000;
            }
            
            &.stat-heal {
              background: rgba(255, 20, 147, 0.8);
              border-color: #ff1493;
            }
            
            &.stat-assist {
              background: rgba(138, 43, 226, 0.8);
              border-color: #8a2be2;
            }
          }
        }
        
        // 稀有度徽章
        .item-rarity {
          text-align: center;
          margin-top: 8px;
          
          .rarity-badge {
            padding: 4px 12px;
            border-radius: 15px;
            font-size: 0.75em;
            font-weight: bold;
            text-transform: uppercase;
            letter-spacing: 1px;
            
            &.rarity-0 {
              background: rgba(128, 128, 128, 0.8);
              color: #ccc;
              border: 1px solid #808080;
            }
            
            &.rarity-1 {
              background: rgba(0, 128, 0, 0.8);
              color: #90ee90;
              border: 1px solid #008000;
            }
            
            &.rarity-2 {
              background: rgba(138, 43, 226, 0.8);
              color: #dda0dd;
              border: 1px solid #8a2be2;
            }
            
            &.rarity-3 {
              background: rgba(255, 215, 0, 0.8);
              color: #fffacd;
              border: 1px solid #ffd700;
            }
            
            &.rarity-4 {
              background: rgba(255, 69, 0, 0.8);
              color: #ffe4e1;
              border: 1px solid #ff4500;
            }
          }
        }
      }
    }
  }
}

// 加载状态样式
.loading-container {
  text-align: center;
  padding: 60px 20px;
  color: @white;
  
  .loading-spinner {
    width: 50px;
    height: 50px;
    border: 4px solid rgba(@dark-red, 0.3);
    border-top: 4px solid @light-red;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 20px;
  }
  
  p {
    font-size: 1.2em;
    color: @gray;
  }
}

// 错误状态样式
.error-container {
  text-align: center;
  padding: 60px 20px;
  color: @white;
  
  .error-icon {
    font-size: 3em;
    margin-bottom: 20px;
    animation: shake 0.5s ease-in-out;
  }
  
  .error-message {
    font-size: 1.1em;
    color: @light-red;
    margin-bottom: 20px;
  }
  
  .retry-btn {
    background: @dark-red;
    color: @white;
    border: 2px solid @light-red;
    padding: 10px 20px;
    border-radius: @border-radius-md;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      background: @light-red;
      transform: translateY(-2px);
    }
  }
}

// 空状态样式
.empty-container {
  text-align: center;
  padding: 60px 20px;
  color: @gray;
  
  .empty-icon {
    font-size: 3em;
    margin-bottom: 20px;
    opacity: 0.6;
  }
  
  p {
    font-size: 1.1em;
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

// 动画关键帧
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}
</style>
