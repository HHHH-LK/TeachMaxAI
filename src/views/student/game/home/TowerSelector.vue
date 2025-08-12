<template>
  <div class="tower-selector">
    <!-- 塔选择器 -->
    <div class="tower-picker">
      <div class="picker-container">
        <div class="picker-track" ref="pickerTrack">
          <div
            v-for="(tower, index) in towers"
            :key="index"
            class="tower-option"
            :class="{ active: selectedTowerIndex === index }"
            @click="selectTower(index)"
          >
            <div class="tower-number">{{ tower.name }}</div>
            <div class="tower-preview" v-if="index > 0">
              <canvas
                :ref="`towerCanvas${index}`"
                :width="60"
                :height="80"
                class="tower-canvas"
              ></canvas>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 塔展示区域 -->
    <div class="tower-display" v-if="selectedTowerIndex > 0">
      <div class="display-container">
        <div class="navigation-buttons">
          <button
              class="nav-btn nav-left"
              @click="navigateTower(-1)"
              :disabled="selectedTowerIndex <= 1"
          >
            <svg viewBox="0 0 24 24" class="nav-icon">
              <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
            </svg>
          </button>

          <button
              class="nav-btn nav-right"
              @click="navigateTower(1)"
              :disabled="selectedTowerIndex >= towers.length - 1"
          >
            <svg viewBox="0 0 24 24" class="nav-icon">
              <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
            </svg>
          </button>
        </div>

        <div class="tower-parts">
          <!-- 塔顶 -->
          <div class="tower-part tower-top">
            <img :src="towerTopImg" alt="塔顶" />
          </div>

          <!-- 塔层 - 可滚动 -->
           <div class="tower-layers" ref="towerLayers">
             <div
               v-for="(layer, index) in towerLayers"
               :key="index"
               class="tower-layer"
             >
               <img :src="towerLayerImg" alt="塔层" />
               <div class="tower-lock">
                 <svg viewBox="0 0 24 24" class="lock-icon">
                   <path d="M12 1C8.676 1 6 3.676 6 7v2H5c-1.103 0-2 .897-2 2v9c0 1.103.897 2 2 2h14c1.103 0 2-.897 2-2v-9c0-1.103-.897-2-2-2h-1V7c0-3.324-2.676-6-6-6zm6 10.723V20H5v-8.277h1V7c0-2.761 2.239-5 5-5s5 2.239 5 5v3.723h2z"/>
                   <path d="M12 12c-1.103 0-2 .897-2 2v3c0 1.103.897 2 2 2s2-.897 2-2v-3c0-1.103-.897-2-2-2z"/>
                 </svg>
               </div>
             </div>
           </div>

          <!-- 塔底 -->
          <div class="tower-part tower-bottom">
            <img :src="towerBottomImg" alt="塔底" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import towerTopImg from '../assets/塔顶.png'
import towerLayerImg from '../assets/塔层.png'
import towerBottomImg from '../assets/塔底.png'

export default {
  name: 'TowerSelector',
  data() {
    return {
      selectedTowerIndex: 0,
      towers: [
        { name: '', level: 0 },
        { name: 'A塔', level: 1 },
        { name: 'B塔', level: 2 },
        { name: 'C塔', level: 3 },
        { name: 'D塔', level: 4 },
        { name: 'E塔', level: 5 },
        { name: 'F塔', level: 6 },
        { name: 'G塔', level: 7 },
        { name: 'H塔', level: 8 }
      ],
      towerLayers: [],
      towerTopImg,
      towerLayerImg,
      towerBottomImg
    }
  },

  mounted() {
    this.initTowerLayers()
    this.renderTowerPreviews()
    this.setupScroll()
  },

  methods: {
    // 初始化塔层
    initTowerLayers() {
      // 为每个塔创建不同数量的层
      this.towerLayers = Array.from({ length: 20 }, (_, i) => ({
        id: i,
        level: i + 1
      }))
    },

    // 渲染塔预览图
    renderTowerPreviews() {
      this.$nextTick(() => {
        this.towers.forEach((tower, index) => {
          if (index > 0) {
            this.renderTowerPreview(index, tower.level)
          }
        })
      })
    },

    // 渲染单个塔预览
    renderTowerPreview(towerIndex, level) {
      const canvas = this.$refs[`towerCanvas${towerIndex}`]
      if (!canvas || !canvas[0]) return

      const ctx = canvas[0].getContext('2d')
      const width = canvas[0].width
      const height = canvas[0].height

      // 清空画布
      ctx.clearRect(0, 0, width, height)

      // 创建更暗的渐变背景
      const gradient = ctx.createLinearGradient(0, 0, 0, height)
      gradient.addColorStop(0, '#1a0f08')
      gradient.addColorStop(0.3, '#2c1810')
      gradient.addColorStop(0.7, '#1a0f08')
      gradient.addColorStop(1, '#0a0504')

      ctx.fillStyle = gradient
      ctx.fillRect(0, 0, width, height)

      // 添加诡异的纹理
      for (let i = 0; i < 20; i++) {
        ctx.strokeStyle = `rgba(139, 0, 0, ${0.1 + Math.random() * 0.2})`
        ctx.lineWidth = 0.5
        ctx.beginPath()
        ctx.moveTo(Math.random() * width, Math.random() * height)
        ctx.lineTo(Math.random() * width, Math.random() * height)
        ctx.stroke()
      }

      // 绘制塔的轮廓 - 更加扭曲
      ctx.strokeStyle = '#8b0000'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(width * 0.2, height * 0.1)
      ctx.lineTo(width * 0.8, height * 0.1)
      ctx.lineTo(width * 0.9, height * 0.3)
      ctx.lineTo(width * 0.85, height * 0.5)
      ctx.lineTo(width * 0.9, height * 0.7)
      ctx.lineTo(width * 0.9, height * 0.9)
      ctx.lineTo(width * 0.1, height * 0.9)
      ctx.lineTo(width * 0.1, height * 0.7)
      ctx.lineTo(width * 0.15, height * 0.5)
      ctx.lineTo(width * 0.1, height * 0.3)
      ctx.closePath()
      ctx.stroke()

      // 添加内部阴影
      ctx.fillStyle = 'rgba(0, 0, 0, 0.6)'
      ctx.fill()

      // 添加血红色符文
      ctx.fillStyle = '#ff0000'
      ctx.font = 'bold 8px Courier'
      ctx.textAlign = 'center'
      ctx.shadowColor = '#ff0000'
      ctx.shadowBlur = 8
      ctx.fillText(`${level}`, width / 2, height * 0.6)

      // 添加诡异的眼睛效果
      ctx.fillStyle = '#ff0000'
      ctx.shadowBlur = 5
      ctx.beginPath()
      ctx.arc(width * 0.3, height * 0.4, 2, 0, Math.PI * 2)
      ctx.arc(width * 0.7, height * 0.4, 2, 0, Math.PI * 2)
      ctx.fill()

      // 添加血滴效果
      ctx.fillStyle = 'rgba(255, 0, 0, 0.7)'
      ctx.shadowBlur = 0
      ctx.beginPath()
      ctx.ellipse(width * 0.5, height * 0.8, 3, 1, 0, 0, Math.PI * 2)
      ctx.fill()

      // 添加闪电效果
      ctx.strokeStyle = '#ff0000'
      ctx.lineWidth = 1
      ctx.shadowColor = '#ff0000'
      ctx.shadowBlur = 3
      ctx.beginPath()
      ctx.moveTo(width * 0.2, height * 0.2)
      ctx.lineTo(width * 0.4, height * 0.3)
      ctx.lineTo(width * 0.3, height * 0.5)
      ctx.lineTo(width * 0.6, height * 0.6)
      ctx.stroke()

      ctx.shadowBlur = 0
    },

    // 选择塔
    selectTower(index) {
      this.selectedTowerIndex = index
      this.$emit('tower-selected', this.towers[index])
      
      // 如果选择的是非第一个塔，也要滚动到对应位置
      if (index > 0) {
        this.scrollTowerPickerToIndex(index)
      }
    },

    // 导航到其他塔
    navigateTower(direction) {
      const newIndex = this.selectedTowerIndex + direction
      if (newIndex >= 1 && newIndex < this.towers.length) {
        this.selectTower(newIndex)
        
        // 让上面的塔选择器也跟着滑动到对应位置
        this.scrollTowerPickerToIndex(newIndex)
      }
    },

    // 滚动塔选择器到指定索引位置
    scrollTowerPickerToIndex(index) {
      const pickerTrack = this.$refs.pickerTrack
      if (pickerTrack) {
        const towerOption = pickerTrack.children[index]
        if (towerOption) {
          const containerWidth = pickerTrack.offsetWidth
          const optionWidth = towerOption.offsetWidth
          const optionLeft = towerOption.offsetLeft
          const scrollPosition = optionLeft - (containerWidth / 2) + (optionWidth / 2)
          
          pickerTrack.scrollTo({
            left: scrollPosition,
            behavior: 'smooth'
          })
        }
      }
    },

    // 设置滚动
    setupScroll() {
      // 塔选择器的左右滚动
      const pickerTrack = this.$refs.pickerTrack
      if (pickerTrack) {
        let isDown = false
        let startX
        let scrollLeft

        pickerTrack.addEventListener('mousedown', (e) => {
          isDown = true
          startX = e.pageX - pickerTrack.offsetLeft
          scrollLeft = pickerTrack.scrollLeft
        })

        pickerTrack.addEventListener('mouseleave', () => {
          isDown = false
        })

        pickerTrack.addEventListener('mouseup', () => {
          isDown = false
        })

        pickerTrack.addEventListener('mousemove', (e) => {
          if (!isDown) return
          e.preventDefault()
          const x = e.pageX - pickerTrack.offsetLeft
          const walk = (x - startX) * 2
          pickerTrack.scrollLeft = scrollLeft - walk
        })
      }

      // 塔层的上下滚动
      const towerLayers = this.$refs.towerLayers
      if (towerLayers) {
        let isDown = false
        let startY
        let scrollTop

        towerLayers.addEventListener('mousedown', (e) => {
          isDown = true
          startY = e.pageY - towerLayers.offsetTop
          scrollTop = towerLayers.scrollTop
        })

        towerLayers.addEventListener('mouseleave', () => {
          isDown = false
        })

        towerLayers.addEventListener('mouseup', () => {
          isDown = false
        })

        towerLayers.addEventListener('mousemove', (e) => {
          if (!isDown) return
          e.preventDefault()
          const y = e.pageY - towerLayers.offsetTop
          const walk = (y - startY) * 2
          towerLayers.scrollTop = scrollTop - walk
        })
      }
    }
  }
}
</script>

<style lang="less" scoped>
.tower-selector {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 65%;
  max-width: 600px;
  z-index: 10;
}

.tower-picker {
  margin-bottom: 30px;

  .picker-container {
    background: rgba(0, 0, 0, 0.9);
    border: 2px solid #8b0000;
    border-radius: 15px;
    padding: 20px;
    box-shadow: 0 0 30px rgba(139, 0, 0, 0.8), inset 0 0 20px rgba(0, 0, 0, 0.5);
    position: relative;
    animation: shadowFlicker 4s ease-in-out infinite;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background:
        radial-gradient(circle at 20% 20%, rgba(139, 0, 0, 0.3) 0%, transparent 50%),
        radial-gradient(circle at 80% 80%, rgba(0, 0, 0, 0.4) 0%, transparent 50%);
      border-radius: 15px;
      pointer-events: none;
    }
  }

  .picker-track {
    display: flex;
    gap: 20px;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    scroll-behavior: smooth;
  }

  .tower-option {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s ease;
    min-width: 70px;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: -5px;
      left: -5px;
      right: -5px;
      bottom: -5px;
      background: radial-gradient(circle, rgba(139, 0, 0, 0.2) 0%, transparent 70%);
      border-radius: 10px;
      opacity: 0;
      transition: opacity 0.3s ease;
      pointer-events: none;
    }

    &:hover {
      transform: translateY(-5px);

      &::before {
        opacity: 1;
      }
    }

    &.active {
      transform: scale(1.1);

      &::before {
        opacity: 1;
        background: radial-gradient(circle, rgba(139, 0, 0, 0.4) 0%, transparent 70%);
      }

      .tower-number {
        color: #ff0000;
        text-shadow: 0 0 15px #ff0000, 0 0 25px #8b0000;
        animation: textPulse 2s ease-in-out infinite;
      }
    }
  }

  .tower-number {
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 10px;
    text-shadow: 0 0 5px rgba(255, 255, 255, 0.5);
    transition: all 0.3s ease;
    font-family: 'Courier New', monospace;
    letter-spacing: 1px;
  }

  .tower-preview {
    width: 55px;
    height: 75px;
    border: 2px solid #8b0000;
    border-radius: 8px;
    overflow: hidden;
    background: linear-gradient(135deg, #1a0f08, #2c1810);
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background:
        radial-gradient(circle at 30% 30%, rgba(139, 0, 0, 0.3) 0%, transparent 50%),
        linear-gradient(45deg, transparent 40%, rgba(0, 0, 0, 0.4) 50%, transparent 60%);
      pointer-events: none;
    }

    .tower-canvas {
      display: block;
      width: 100%;
      height: 100%;
      filter: contrast(1.2) brightness(0.8);
    }
  }
}

.tower-display {
  .display-container {
    background: rgba(0, 0, 0, 0.95);
    border: 3px solid #8b0000;
    border-radius: 20px;
    padding: 25px;
    box-shadow: 0 0 40px rgba(139, 0, 0, 0.9), inset 0 0 30px rgba(0, 0, 0, 0.7);
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      background: linear-gradient(45deg, #8b0000, #ff0000, #8b0000);
      border-radius: 20px;
      z-index: -1;
      animation: borderGlow 2s ease-in-out infinite;
    }

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background:
        radial-gradient(circle at 20% 20%, rgba(139, 0, 0, 0.2) 0%, transparent 50%),
        radial-gradient(circle at 80% 80%, rgba(0, 0, 0, 0.3) 0%, transparent 50%);
      border-radius: 20px;
      pointer-events: none;
    }
  }

  // 导航按钮样式
  .navigation-buttons {
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    transform: translateY(-50%);
    display: flex;
    justify-content: space-between;
    pointer-events: none;
    z-index: 20;
  }

  .nav-btn {
    width: 50px;
    height: 50px;
    border: 2px solid #8b0000;
    border-radius: 50%;
    background: 
      radial-gradient(circle at 30% 30%, rgba(139, 0, 0, 0.4) 0%, transparent 50%),
      radial-gradient(circle at 70% 70%, rgba(0, 0, 0, 0.6) 0%, transparent 50%),
      linear-gradient(135deg, #1a0f08 0%, #2c1810 50%, #1a0f08 100%);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    pointer-events: auto;
    position: relative;
    box-shadow: 
      0 0 20px rgba(139, 0, 0, 0.6),
      inset 0 0 15px rgba(0, 0, 0, 0.8),
      0 4px 8px rgba(0, 0, 0, 0.6);
    
    // 添加神秘符文背景
    &::before {
      content: '';
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      background: 
        conic-gradient(from 0deg, #8b0000, #ff0000, #8b0000, #660000, #8b0000);
      border-radius: 50%;
      z-index: -1;
      opacity: 0;
      transition: opacity 0.4s ease;
      animation: borderRotate 8s linear infinite;
    }

    // 添加内部纹理
    &::after {
      content: '';
      position: absolute;
      top: 2px;
      left: 2px;
      right: 2px;
      bottom: 2px;
      background: 
        radial-gradient(circle at 20% 20%, rgba(255, 0, 0, 0.1) 0%, transparent 40%),
        radial-gradient(circle at 80% 80%, rgba(0, 0, 0, 0.3) 0%, transparent 40%),
        repeating-conic-gradient(from 0deg, transparent 0deg, rgba(139, 0, 0, 0.1) 10deg, transparent 20deg);
      border-radius: 50%;
      pointer-events: none;
    }

    &:hover {
      transform: scale(1.15) rotate(5deg);
      border-color: #ff0000;
      box-shadow: 
        0 0 40px rgba(255, 0, 0, 0.9),
        inset 0 0 25px rgba(255, 0, 0, 0.3),
        0 8px 16px rgba(0, 0, 0, 0.8);

      &::before {
        opacity: 1;
        animation: borderRotate 4s linear infinite;
      }

      .nav-icon {
        fill: #ff0000;
        filter: drop-shadow(0 0 15px rgba(255, 0, 0, 0.9));
        animation: iconPulse 1.5s ease-in-out infinite;
      }
    }

    &:active {
      transform: scale(0.95);
      box-shadow: 
        0 0 20px rgba(255, 0, 0, 0.7),
        inset 0 0 20px rgba(0, 0, 0, 0.9);
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
      border-color: #444;
      box-shadow: none;
      background: 
        radial-gradient(circle at 30% 30%, rgba(68, 68, 68, 0.2) 0%, transparent 50%),
        linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 50%, #1a1a1a 100%);

      &:hover {
        transform: none;
        border-color: #444;
        box-shadow: none;

        &::before {
          opacity: 0;
        }

        .nav-icon {
          fill: #666;
          filter: none;
          animation: none;
        }
      }
    }

    .nav-icon {
      width: 24px;
      height: 24px;
      fill: #8b0000;
      transition: all 0.4s ease;
      filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.8));
      position: relative;
      z-index: 2;
    }
  }

  .tower-parts {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0;
    position: relative;
  }

  .tower-part {
    img {
      width: 140px;
      height: auto;
      filter: drop-shadow(0 0 15px rgba(139, 0, 0, 0.9)) contrast(1.1) brightness(0.9);
      transition: all 0.3s ease;

      &:hover {
        filter: drop-shadow(0 0 25px rgba(255, 0, 0, 0.8)) contrast(1.2) brightness(1.1);
        transform: scale(1.05);
      }
    }
  }

  .tower-layers {
    max-height: 280px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b0000 #1a0f08;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-left: 15px; /* 向右移动塔层更多 */

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: #1a0f08;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-thumb {
      background: #8b0000;
      border-radius: 3px;

      &:hover {
        background: #ff0000;
      }
    }

    .tower-layer {
     margin: 0;
     position: relative;
     display: flex;
     justify-content: center;

     &::before {
       content: '';
       position: absolute;
       top: 0;
       left: 0;
       right: 0;
       bottom: 0;
       background: linear-gradient(90deg, transparent 0%, rgba(139, 0, 0, 0.1) 50%, transparent 100%);
       opacity: 0;
       transition: opacity 0.3s ease;
       pointer-events: none;
     }

     &:hover::before {
       opacity: 1;
     }

     img {
       width: 140px;
       height: auto;
       filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.7)) contrast(1.1) brightness(0.9);
       transition: all 0.3s ease;

       &:hover {
         filter: drop-shadow(0 0 20px rgba(255, 0, 0, 0.8)) contrast(1.2) brightness(1.1);
         transform: scale(1.03);
       }
     }

     .tower-lock {
       position: absolute;
       top: 50%;
       left: 50%;
       transform: translate(-50%, -50%);
       z-index: 10;
       pointer-events: none;

       .lock-icon {
         width: 24px;
         height: 24px;
         fill: #8b0000;
         filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.8));
         animation: lockGlow 3s ease-in-out infinite;

         path {
           stroke: #ff0000;
           stroke-width: 0.5;
           stroke-linejoin: round;
         }
       }
     }

     &:hover .tower-lock .lock-icon {
       fill: #ff0000;
       filter: drop-shadow(0 0 15px rgba(255, 0, 0, 0.9));
       animation: lockPulse 1s ease-in-out infinite;
     }
   }
  }
 }

 @keyframes borderGlow {
  0%, 100% {
    opacity: 0.3;
    box-shadow: 0 0 20px rgba(139, 0, 0, 0.8);
  }
  50% {
    opacity: 1;
    box-shadow: 0 0 40px rgba(255, 0, 0, 1);
  }
}

@keyframes textPulse {
  0%, 100% {
    text-shadow: 0 0 15px #ff0000, 0 0 25px #8b0000;
  }
  50% {
    text-shadow: 0 0 25px #ff0000, 0 0 35px #8b0000, 0 0 45px #ff0000;
  }
}

@keyframes shadowFlicker {
  0%, 100% {
    box-shadow: 0 0 30px rgba(139, 0, 0, 0.6);
  }
  25% {
    box-shadow: 0 0 40px rgba(255, 0, 0, 0.8);
  }
  50% {
    box-shadow: 0 0 35px rgba(139, 0, 0, 0.7);
  }
  75% {
    box-shadow: 0 0 45px rgba(255, 0, 0, 0.9);
  }
}

@keyframes lockGlow {
  0%, 100% {
    filter: drop-shadow(0 0 8px rgba(139, 0, 0, 0.8));
    transform: scale(1);
  }
  50% {
    filter: drop-shadow(0 0 15px rgba(139, 0, 0, 0.9));
    transform: scale(1.05);
  }
}

@keyframes lockPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

@keyframes borderRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes iconPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .tower-selector {
    width: 90%;
  }

  .tower-picker .picker-container {
    padding: 15px;
  }

  .tower-display .display-container {
    padding: 20px;
  }

  .tower-display .nav-btn {
    width: 40px;
    height: 40px;

    .nav-icon {
      width: 20px;
      height: 20px;
    }
  }

  .tower-part img,
  .tower-layer img {
    width: 100px;
  }

  .tower-layer .tower-lock .lock-icon {
    width: 18px;
    height: 18px;
  }
}
</style>


