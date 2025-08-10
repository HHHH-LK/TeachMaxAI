<template>
  <div class="intro-container">
    <!-- 加载遮罩层 -->
    <div
        class="loader"
        v-if="loading"
    >
      <div class="loader-bar">
        <div class="loader-progress" :style="{ width: progress + '%' }"></div>
      </div>
      <p class="loading-text">载入中... {{ progress }}%</p>
    </div>

    <!-- 动画场景容器 -->
    <div class="scene-container" v-else>
      <!-- 背景星空 -->
      <div class="starry-sky"></div>

      <!-- 远处雾气 -->
      <div class="far-fog"></div>

      <!-- 高塔主体 -->
      <div class="tower">
        <div class="tower-base"></div>
        <div class="tower-middle"></div>
        <div class="tower-top"></div>
        <div class="tower-window" v-for="i in 8" :key="i"></div>
        <div class="tower-bat" v-for="i in 3" :key="'bat' + i"></div>
      </div>

      <!-- 近处雾气 -->
      <div class="near-fog"></div>

      <!-- 人物剪影 -->
      <div class="character"></div>

      <!-- 前景装饰 -->
      <div class="foreground"></div>

      <!-- 提示文字 -->
      <div class="prompt-text">按任意键继续</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

// 加载状态管理
const loading = ref(true);
const progress = ref(0);

// 模拟加载过程
onMounted(() => {
  // 模拟资源加载进度
  const timer = setInterval(() => {
    progress.value += Math.random() * 10;
    if (progress.value >= 100) {
      progress.value = 100;
      clearInterval(timer);
      // 加载完成后延迟显示动画
      setTimeout(() => {
        loading.value = false;
        // 监听键盘事件
        window.addEventListener('keydown', handleKeyPress);
      }, 500);
    }
  }, 300);

  // 清理函数
  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyPress);
  });
});

// 键盘事件处理
function handleKeyPress() {
  // 这里可以添加进入游戏的逻辑
  console.log('进入游戏');
}
</script>

<style scoped>
.intro-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #0a0a14;
}

/* 加载界面样式 */
.loader {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #0a0a14;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.loader-bar {
  width: 300px;
  height: 8px;
  border: 1px solid #4a4a68;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 20px;
}

.loader-progress {
  height: 100%;
  background-color: #8b0000;
  transition: width 0.3s ease;
}

.loading-text {
  color: #999;
  font-family: 'Courier New', monospace;
  font-size: 1.2rem;
}

/* 场景容器 */
.scene-container {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 星空背景 */
.starry-sky {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
      radial-gradient(2px 2px at 20px 30px, #ffffff, rgba(0,0,0,0)),
      radial-gradient(2px 2px at 40px 70px, #ffffff, rgba(0,0,0,0)),
      radial-gradient(2px 2px at 50px 160px, #ffffff, rgba(0,0,0,0)),
      radial-gradient(2px 2px at 90px 40px, #ffffff, rgba(0,0,0,0)),
      radial-gradient(2px 2px at 130px 80px, #ffffff, rgba(0,0,0,0)),
      radial-gradient(2px 2px at 160px 120px, #ffffff, rgba(0,0,0,0));
  background-repeat: repeat;
  background-size: 200px 200px;
  animation: twinkle 4s infinite alternate;
  opacity: 0.7;
}

@keyframes twinkle {
  from { opacity: 0.5; }
  to { opacity: 1; }
}

/* 远处雾气 */
.far-fog {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 40%;
  background: linear-gradient(to top, rgba(10, 10, 20, 0.8), transparent);
  animation: fogFlow 20s linear infinite;
}

/* 近处雾气 */
.near-fog {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 30%;
  background: linear-gradient(to top, rgba(10, 10, 20, 0.9), transparent);
  animation: fogFlow 15s linear reverse infinite;
  z-index: 2;
}

@keyframes fogFlow {
  0% { background-position: 0 0; }
  100% { background-position: 1000px 0; }
}

/* 高塔样式 */
.tower {
  position: absolute;
  top: 20%;
  right: 30%;
  width: 200px;
  height: 400px;
  opacity: 0;
  animation: towerAppear 2s 1s forwards;
}

@keyframes towerAppear {
  from { opacity: 0; transform: translateY(50px); }
  to { opacity: 1; transform: translateY(0); }
}

.tower-base {
  position: absolute;
  bottom: 0;
  left: 20px;
  width: 160px;
  height: 250px;
  background-color: #1a1a2e;
  border: 2px solid #252540;
}

.tower-middle {
  position: absolute;
  bottom: 250px;
  left: 50px;
  width: 100px;
  height: 100px;
  background-color: #16213e;
  border: 2px solid #252540;
}

.tower-top {
  position: absolute;
  bottom: 350px;
  left: 80px;
  width: 40px;
  height: 50px;
  background-color: #0f3460;
  border: 2px solid #252540;
  clip-path: polygon(0 100%, 100% 100%, 50% 0);
}

.tower-window {
  position: absolute;
  width: 20px;
  height: 20px;
  background-color: #2c3e50;
  border: 1px solid #34495e;
}

/* 随机位置的窗户 */
.tower-window:nth-child(1) { top: 50px; left: 40px; }
.tower-window:nth-child(2) { top: 100px; left: 120px; }
.tower-window:nth-child(3) { top: 150px; left: 70px; }
.tower-window:nth-child(4) { top: 200px; left: 150px; }
.tower-window:nth-child(5) { top: 250px; left: 50px; }
.tower-window:nth-child(6) { top: 300px; left: 100px; }
.tower-window:nth-child(7) { top: 320px; left: 130px; }
.tower-window:nth-child(8) { top: 340px; left: 60px; }

/* 蝙蝠 */
.tower-bat {
  position: absolute;
  width: 30px;
  height: 20px;
  background-color: #000;
  border-radius: 50% / 30%;
  animation: batFly 8s infinite ease-in-out;
}

.tower-bat:nth-child(9) { top: 10%; left: -50px; animation-delay: 0s; }
.tower-bat:nth-child(10) { top: 30%; left: 250px; animation-delay: 2s; }
.tower-bat:nth-child(11) { top: 60%; left: 300px; animation-delay: 1s; }

@keyframes batFly {
  0% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(50px, -30px) rotate(10deg); }
  50% { transform: translate(100px, 0) rotate(0deg); }
  75% { transform: translate(150px, -20px) rotate(-10deg); }
  100% { transform: translate(200px, 0) rotate(0deg); }
}

/* 人物剪影 */
.character {
  position: absolute;
  bottom: 15%;
  left: 20%;
  width: 60px;
  height: 120px;
  opacity: 0;
  animation: characterAppear 2s 3s forwards;
  z-index: 1;
}

.character::before {
  content: '';
  position: absolute;
  bottom: 0;
  width: 30px;
  height: 80px;
  background-color: #1a1a2e;
  left: 15px;
}

.character::after {
  content: '';
  position: absolute;
  top: 0;
  width: 40px;
  height: 40px;
  background-color: #1a1a2e;
  border-radius: 50%;
  left: 10px;
}

@keyframes characterAppear {
  from { opacity: 0; transform: translateX(-50px); }
  to { opacity: 1; transform: translateX(0); }
}

/* 前景装饰 */
.foreground {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 20%;
  background: linear-gradient(to top, #0a0a14 50%, transparent);
  z-index: 3;
}

/* 提示文字 */
.prompt-text {
  position: absolute;
  bottom: 10%;
  left: 50%;
  transform: translateX(-50%);
  color: #888;
  font-family: 'Courier New', monospace;
  font-size: 1rem;
  opacity: 0;
  animation: textBlink 2s 5s forwards;
  z-index: 4;
}

@keyframes textBlink {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0.7; }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .tower {
    width: 150px;
    height: 300px;
  }

  .tower-base {
    height: 200px;
  }

  .tower-middle {
    bottom: 200px;
  }

  .tower-top {
    bottom: 300px;
  }

  .character {
    width: 50px;
    height: 100px;
  }
}
</style>