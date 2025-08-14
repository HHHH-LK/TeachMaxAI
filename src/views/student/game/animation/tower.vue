<template>
  <div class="transition-container">
    <!-- 跳过按钮 -->
    <div class="skip-button" v-show="showSkipButton" @click="skipAnimation">
      <span class="skip-text">跳过</span>
    </div>

    <!-- 背景层 -->
    <div class="background-layer">
      <img src="./student/game/img/背景2.png" alt="背景" class="background-image background-1" :class="{ 'active': currentBackground === 1 }" />
      <img src="./student/game/img/背景3.png" alt="背景" class="background-image background-2" :class="{ 'active': currentBackground === 2 }" />
      <img src="./student/game/img/背景1.png" alt="背景" class="background-image background-3" :class="{ 'active': currentBackground === 3 }" />
    </div>

    <!-- 内容图片层 -->
    <div class="content-layer">
      <!-- 第一张图片 -->
      <img
          src="./student/game/img/总塔（主体）.png"
          alt="图片1"
          class="content-image image-1"
          :class="{ 'active': currentImage === 1 }"
      />

      <!-- 第二张图片 -->
      <img
          src="./student/game/img/门口（无水印）.png"
          alt="图片2"
          class="content-image image-2"
          :class="{ 'active': currentImage === 2 }"
      />

      <!-- 第三张图片 -->
      <img
          src="./student/game/img/1.png"
          alt="图片3"
          class="content-image image-3"
          :class="{ 'active': currentImage === 3 }"
      />

      <!-- 第四张图片 -->
      <img
          src="./student/game/img/3（塔）.png"
          alt="图片4"
          class="content-image image-4"
          :class="{ 'active': currentImage === 4 }"
      />

      <!-- 第五张图片 -->
      <img
          src="./student/game/img/2.png"
          alt="图片5"
          class="content-image image-5"
          :class="{ 'active': currentImage === 5 }"
      />

      <!-- 第二轮图片 -->
      <!-- 第六张图片 -->
      <img
          src="./student/game/img/楼梯（无水印）.png"
          alt="图片6"
          class="content-image image-6"
          :class="{ 'active': currentImage === 6 }"
      />

      <!-- 第七张图片 -->
      <img
          src="./student/game/img/柱（无水印）.png"
          alt="图片7"
          class="content-image image-7"
          :class="{ 'active': currentImage === 7 }"
      />

      <!-- 第八张图片 -->
      <img
          src="./student/game/img/标靶（无水印）.png"
          alt="图片8"
          class="content-image image-8"
          :class="{ 'active': currentImage === 8 }"
      />

      <!-- 第九张图片 -->
      <img
          src="./student/game/img/宝剑（主体）.png"
          alt="图片9"
          class="content-image image-9"
          :class="{ 'active': currentImage === 9 }"
      />

      <!-- 第十张图片 -->
      <img
          src="./student/game/img/楼阁（无水印）.png"
          alt="图片10"
          class="content-image image-10"
          :class="{ 'active': currentImage === 10 }"
      />

      <!-- 第三轮图片 -->
      <!-- 第十一张图片 -->
      <img
          src="./student/game/img/窗（无水印）.png"
          alt="图片11"
          class="content-image image-11"
          :class="{ 'active': currentImage === 11 }"
      />

      <!-- 第十二张图片 -->
      <img
          src="./student/game/img/天花板（无水印）.png"
          alt="图片12"
          class="content-image image-12"
          :class="{ 'active': currentImage === 12 }"
      />

      <!-- 第十三张图片 -->
      <img
          src="./student/game/img/龙（主体）.png"
          alt="图片13"
          class="content-image image-13"
          :class="{ 'active': currentImage === 13 }"
      />

      <!-- 第十四张图片 -->
      <img
          src="./student/game/img/宝剑（无主体）.png"
          alt="图片14"
          class="content-image image-14"
          :class="{ 'active': currentImage === 14 }"
      />

      <!-- 第十五张图片 -->
      <img
          src="./student/game/img/总塔（无水印）.png"
          alt="图片15"
          class="content-image image-15"
          :class="{ 'active': currentImage === 15 }"
      />
    </div>

    <!-- 文字展示区域 -->
    <div class="text-display">
      <div class="text-container">
        <p class="story-text" :class="{ 'active': currentTextIndex === 0 }">
          在遥远的未来，人类文明发展到了一个全新的高度，科技与智慧的结晶催生了一座神秘的高塔"永恒之塔"。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 1 }">
          这座塔并非由实体砖石建成，而是由无数虚拟系统和数据结构交织而成，象征着人类对完美设计与无穷知识的追求。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 2 }">
          塔的每一层都蕴含着特定的知识领域，唯有通过理解与掌握该层的核心理念，才能继续向上攀登。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 3 }">
          传说中，塔的顶层隐藏着一种能够解决所有人类问题的终极模型--"万物蓝图"。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 4 }">
          然而，进入塔中的人很快发现，这里的挑战并不在于体力或武器，而在于逻辑思维与系统设计能力。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 5 }">
          每一位挑战者都需要运用UML(统一建模语言)来解析塔中复杂的规则与结构:从用例图描绘塔的功能需求，到类图揭示其内部逻辑。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 6 }">
          从时序图模拟动态交互，到状态图追踪变化规律。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 7 }">
          随着挑战者不断深入，他们逐渐意识到，"永恒之塔"不仅是对个人能力的试炼场，更是一面镜子,映射出他们在现实世界中对系统分析与设计的理解深度。
        </p>
        <p class="story-text" :class="{ 'active': currentTextIndex === 8 }">
          只有真正领悟UML建模精髓的人，才能解开层层谜题，最终触及那传说中的"万物蓝图"，为人类开启新的纪元篇章。
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

// 响应式数据
const currentImage = ref(0)
const currentBackground = ref(1)
const currentTextIndex = ref(-1)
const isTransitioning = ref(false)
const showSkipButton = ref(false) // 控制跳过按钮显示
let transitionTimer = null

// 语音播放相关
const isSpeaking = ref(false)
const speechQueue = ref([])
let currentSpeech = null

// 语音播放功能
const speakText = (text) => {
  if ('speechSynthesis' in window) {
    // 创建语音合成实例
    const utterance = new SpeechSynthesisUtterance(text)

    // 设置语音参数 - 优化长文本播放，确保在动画结束前播放完
    utterance.lang = 'zh-CN' // 中文
    utterance.rate = 1.6     // 加快语速，确保在动画结束前完成
    utterance.pitch = 1.0    // 音调
    utterance.volume = 1.0   // 音量

    // 语音播放开始
    utterance.onstart = () => {
      isSpeaking.value = true
    }

    // 语音播放结束
    utterance.onend = () => {
      isSpeaking.value = false
    }

    // 语音播放错误处理
    utterance.onerror = (event) => {
      console.error('语音播放错误:', event.error)
      isSpeaking.value = false
    }

    // 直接播放，不使用队列
    currentSpeech = utterance
    speechSynthesis.speak(utterance)
  }
}

// 停止语音播放
const stopSpeaking = () => {
  if ('speechSynthesis' in window) {
    speechSynthesis.cancel()
    isSpeaking.value = false
    currentSpeech = null
  }
}

// 跳过动画
const skipAnimation = () => {
  // 停止语音播放
  stopSpeaking()

  // 停止所有动画
  isTransitioning.value = false

  // 重置所有状态
  currentImage.value = 0
  currentBackground.value = 1
  currentTextIndex.value = -1
  showSkipButton.value = false

  // 这里可以添加跳转到下一个页面的逻辑
  // 例如：router.push('/next-page')
  console.log('动画已跳过')
}

// 自动开始第一轮转场动画
const startTransition = () => {
  isTransitioning.value = true
  currentImage.value = 1

  // 开始显示文字
  startTextDisplay()

  // 第一张图片显示
  setTimeout(() => {
    currentImage.value = 2
  }, 5000)

  // 第二张图片显示
  setTimeout(() => {
    currentImage.value = 3
  }, 10000)

  // 第三张图片显示
  setTimeout(() => {
    currentImage.value = 4
  }, 15000)

  // 第四张图片显示
  setTimeout(() => {
    currentImage.value = 5
  }, 20000)

  // 第五张图片显示
  setTimeout(() => {
    // 第一轮结束，显示跳过按钮
    showSkipButton.value = true

    // 开始第二轮
    setTimeout(() => {
      startSecondRound()
    }, 1000)
  }, 25000)
}

// 开始文字显示
const startTextDisplay = () => {
  currentTextIndex.value = -1

  // 将所有文字拼在一起，一次性播放语音
  const fullStory = `在遥远的未来，人类文明发展到了一个全新的高度，科技与智慧的结晶催生了一座神秘的高塔"永恒之塔"。这座塔并非由实体砖石建成，而是由无数虚拟系统和数据结构交织而成，象征着人类对完美设计与无穷知识的追求。塔的每一层都蕴含着特定的知识领域，唯有通过理解与掌握该层的核心理念，才能继续向上攀登。传说中，塔的顶层隐藏着一种能够解决所有人类问题的终极模型--"万物蓝图"。然而，进入塔中的人很快发现，这里的挑战并不在于体力或武器，而在于逻辑思维与系统设计能力。每一位挑战者都需要运用UML(统一建模语言)来解析塔中复杂的规则与结构:从用例图描绘塔的功能需求，到类图揭示其内部逻辑。从时序图模拟动态交互，到状态图追踪变化规律。随着挑战者不断深入，他们逐渐意识到，"永恒之塔"不仅是对个人能力的试炼场，更是一面镜子,映射出他们在现实世界中对系统分析与设计的理解深度。只有真正领悟UML建模精髓的人，才能解开层层谜题，最终触及那传说中的"万物蓝图"，为人类开启新的纪元篇章。`

  // 在页面加载后立即开始播放完整故事，确保在动画结束前播放完
  setTimeout(() => {
    speakText(fullStory)
  }, 1000)

  // 第一段文字
  setTimeout(() => {
    currentTextIndex.value = 0
  }, 2000)

  // 第二段文字（隐藏第一段）
  setTimeout(() => {
    currentTextIndex.value = 1
  }, 8000)

  // 第三段文字（隐藏第二段）
  setTimeout(() => {
    currentTextIndex.value = 2
  }, 14000)

  // 第四段文字（隐藏第三段）
  setTimeout(() => {
    currentTextIndex.value = 3
  }, 22000)

  // 第五段文字（隐藏第四段）
  setTimeout(() => {
    currentTextIndex.value = 4
  }, 30000)

  // 第六段文字（隐藏第五段）
  setTimeout(() => {
    currentTextIndex.value = 5
  }, 38000)

  // 第七段文字（隐藏第六段）
  setTimeout(() => {
    currentTextIndex.value = 6
  }, 46000)

  // 第八段文字（隐藏第七段）
  setTimeout(() => {
    currentTextIndex.value = 7
  }, 54000)

  // 第九段文字（隐藏第八段）
  setTimeout(() => {
    currentTextIndex.value = 8
  }, 62000)

  // 最后隐藏所有文字
  setTimeout(() => {
    currentTextIndex.value = -1
  }, 70000)

  // 语音在动画结束前停止
  setTimeout(() => {
    stopSpeaking()
  }, 70000)
}

// 开始第二轮转场
const startSecondRound = () => {
  // 切换背景到背景3
  currentBackground.value = 2
  currentImage.value = 6

  // 第六张图片显示
  setTimeout(() => {
    currentImage.value = 7
  }, 5000)

  // 第七张图片显示
  setTimeout(() => {
    currentImage.value = 8
  }, 10000)

  // 第八张图片显示
  setTimeout(() => {
    currentImage.value = 9
  }, 15000)

  // 第九张图片显示
  setTimeout(() => {
    currentImage.value = 10
  }, 20000)

  // 第十张图片显示
  setTimeout(() => {
    // 第二轮结束，开始第三轮
    setTimeout(() => {
      startThirdRound()
    }, 1000)
  }, 25000)
}

// 开始第三轮转场
const startThirdRound = () => {
  // 切换背景到背景1
  currentBackground.value = 3
  currentImage.value = 11

  // 第十一张图片显示
  setTimeout(() => {
    currentImage.value = 12
  }, 5000)

  // 第十二张图片显示
  setTimeout(() => {
    currentImage.value = 13
  }, 10000)

  // 第十三张图片显示
  setTimeout(() => {
    currentImage.value = 14
  }, 15000)

  // 第十四张图片显示
  setTimeout(() => {
    currentImage.value = 15
  }, 20000)

  // 第十五张图片显示
  setTimeout(() => {
    setTimeout(() => {
      isTransitioning.value = false
    }, 1000)
  }, 25000)
}

// 组件挂载后自动开始转场
onMounted(() => {
  // 延迟1秒后自动开始，给页面加载一些时间
  setTimeout(() => {
    startTransition()
  }, 1000)
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (transitionTimer) {
    clearTimeout(transitionTimer)
  }
  // 停止语音播放
  stopSpeaking()
})
</script>

<style scoped>
.transition-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #000;
}

/* 背景层样式 */
.background-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 105%;
  height: 105%;
  z-index: 1;
}

/* 跳过按钮样式 */
.skip-button {
  position: fixed;
  top: 30px;
  right: 30px;
  z-index: 100;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 12px 20px;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}

.skip-button:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
}

.skip-button:active {
  transform: translateY(0);
}

.skip-text {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}

.background-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 1.5s ease-in-out;
  will-change: transform, opacity, filter;
  backface-visibility: hidden;
}

.background-image.active {
  opacity: 0.3; /* 进一步降低透明度，让背景更透明 */
  animation: backgroundFade 8s ease-in-out infinite, backgroundFloat 6s ease-in-out infinite;
}

/* 内容图片层样式 */
.content-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-image {
  position: absolute;
  height: 105%;
  object-fit: cover;
  opacity: 0;
  transform: translate(0, 0);
  transition: opacity 1.2s ease-in-out;
  pointer-events: none;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.3));
  will-change: transform, opacity;
  backface-visibility: hidden;
  transform-style: preserve-3d;
}

.content-image.active {
  pointer-events: auto;
}

/* 文字展示区域样式 */
.text-display {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  width: 80%;
  max-width: 900px;
}

.text-container {
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.story-text {
  color: #ffffff;
  font-size: 24px;
  line-height: 1.6;
  margin: 0;
  opacity: 0;
  transform: translateY(20px);
  transition: all 1.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  text-align: center;
  text-shadow: 0 4px 8px rgba(0, 0, 0, 0.9), 0 0 30px rgba(0, 0, 0, 0.7), 0 0 50px rgba(0, 0, 0, 0.5);
  position: absolute;
  width: 100%;
  left: 0;
  top: 50%;
  transform: translateY(-50%) translateY(20px);
  pointer-events: none;
  font-weight: 600;
  letter-spacing: 1px;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}

.story-text.active {
  opacity: 1;
  transform: translateY(-50%) translateY(0);
  pointer-events: auto;
}

/* 响应式文字样式 */
@media (max-width: 768px) {
  .text-display {
    width: 90%;
    bottom: 50px;
  }

  .text-container {
    min-height: 100px;
  }

  .story-text {
    font-size: 20px;
    line-height: 1.5;
    letter-spacing: 0.8px;
  }

  .skip-button {
    top: 20px;
    right: 20px;
    padding: 10px 16px;
  }

  .skip-text {
    font-size: 14px;
  }
}

/* 图片单向平移动画 */
@keyframes contentFloat {
  0% {
    transform: translate(-8px, 0px);
  }
  50% {
    transform: translate(0px, 0px);
  }
  100% {
    transform: translate(8px, 0px);
  }
}

/* 背景淡化动画 */
@keyframes backgroundFade {
  0%, 100% {
    opacity: 0.3;
    filter: brightness(1) contrast(1.05);
  }
  25% {
    opacity: 0.25;
    filter: brightness(1) contrast(1.05);
  }
  50% {
    opacity: 0.2;
    filter: brightness(1) contrast(1.05);
  }
  75% {
    opacity: 0.25;
    filter: brightness(1) contrast(1.05);
  }
}

/* 背景轻微晃动动画 */
@keyframes backgroundFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1.02);
  }
  25% {
    transform: translate(-3px, -2px) scale(1.025);
  }
  50% {
    transform: translate(2px, 1px) scale(1.03);
  }
  75% {
    transform: translate(-1px, 3px) scale(1.015);
  }
}

/* 添加一些额外的视觉效果 */
.transition-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at center, transparent 0%, rgba(0, 0, 0, 0.1) 100%);
  z-index: 1;
  pointer-events: none;
}

/* 图片进入动画增强 - 默认隐藏 */
.content-image {
  opacity: 0;
  transform: translate(0, 0) scale(1);
}

/* 第一张图片从上方淡入 */
.image-1.active {
  animation: imageEntranceFromTop 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

/* 第二张图片从下方淡入 */
.image-2.active {
  animation: imageEntranceFromBottom 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

/* 第三张图片从左侧淡入 */
.image-3.active {
  animation: imageEntranceFromLeft 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

/* 第四张图片从右侧淡入 */
.image-4.active {
  animation: imageEntranceFromRight 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
  object-fit: cover;
}

/* 第五张图片从中间淡入缩放 */
.image-5.active {
  animation: imageEntranceFromCenter 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
  object-fit: cover;
}

@keyframes imageEntranceFromTop {
  0% {
    opacity: 0;
    transform: translate(0, -80px) scale(0.95);
  }
  100% {
    opacity: 0.7;
    transform: translate(0, 0) scale(1);
  }
}

@keyframes imageEntranceFromBottom {
  0% {
    opacity: 0;
    transform: translate(0, 80px) scale(0.95);
  }
  100% {
    opacity: 0.7;
    transform: translate(0, 0) scale(1);
  }
}

@keyframes imageEntranceFromLeft {
  0% {
    opacity: 0;
    transform: translate(-80px, 0) scale(0.95);
  }
  100% {
    opacity: 0.7;
    transform: translate(0, 0) scale(1);
  }
}

@keyframes imageEntranceFromRight {
  0% {
    opacity: 0;
    transform: translate(80px, 0) scale(0.95);
  }
  100% {
    opacity: 0.7;
    transform: translate(0, 0) scale(1);
  }
}

@keyframes imageEntranceFromCenter {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0.6);
  }
  100% {
    opacity: 0.7;
    transform: translate(0, 0) scale(1);
  }
}

/* 平移动画 - 在进入动画完成后开始 */
.image-1.active {
  animation: imageEntranceFromTop 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-2.active {
  animation: imageEntranceFromBottom 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-3.active {
  animation: imageEntranceFromLeft 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-4.active {
  animation: imageEntranceFromRight 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-5.active {
  animation: imageEntranceFromCenter 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

/* 第二轮图片动画 */
.image-6.active {
  animation: imageEntranceFromTop 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-7.active {
  animation: imageEntranceFromBottom 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-8.active {
  animation: imageEntranceFromLeft 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-9.active {
  animation: imageEntranceFromRight 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-10.active {
  animation: imageEntranceFromCenter 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

/* 第三轮图片动画 */
.image-11.active {
  animation: imageEntranceFromTop 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-12.active {
  animation: imageEntranceFromBottom 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-13.active {
  animation: imageEntranceFromLeft 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-14.active {
  animation: imageEntranceFromRight 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}

.image-15.active {
  animation: imageEntranceFromCenter 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards, contentFloat 4s cubic-bezier(0.4, 0, 0.2, 1) infinite 1.2s;
}
</style>