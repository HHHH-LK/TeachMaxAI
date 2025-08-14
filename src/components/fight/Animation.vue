<template>
  <transition name="fade">
    <div class="comic-animation" v-if="show">
      <!-- 第一页：前三幕 -->
      <div class="page page-1" v-show="currentPage === 1">
        <div class="comic-panels">
          <div class="panel panel-1" ref="panel1">
            <div class="panel-content">
              <img src="/Image/Anim01.png" class="scene-image" />
            </div>
          </div>

          <div class="panel panel-2" ref="panel2">
            <div class="panel-content">
              <img src="/Image/Anim02.png" class="scene-image" />
            </div>
          </div>

          <div class="panel panel-3" ref="panel3">
            <div class="panel-content">
              <img src="/Image/Anim03.gif" class="scene-image" />
            </div>
          </div>
        </div>
      </div>

      <!-- 第二页：后两幕 -->
      <div class="page page-2" v-show="currentPage === 2">
        <div class="comic-panels">
          <div class="panel panel-4" ref="panel4">
            <div class="panel-content">
              <img src="/Image/Anim04.png" class="scene-image" />
            </div>
          </div>

          <div class="panel panel-5" ref="panel5">
            <div class="panel-content">
              <video 
                ref="sceneVideo" 
                class="scene-video" 
                muted 
                playsinline
                @ended="onVideoEnded"
              >
                <source src="/Video/Anim05.mp4" type="video/mp4">
              </video>
            </div>
          </div>
        </div>
      </div>

      <!-- 全局字幕区域 -->
      <div class="global-caption" ref="globalCaption">
        <div class="caption-container">
          <div class="caption-content">
            <p class="caption-paragraph">
              {{ currentParagraph }}
            </p>
          </div>
        </div>
      </div>

      <!-- 控制按钮 -->
      <button class="next-button" @click="nextScene">
        <span v-if="currentScene < 5">下一幕</span>
        <span v-else>开始探索</span>
      </button>

      <!-- 动态效果元素 -->
      <div class="comic-effects">
        <div class="effect effect-1"></div>
        <div class="effect effect-2"></div>
        <div class="effect effect-3"></div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { gsap } from "gsap";

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  duration: {
    type: Number,
    default: 40000,
  },
  captions: {
    type: String,
    default: `在总塔的第1层，传说中这里是"初始之厅"，一片由纯粹数据构成的空间。这里没有复杂的逻辑，也没有深奥的算法，只有最基础的元素--变量和数据类型，构成了这片空间的基石。
你醒来时，发现自己站在一个漂浮着无数光点的大厅中，每个光点都闪烁着不同的颜色:红色代表整数，蓝色代表浮点数，绿色是字符串，金色则是布尔值。这些光点像是有生命一般，在空中游走、碰撞，却又遵循着某种未知的规则。
一个低沉而温和的声音在大厅中响起:"欢迎来到初始之厅，旅行者。在这里，你需要学会掌握这些基础的数据形态，才能唤醒沉睡的'核心程序'，打开通往更高层的道路。记住，每一个变量都是你的工具，每一种数据类型都有其独特的用途。"
随着声音的消失，你面前出现了一块悬浮的石板，上面显示着一行文字:"任务:定义并操作变量,完成指定目标。"不远处，一扇巨大的门静静地伫立着，门上布满了复杂的数据流纹路，似乎等待着被激活。
你迈出了第一步，准备探索这个由变量与数据构成的世界，揭开它的秘密`,
  },
});

const emit = defineEmits(["animation-end"]);

const currentScene = ref(0);
const currentPage = ref(1);

// 元素引用
const panel1 = ref(null);
const panel2 = ref(null);
const panel3 = ref(null);
const panel4 = ref(null);
const panel5 = ref(null);
const globalCaption = ref(null);
const sceneVideo = ref(null); // 视频元素引用

// 语音合成相关
const speechSynthesis = window.speechSynthesis;
const utterance = ref(null);
const isSpeaking = ref(false);

// 分割字幕为段落
const captionsArray = computed(() => {
  return props.captions.split("\n");
});

// 计算当前场景的字幕段落
const currentParagraph = computed(() => {
  if (
    currentScene.value > 0 &&
    currentScene.value <= captionsArray.value.length
  ) {
    return captionsArray.value[currentScene.value - 1];
  }
  return "";
});

// 重置所有面板
const resetPanels = () => {
  const panels = [panel1.value, panel2.value, panel3.value, panel4.value, panel5.value];
  
  panels.forEach(panel => {
    if (panel) {
      gsap.set(panel, {
        opacity: 0,
        scale: 0.1,
        rotation: 0,
        y: 50,
        x: 0,
        z: 0
      });
    }
  });
};

// 动画单个面板
const animatePanel = (panel) => {
  if (!panel) return;
  
  gsap.to(panel, {
    opacity: 1,
    scale: 1,
    rotation: 0,
    y: 0,
    duration: 3,
    ease: "elastic.out(1, 0.75)",
    onStart: () => {
      // 添加3D效果
      gsap.set(panel, {
        transformStyle: "preserve-3d",
        perspective: 9000
      });
      
      // 添加阴影效果
      gsap.to(panel, {
        boxShadow: "0 10px 25px rgba(139, 0, 0, 0.6)",
        duration: 0.5
      });
    }
  });
  
  // 添加额外的3D效果
  gsap.fromTo(panel, 
    { rotationY: -10, rotationX: 15 },
    { rotationY: 0, rotationX: 0, duration: 1.5, ease: "back.out(1.7)" }
  );
};

// 动画字幕
const animateCaption = () => {
  if (!globalCaption.value) return;
  
  gsap.fromTo(globalCaption.value,
    { opacity: 0, y: 30 },
    {
      opacity: 1,
      y: 0,
      duration: 1,
      ease: "power3.out",
      onStart: () => {
        // 添加文字动画
        gsap.fromTo(".caption-paragraph", 
          { opacity: 0, y: 20 },
          { opacity: 1, y: 0, stagger: 0.1, duration: 0.8 }
        );
      }
    }
  );
};

// 播放场景动画
const playSceneAnimation = () => {
  switch (currentScene.value) {
    case 1:
      animatePanel(panel1.value);
      break;
    case 2:
      animatePanel(panel2.value);
      break;
    case 3:
      animatePanel(panel3.value);
      break;
    case 4:
      animatePanel(panel4.value);
      break;
    case 5:
      // 第五幕特殊处理 - 播放视频
      animatePanel(panel5.value);
      playVideo();
      break;
  }
  
  // 字幕动画
  animateCaption();
  
  // 开始语音合成
  startSpeechSynthesis();
};

// 播放视频
const playVideo = () => {
  if (sceneVideo.value) {
    sceneVideo.value.currentTime = 0; // 重置到开始
    sceneVideo.value.play().catch(e => {
      console.error("视频播放失败:", e);
    });
  }
};

// 视频结束事件处理
const onVideoEnded = () => {
  // 视频结束后暂停在最后一帧
  if (sceneVideo.value) {
    sceneVideo.value.pause();
  }
};

// 开始语音合成
const startSpeechSynthesis = () => {
  // 停止当前语音
  stopSpeechSynthesis();
  
  // 创建新的语音合成
  const text = currentParagraph.value;
  if (!text) return;
  
  utterance.value = new SpeechSynthesisUtterance(text);
  utterance.value.lang = 'zh-CN'; // 设置语言为中文
  utterance.value.rate = 2.0; // 语速
  utterance.value.pitch = 1.0; // 音调
  utterance.value.volume = 1.0; // 音量
  
  // 语音开始事件
  utterance.value.onstart = () => {
    isSpeaking.value = true;
  };
  
  // 语音结束事件
  utterance.value.onend = () => {
    isSpeaking.value = false;
    // 语音结束后自动进入下一幕
    if (currentScene.value < 5) {
      nextScene();
    }
  };
  
  // 播放语音
  speechSynthesis.speak(utterance.value);
};

// 停止语音合成
const stopSpeechSynthesis = () => {
  if (speechSynthesis.speaking) {
    speechSynthesis.cancel();
  }
  isSpeaking.value = false;
};

// 切换到下一幕
const nextScene = () => {
  // 停止当前语音
  stopSpeechSynthesis();
  
  if (currentScene.value < 5) {
    currentScene.value++;
    
    // 检查是否需要翻页
    if (currentScene.value === 4) {
      currentPage.value = 2;
    }
    
    playSceneAnimation();
  } else {
    // 动画结束
    emit("animation-end");
  }
};

// 监听show属性变化
watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      currentScene.value = 1; // 从第一幕开始
      // 确保面板重置后再播放动画
      resetPanels();
      setTimeout(() => {
        playSceneAnimation();
      }, 50);
    } else {
      // 停止语音合成
      stopSpeechSynthesis();
    }
  },
  { immediate: true }
);

// 组件卸载时停止语音
onUnmounted(() => {
  stopSpeechSynthesis();
});

// 初始化
onMounted(() => {
  resetPanels();
});
</script>

<style scoped>
.comic-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #0a0805, #1a140f);
  z-index: 30000;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  padding: 20px;
  box-sizing: border-box;
}

.page {
  position: relative;
  width: 100%;
  height: 90%; 
  display: flex;
  justify-content: center;
  align-items: center;
}

.comic-panels {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: 1fr;
  gap: 8px;
  width: 100%;
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
}

.panel {
  position: relative;
  background: rgba(30, 20, 10, 0.7);
  box-shadow: 0 5极狐 15px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  opacity: 0;
  transform: scale(0.1);
}

.page-1 .panel-1 {
  grid-column: 1;
  clip-path: polygon(0 0, 100% 0, 80% 100%, 0% 100%);
}

.page-1 .panel-2 {
  grid-column: 2;
  clip-path: polygon(20% 0, 100% 0, 100% 100%, 0 100%);
}

.page-1 .panel-3 {
  grid-column: 3;
}

/* 第二页的形状 */
.page-2 .comic-panels {
  grid-template-columns: repeat(2, 1fr);
  gap: 8px; 
}

.page-2 .panel-4 {
  grid-column: 1;
  clip-path: polygon(0 0, 80% 0, 100% 100%, 0 100% );
}

.page-2 .panel-5 {
  grid-column: 2;
  clip-path: polygon(0 0, 100% 0%, 100% 100%, 20% 100%); 
}

.panel-content {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.scene-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

/* 视频样式 - 与图片样式保持一致 */
.scene-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
  pointer-events: none; /* 防止视频拦截点击事件 */
}

.panel.active .scene-image,
.panel.active .scene-video {
  transform: scale(1.05);
}

/* 全局字幕区域 */
.global-caption {
  position: fixed;
  width: 80%;
  max-width: 900px;
  height: 20%;
  margin-top: 400px;
  opacity: 0;
  transform: translateY(30px);
}

.caption-container {
  width: 100%;
  height: 100%;
  padding: 20px;
  background: 
    linear-gradient(rgba(20, 15, 10, 0.9), rgba(40, 30, 20, 0.7)),
    url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM34 90c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm56-76c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM12 86c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm28-65c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm23-11c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-6 60c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm29 22c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zM32 63c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm57-13c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-9-21c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM60 91c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM35 41c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM12 60c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2z' fill='%235a3a1a' fill-opacity='0.1' fill-rule='evenodd'/%3E%3C/svg%3E");
  border-radius: 10px;
  box-shadow: 
    0 0 15px rgba(139, 0, 0, 0.5),
    inset 0 0 10px rgba(0, 0, 0, 0.8);
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.caption-container::before {
  left: 0;
  border-right: 1px solid #d4af37;
}

.caption-container::after {
  right: 0;
  border-left: 1px solid #d4af37;
}

.caption-content {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 2;
}

.caption-paragraph {
  font-size: 22px;
  color: #e0e0e0; 
  line-height: 1.6;
  text-shadow: 
    0 0 5px rgba(0, 0, 0, 0.7),
    0 0 10px rgba(200, 200, 200, 0.5); 
  font-family: "Cinzel", serif;
  margin: 0;
  padding: 15px;
  text-align: center;
  max-width: 800px;
  opacity: 0;
  transform: translateY(20px);
  position: relative;
  letter-spacing: 0.5px;
}

.caption-paragraph::first-letter {
  font-size: 36px;
  font-weight: bold;
  color: #f0f0f0; 
  float: left;
  margin-right: 8px;
  margin-bottom: -5px;
  text-shadow: 
    0 0 8px rgba(240, 240, 240, 0.8),
    0 0 15px rgba(240, 240, 240, 0.5);
}

.next-button {
  position: absolute;
  bottom: 40px;
  right: 40px;
  padding: 12px 30px;
  background: linear-gradient(to bottom, #8b4513, #5d2906);
  color: #ffd700;
  border: 2px solid #d4af37;
  border-radius: 8px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  text-shadow: 1px 1px 2px #000;
  box-shadow: 
    0 5px 0 #5d2906, 
    inset 0 0 15px rgba(255, 215, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 100;
  pointer-events: auto; /* 确保按钮可点击 */
}

.next-button:hover {
  background: linear-gradient(to bottom, #a0522d, #8b4513);
  transform: translateY(-3px);
  box-shadow: 
    0 8px 0 #5d2906, 
    inset 0 0 20px rgba(255, 215, 0, 0.5);
}

.comic-effects {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: -1;
}

.effect {
  position: absolute;
  border-radius: 50%;
  background: rgba(180, 135, 40, 0.3);
  box-shadow: 0 0 15px rgba(249, 224, 118, 0.5);
  animation: float 15s infinite ease-in-out;
}

.effect-1 {
  top: 20%;
  left: 10%;
  width: 100px;
  height: 100px;
  animation-delay: 0s;
}

.effect-2 {
  top: 60%;
  left: 80%;
  width: 80px;
  height: 80px;
  animation-delay: 2s;
}

.effect-3 {
  top: 40%;
  left: 40%;
  width: 60px;
  height: 60px;
  animation-delay: 4s;
}

/* 动画定义 */
@keyframes float {
  0%,
  100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(-20px, 15px);
  }
  50% {
    transform: translate(10px, -20px);
  }
  75% {
    transform: translate(15px, 10px);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 1.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .comic-panels {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr;
    gap: 8px; /* 移动端间距更小 */
  }

  .page-1 .panel-1,
  .page-1 .panel-2,
  .page-1 .panel-3,
  .page-2 .panel-4,
  .page-2 .panel-5 {
    grid-column: 1;
    grid-row: 1;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
  }

  .global-caption {
    width: 95%;
    height: 25%;
  }

  .caption-paragraph {
    font-size: 1rem;
    padding: 10px;
  }
  
  .caption-paragraph::first-letter {
    font-size: 24px;
    margin-bottom: -3px;
  }

  .next-button {
    padding: 10px 20px;
    font-size: 16px;
    bottom: 20px;
    right: 20px;
  }

  .page {
    height: 65%; /* 移动端高度调整 */
  }
}
</style>