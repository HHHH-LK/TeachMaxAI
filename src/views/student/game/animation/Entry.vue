<template>
  <div class="transition-container">
    <div class="gif-container" v-if="!isLoading">
      <img
          v-for="(gif, index) in gifList"
          :key="index"
          :src="gif.src"
          :class="['gif-image', { 'active': currentScene === index }]"
          :alt="gif.name"
          @load="onGifLoad(index)"
          @error="onGifError(index)"
      />

      <!-- 雪花效果容器 -->
      <div class="snow-container">
        <div
            v-for="snowflake in snowflakes"
            :key="snowflake.id"
            class="snowflake"
            :style="{
            left: snowflake.x + 'px',
            top: snowflake.y + 'px',
            opacity: snowflake.opacity,
            transform: `rotate(${snowflake.rotation}deg) scale(${snowflake.scale})`,
            animationDuration: snowflake.duration + 's'
          }"
        >❄</div>
      </div>
    </div>

    <div class="ui-overlay">
      <!-- 加载进度条 -->
      <div class="loading-progress" v-if="isLoading">
        <div class="progress-bar" :class="{ 'completed': loadingProgress >= 100 }">
          <div class="progress-fill" :style="{ width: `${loadingProgress}%` }"></div>
        </div>
        <p class="loading-text" :class="{ 'completed': loadingProgress >= 100 }">
          {{ loadingProgress >= 100 ? '即将进入神秘之塔...' : `加载资源中... ${Math.round(loadingProgress)}%` }}
        </p>
      </div>

      <!-- 错误提示 -->
      <div class="error-message" v-if="loadError">
        <p>{{ loadError }}</p>
        <button @click="retryLoad" class="retry-btn">重试</button>
      </div>
    </div>
  </div>
</template>

<script>
import {ref, onMounted, onBeforeUnmount} from 'vue'
import img1 from './assets/远眺.gif'
import img2 from './assets/初涉.gif'
import img3 from './assets/摸索.gif'
import img4 from './assets/临近.gif'
import img5 from './assets/总塔.gif'

export default {
  name: 'GameTransitionAnimation',
  setup() {
    const isLoading = ref(true)
    const loadingProgress = ref(0)
    const loadError = ref('')
    const currentScene = ref(0)
    const loadedGifs = ref(0)
    const snowflakes = ref([])

    const gifList = [
      { src: img1, name: '远眺' },
      { src: img2, name: '初涉' },
      { src: img3, name: '摸索' },
      { src: img4, name: '临近' },
      { src: img5, name: '总塔' }
    ]

    let transitionTimer = null
    let sceneTimer = null
    let snowAnimationId = null
    let lastSnowflakeTime = 0

    const onGifLoad = (index) => {
      loadedGifs.value++
      loadingProgress.value = (loadedGifs.value / gifList.length) * 100

      if (loadedGifs.value === gifList.length) {
        // 确保进度条显示100%后再开始动画
        setTimeout(() => {
          isLoading.value = false
          startTransition()
        }, 1000) // 延迟1秒，让用户看到100%进度
      }
    }

    const onGifError = (index) => {
      console.warn(`GIF ${index} 加载失败:`, gifList[index].src)
      loadedGifs.value++
      loadingProgress.value = (loadedGifs.value / gifList.length) * 100

      if (loadedGifs.value === gifList.length) {
        // 确保进度条显示100%后再开始动画
        setTimeout(() => {
          isLoading.value = false
          if (loadedGifs.value < gifList.length) {
            loadError.value = '部分GIF资源加载失败，但可以继续播放'
          }
          startTransition()
        }, 1000) // 延迟1秒，让用户看到100%进度
      }
    }

    const startTransition = () => {
      // 显示第一张图片
      currentScene.value = 0

      // 开始自动转场
      sceneTimer = setInterval(() => {
        const nextScene = (currentScene.value + 1) % gifList.length
        performDramaticTransition(nextScene)
      }, 5000) // 每5秒切换一次

      // 启动雪花效果
      startSnowEffect()
    }

    // 执行优雅转场
    const performDramaticTransition = (nextScene) => {
      // 创建转场特效元素
      createTransitionEffects()

      // 延迟切换场景，让特效先播放
      setTimeout(() => {
        currentScene.value = nextScene
      }, 500)
    }

    // 创建转场特效
    const createTransitionEffects = () => {
      // 粒子爆炸效果
      createParticleExplosion()

      // 波纹效果
      createRippleEffect()

      // 屏幕分割效果
      createScreenSplitEffect()

      // 缩放和模糊效果
      createScaleAndBlurEffect()
    }



    // 粒子效果
    const createParticleExplosion = () => {
      const container = document.querySelector('.transition-container')
      if (!container) return

      for (let i = 0; i < 15; i++) {
        const particle = document.createElement('div')
        particle.className = 'transition-particle'

        const angle = (i / 15) * Math.PI * 2
        const distance = 80 + Math.random() * 60
        const x = Math.cos(angle) * distance
        const y = Math.sin(angle) * distance

        particle.style.left = '50%'
        particle.style.top = '50%'
        particle.style.transform = `translate(-50%, -50%)`

        container.appendChild(particle)

        // 粒子动画
        setTimeout(() => {
          particle.style.transform = `translate(calc(-50% + ${x}px), calc(-50% + ${y}px))`
          particle.style.opacity = '0'
        }, 100)

        // 清理粒子
        setTimeout(() => {
          if (particle.parentNode) {
            particle.parentNode.removeChild(particle)
          }
        }, 1200)
      }
    }

    // 波纹效果
    const createRippleEffect = () => {
      const container = document.querySelector('.transition-container')
      if (!container) return

      const ripple = document.createElement('div')
      ripple.className = 'ripple-effect'
      container.appendChild(ripple)

      setTimeout(() => {
        if (ripple.parentNode) {
          ripple.parentNode.removeChild(ripple)
        }
      }, 1200)
    }

    // 屏幕分割效果
    const createScreenSplitEffect = () => {
      const container = document.querySelector('.transition-container')
      if (!container) return

      const splitBar = document.createElement('div')
      splitBar.className = 'screen-split-bar'
      splitBar.style.top = '50%'
      container.appendChild(splitBar)

      setTimeout(() => {
        if (splitBar.parentNode) {
          splitBar.parentNode.removeChild(splitBar)
        }
      }, 1000)
    }

    // 缩放和模糊效果
    const createScaleAndBlurEffect = () => {
      const activeImage = document.querySelector('.gif-image.active')
      if (activeImage) {
        // 添加缩放效果
        activeImage.style.transform = 'scale(1.1)'
        activeImage.style.filter = 'blur(2px) brightness(1.2)'

        // 恢复原状
        setTimeout(() => {
          activeImage.style.transform = 'scale(1)'
          activeImage.style.filter = 'brightness(0.9) contrast(1.1)'
        }, 800)
      }
    }

    // 创建雪花
    const createSnowflake = () => {
      return {
        id: Math.random().toString(36).substr(2, 9),
        x: Math.random() * window.innerWidth,
        y: -30 - Math.random() * 50,
        opacity: 0,
        targetOpacity: Math.random() * 0.6 + 0.3,
        rotation: Math.random() * 360,
        scale: Math.random() * 0.4 + 0.6,
        duration: Math.random() * 3 + 2,
        speed: Math.random() * 1.5 + 0.8,
        sway: Math.random() * 1.5 - 0.75,
        fadeIn: true
      }
    }

    // 启动雪花效果
    const startSnowEffect = () => {
      // 不创建初始雪花，让雪花慢慢生成
      snowflakes.value = []

      // 延迟一点开始生成雪花，让用户先看到GIF
      setTimeout(() => {
        // 开始雪花动画
        animateSnow()
      }, 200) // 延迟0.2秒开始下雪
    }

    // 雪花动画循环
    const animateSnow = () => {
      // 更新现有雪花位置
      snowflakes.value.forEach((snowflake, index) => {
        // 雪花渐入效果
        if (snowflake.fadeIn && snowflake.opacity < snowflake.targetOpacity) {
          snowflake.opacity += 0.02
          if (snowflake.opacity >= snowflake.targetOpacity) {
            snowflake.fadeIn = false
          }
        }

        // 雪花下落
        snowflake.y += snowflake.speed

        // 雪花左右摇摆
        snowflake.x += Math.sin(snowflake.y * 0.01) * 0.5 * snowflake.sway

        // 雪花旋转
        snowflake.rotation += 0.5

        // 如果雪花落到底部，重新从顶部开始
        if (snowflake.y > window.innerHeight + 20) {
          snowflake.y = -20
          snowflake.x = Math.random() * window.innerWidth
          snowflake.opacity = 0
          snowflake.fadeIn = true
        }

        // 如果雪花飘出左右边界，重新定位
        if (snowflake.x < -20) {
          snowflake.x = window.innerWidth + 20
        } else if (snowflake.x > window.innerWidth + 20) {
          snowflake.x = -20
        }
      })

      // 使用时间控制生成新雪花，让雪花生成更自然
      const currentTime = Date.now()
      if (currentTime - lastSnowflakeTime > 200) { // 每200ms生成一个雪花，稍微慢一点
        const newSnowflake = createSnowflake()
        snowflakes.value.push(newSnowflake)
        lastSnowflakeTime = currentTime
      }

      // 限制雪花数量，避免性能问题
      if (snowflakes.value.length > 80) { // 减少最大数量
        snowflakes.value.splice(0, 5) // 每次只移除少量雪花
      }

      snowAnimationId = requestAnimationFrame(animateSnow)
    }

    // 处理窗口大小变化
    const handleResize = () => {
      // 重新调整雪花位置，确保雪花在可视区域内
      snowflakes.value.forEach(snowflake => {
        if (snowflake.x > window.innerWidth) {
          snowflake.x = Math.random() * window.innerWidth
        }
        if (snowflake.y > window.innerHeight) {
          snowflake.y = -20
        }
      })
    }

    const retryLoad = () => {
      loadError.value = ''
      isLoading.value = true
      loadingProgress.value = 0
      loadedGifs.value = 0
      currentScene.value = 0

      // 重新加载所有GIF
      gifList.forEach((gif, index) => {
        const img = new Image()
        img.onload = () => onGifLoad(index)
        img.onerror = () => onGifError(index)
        img.src = gif.src
      })
    }

    onMounted(() => {
      // 预加载所有GIF
      gifList.forEach((gif, index) => {
        const img = new Image()
        img.onload = () => onGifLoad(index)
        img.onerror = () => onGifError(index)
        img.src = gif.src
      })

      // 监听窗口大小变化，重新调整雪花位置
      window.addEventListener('resize', handleResize)
    })

    onBeforeUnmount(() => {
      if (sceneTimer) {
        clearInterval(sceneTimer)
      }
      if (transitionTimer) {
        clearTimeout(transitionTimer)
      }
      if (snowAnimationId) {
        cancelAnimationFrame(snowAnimationId)
      }

      // 移除窗口大小监听器
      window.removeEventListener('resize', handleResize)
    })

    return {
      isLoading,
      loadingProgress,
      loadError,
      currentScene,
      gifList,
      snowflakes,
      retryLoad,
      onGifLoad,
      onGifError
    }
  }
}
</script>

<style scoped>
.transition-container {
  position: fixed;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #000000;
  font-family: 'Arial', sans-serif;
  top: 0;
  left: 0;
  z-index: 9999;
}

.gif-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #000000;
}

.gif-image {
  position: absolute;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: all 0.8s ease-in-out;
  filter: brightness(0.9) contrast(1.1);
  transform: scale(1);
}

.gif-image.active {
  opacity: 1;
}

/* 雪花效果样式 */
.snow-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 5;
}

.snowflake {
  position: absolute;
  color: #ffffff;
  font-size: 12px;
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.8);
  user-select: none;
  pointer-events: none;
  will-change: transform;
  animation: snowFloat 3s ease-in-out infinite;
}

/* 雪花飘落动画 */
@keyframes snowFloat {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-5px) rotate(180deg);
  }
}

.ui-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  pointer-events: none;
  z-index: 10;
}

.loading-progress {
  text-align: center;
  color: #ffffff;
  pointer-events: none;
}

.progress-bar {
  width: 400px;
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 0 20px rgba(0, 255, 136, 0.3),
  inset 0 0 10px rgba(0, 0, 0, 0.5);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00ff88, #00cc66, #00ff88);
  border-radius: 3px;
  transition: width 0.3s ease;
  box-shadow: 0 0 15px rgba(0, 255, 136, 0.6);
  background-size: 200% 100%;
  animation: progressShimmer 2s linear infinite;
}

/* 100%完成时的特殊效果 */
.progress-bar.completed {
  border-color: rgba(0, 255, 136, 0.8);
  box-shadow: 0 0 30px rgba(0, 255, 136, 0.8);
}

.progress-bar.completed .progress-fill {
  background: linear-gradient(90deg, #00ff88, #00cc66, #00ff88, #00ff88);
  animation: progressShimmer 1s linear infinite;
  box-shadow: 0 0 25px rgba(0, 255, 136, 0.8);
}

.loading-text.completed {
  color: #00ff88;
  text-shadow: 0 0 15px rgba(0, 255, 136, 0.8);
  animation: textGlow 1s ease-in-out infinite;
}

.loading-text {
  font-size: 18px;
  margin: 0;
  text-shadow: 0 0 10px rgba(0, 255, 136, 0.8);
  animation: textPulse 2s ease-in-out infinite;
  letter-spacing: 1px;
}

.error-message {
  text-align: center;
  color: #ff6b6b;
  margin-top: 20px;
  pointer-events: auto;
}

.error-message p {
  margin: 0 0 15px 0;
  font-size: 16px;
  text-shadow: 0 0 10px rgba(255, 107, 107, 0.5);
}

.retry-btn {
  background: linear-gradient(45deg, #ff6b6b, #ee5a52);
  border: none;
  color: white;
  padding: 10px 20px;
  border-radius: 25px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.3);
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.4);
}

.retry-btn:active {
  transform: translateY(0);
}

@keyframes progressShimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

@keyframes textPulse {
  0%, 100% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.02);
  }
}

@keyframes textGlow {
  0%, 100% {
    text-shadow: 0 0 15px rgba(0, 255, 136, 0.8);
  }
  50% {
    text-shadow: 0 0 25px rgba(0, 255, 136, 1), 0 0 35px rgba(0, 255, 136, 0.6);
  }
}

@media (max-width: 768px) {
  .progress-bar {
    width: 280px;
    height: 6px;
  }

  .loading-text {
    font-size: 16px;
  }

  /* 移动端全屏优化 */
  .transition-container {
    width: 100vw;
    height: 100vh;
    min-height: 100vh;
  }

  .gif-image {
    object-fit: cover;
  }

  /* 移动端雪花优化 */
  .snowflake {
    font-size: 10px;
  }

  /* 移动端特效优化 */
  .screen-split-bar {
    height: 1px;
  }
}

@media (max-width: 480px) {
  .progress-bar {
    width: 240px;
  }

  .loading-text {
    font-size: 14px;
  }
}

/* 优雅转场特效样式 */

/* 缩放效果 */
.scale-effect {
  animation: scaleEffect 0.6s ease-in-out;
}

@keyframes scaleEffect {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* 模糊效果 */
.blur-effect {
  animation: blurEffect 0.6s ease-in-out;
}

@keyframes blurEffect {
  0%, 100% {
    filter: blur(0px);
  }
  50% {
    filter: blur(8px);
  }
}

/* 优雅转场粒子效果 */
.transition-particle {
  position: absolute;
  width: 3px;
  height: 3px;
  background: linear-gradient(45deg, #00ffff, #0080ff, #8000ff);
  border-radius: 50%;
  pointer-events: none;
  z-index: 1000;
  transition: all 1.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 8px currentColor;
  opacity: 0.8;
}

/* 柔和波纹效果 */
.ripple-effect {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border: 1px solid rgba(0, 255, 255, 0.6);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 1000;
  animation: rippleEffect 1.5s ease-out forwards;
}

@keyframes rippleEffect {
  0% {
    width: 0;
    height: 0;
    opacity: 1;
    border-width: 2px;
  }
  100% {
    width: 400px;
    height: 400px;
    opacity: 0;
    border-width: 1px;
  }
}

/* 优雅屏幕分割效果 */
.screen-split-bar {
  position: absolute;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, #00ffff, #0080ff, transparent);
  z-index: 1000;
  pointer-events: none;
  animation: splitBarEffect 1.2s ease-in-out forwards;
}

@keyframes splitBarEffect {
  0% {
    transform: scaleX(0);
    opacity: 0;
  }
  50% {
    transform: scaleX(1);
    opacity: 1;
  }
  100% {
    transform: scaleX(0);
    opacity: 0;
  }
}

/* 扭曲效果 */
.distortion-effect {
  animation: distortionEffect 0.8s ease-in-out;
}

@keyframes distortionEffect {
  0%, 100% {
    filter: none;
  }
  25% {
    filter: hue-rotate(90deg) saturate(2);
  }
  50% {
    filter: hue-rotate(180deg) saturate(3) contrast(1.5);
  }
  75% {
    filter: hue-rotate(270deg) saturate(2);
  }
}

/* 全屏模式优化 */
html, body {
  margin: 0;
  padding: 0;
  overflow: hidden;
  height: 100%;
}

/* 确保GIF完全填充屏幕 */
.gif-image.active {
  opacity: 1;
  width: 100vw;
  height: 100vh;
  object-fit: cover;
  object-position: center;
}
</style>