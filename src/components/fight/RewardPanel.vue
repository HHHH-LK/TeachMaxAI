<template>
  <transition name="panel">
    <div class="reward-panel" v-if="show">
      <div class="panel-bg-overlay"></div>
      
      <div class="exit-label arrow-container left-arrow" @click="closePanel">
        <div class="exit-content">
          <span class="exit-text">退出</span>
        </div>
      </div>
      
      <div class="next-wrapper">
        <button class="big-arrow-button arrow-container right-arrow" @click="goToNextLevel">
          <span class="btn-text">进入下一层</span>
        </button>
      </div>

      <!-- 卷轴容器 -->
      <div class="scroll-container">
        <!-- 卷轴顶部装饰 -->
        <div class="scroll-top">
          <div class="scroll-knob left"></div>
          <div class="scroll-knob right"></div>
        </div>
        
        <!-- 卷轴内容区域 -->
        <div class="scroll-content">
          <!-- 烧焦边缘效果 -->
          <div class="burned-edge top"></div>
          <div class="burned-edge bottom"></div>
          
          <!-- 面板头部 -->
          <div class="panel-header">
            <div class="scroll-banner">
              <span class="banner-text">好好搜刮 </span>
              <div class="banner-decoration"></div>
            </div>
          </div>

          <!-- 奖励区域（可滚动） -->
          <div class="scrollable-content">
            <div class="panel-content">
              <div class="rewards-section">
                <div class="rewards-card">
                  <div
                    v-for="(reward, index) in rewards"
                    :key="index"
                    class="reward-item"
                    :class="{ selected: selectedReward === index }"
                    @click="selectReward(index)"
                  >
                    <div class="reward-left">
                      <div class="reward-icon">
                        <i class="fas" :class="getRewardIcon(index)"></i>
                      </div>
                    </div>
                    <div class="reward-right">
                      <div class="reward-text">{{ reward }}</div>
                      <div class="reward-glow"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 卷轴底部装饰 -->
        <div class="scroll-bottom">
          <div class="scroll-knob left"></div>
          <div class="scroll-knob right"></div>
        </div>
      </div>
      
      <!-- 全局悬浮粒子效果 -->
      <div class="floating-particles">
        <div class="particle"></div>
        <div class="particle"></div>
        <div class="particle"></div>
        <div class="particle"></div>
        <div class="particle"></div>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: "RewardPanelScroll",
  props: {
    show: { type: Boolean, default: false },
    healthValue: { type: Number, default: 63 },
    maxHealth: { type: Number, default: 75 },
    charImg: { type: String, default: "" },
  },
  data() {
    return {
      rewards: ["100经验", "生命药水", "神秘卷轴", "黄金护符", "龙之鳞片", "精灵之泪", "矮人战锤", "巫师法杖"],
      selectedReward: null,
    };
  },
  computed: {
    healthPercentage() {
      const pct = (this.healthValue / this.maxHealth) * 100;
      return Math.max(0, Math.min(100, pct));
    },
  },
  methods: {
    getRewardIcon(index) {
      const icons = ["fa-gem", "fa-flask", "fa-scroll", "fa-medal", "fa-dragon", "fa-tint", "fa-hammer", "fa-staff"];
      return icons[index];
    },
    selectReward(index) {
      this.selectedReward = index;
      this.$emit("select", index);
    },
    closePanel() {
      this.$emit("close");
    },
    skipRewards() {
      this.$emit("skip");
    },
    goToNextLevel() {
      this.$emit("next-level");
    },
  },
};
</script>

<style scoped>
/* 引入字体图标 */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css');
@import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@400;700;900&family=MedievalSharp&display=swap');

/* ==================== 过渡动画增强 ==================== */
.panel-enter-active,
.panel-leave-active {
  transition: all 0.8s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.panel-enter-active .panel-bg-overlay,
.panel-leave-active .panel-bg-overlay {
  transition: opacity 0.6s ease;
}

.panel-enter .panel-bg-overlay,
.panel-leave-to .panel-bg-overlay {
  opacity: 0;
}

.panel-enter-active .scroll-container,
.panel-leave-active .scroll-container {
  transition: all 0.7s cubic-bezier(0.175, 0.885, 0.32, 1.275) 0.15s;
}

.panel-enter .scroll-container,
.panel-leave-to .scroll-container {
  opacity: 0;
  transform: translateY(50px) scale(0.85);
  filter: blur(5px);
}

.panel-enter-to .scroll-container,
.panel-leave .scroll-container {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.reward-item {
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* ==================== 背景与覆盖层 ==================== */
.panel-bg-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(ellipse at center, rgba(8, 0, 15, 0.3) 0%, rgba(3, 0, 8, 0.5) 70%);
  backdrop-filter: blur(15px);
  z-index: 0;
}

.reward-panel {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  perspective: 1200px;
  overflow: hidden;
  font-family: 'Cinzel', 'MedievalSharp', serif;
}

/* ==================== 全局悬浮粒子效果 ==================== */
.floating-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 2;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(180, 135, 40, 0.3);
  box-shadow: 0 0 15px rgba(249, 224, 118, 0.5);
  animation: float 15s infinite ease-in-out;
}

.particle:nth-child(1) {
  width: 8px;
  height: 8px;
  top: 20%;
  left: 15%;
  animation-delay: 0s;
}

.particle:nth-child(2) {
  width: 12px;
  height: 12px;
  top: 60%;
  left: 80%;
  animation-delay: -5s;
}

.particle:nth-child(3) {
  width: 6px;
  height: 6px;
  top: 40%;
  left: 50%;
  animation-delay: -10s;
}

.particle:nth-child(4) {
  width: 10px;
  height: 10px;
  top: 75%;
  left: 25%;
  animation-delay: -7s;
}

.particle:nth-child(5) {
  width: 5px;
  height: 5px;
  top: 10%;
  left: 70%;
  animation-delay: -12s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  25% { transform: translate(-20px, 15px); }
  50% { transform: translate(10px, -20px); }
  75% { transform: translate(15px, 10px); }
}

/* ==================== 箭头形状容器 ==================== */
.arrow-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  z-index: 10;
}

.arrow-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  transition: all 0.3s ease;
}

/* ==================== 左侧退出按钮 - 向左箭头形状 ==================== */
.exit-label {
  position: absolute;
  left: 6%;
  top: 52%;
  transform: translateY(-50%);
  width: 150px;
  height: 60px;
}

.left-arrow {
  clip-path: polygon(0 50%, 20% 0, 100% 0, 100% 100%, 20% 100%);
}

.left-arrow::before {
  background: 
    linear-gradient(135deg, rgba(80, 20, 20, 0.8), rgba(120, 20, 20, 0.6)),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" fill="%23403020" opacity="0.2"/></svg>');
  border: 2px solid rgba(139, 0, 0, 0.5);
  box-shadow: 
    0 10px 25px rgba(139, 0, 0, 0.4),
    inset 0 0 15px rgba(178, 34, 34, 0.3);
}

.exit-content {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  height: 100%;
  padding-right: 40px;
  color: #ff7878;
  font-weight: 700;
  font-size: 22px;
  letter-spacing: 3px;
  text-shadow: 0 0 10px rgba(255, 60, 60, 0.7);
  transition: all 0.3s ease;
}

.exit-label:hover {
  transform: translateY(-50%) translateX(-5px);
}

.exit-label:hover .exit-content {
  color: #ff4040;
  text-shadow: 0 0 15px rgba(255, 60, 60, 0.9);
}

.exit-label:hover::before {
  background: 
    linear-gradient(135deg, rgba(120, 20, 20, 0.9), rgba(160, 30, 30, 0.7)),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" fill="%23403020" opacity="0.2"/></svg>');
  box-shadow: 
    0 10px 30px rgba(178, 34, 34, 0.6),
    inset 0 0 20px rgba(205, 92, 92, 0.4);
  border-color: rgba(178, 34, 34, 0.6);
}

/* ==================== 右侧进入下一层按钮 - 向右箭头形状 ==================== */
.next-wrapper {
  position: absolute;
  right: 6%;
  top: 52%;
  transform: translateY(-50%);
}

.big-arrow-button {
  width: 180px;
  height: 60px;
  padding: 0;
  border: none;
  background: transparent;
}

.right-arrow {
  clip-path: polygon(0 0, 80% 0, 100% 50%, 80% 100%, 0 100%);
}

.right-arrow::before {
  background: 
    linear-gradient(135deg, #5D2906, #8B4513, #5D2906),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" fill="%23403020" opacity="0.2"/></svg>');
  box-shadow: 
    0 8px 20px rgba(139, 69, 19, 0.6),
    inset 0 4px 10px rgba(255, 215, 0, 0.3);
}

.btn-text {
  color: #ff9100;
  font-weight: 800;
  font-size: 18px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.7);
  letter-spacing: 1px;
  /* font-family: 'MedievalSharp', cursive; */
  /* padding-left: 25px; */
  transition: all 0.3s ease;
}

/* .big-arrow-button:hover {
  transform: translateY(-50%) translateX(-5px);
} */

.big-arrow-button:hover .btn-text {
  color: #FFEC8B;
  text-shadow: 0 0 8px rgba(255, 215, 0, 0.8);
}

.big-arrow-button:hover::before {
  background: 
    linear-gradient(135deg, #8B4513, #A0522D, #8B4513),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" fill="%23403020" opacity="0.2"/></svg>');
  box-shadow: 
    0 12px 25px rgba(160, 82, 45, 0.7),
    inset 0 4px 10px rgba(255, 215, 0, 0.5);
}


.scroll-container {
  position: relative;
  width: 40%;
  max-width: 800px;
  margin: 0 auto;
  z-index: 5;
  perspective: 1000px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8);
}

.scroll-top, .scroll-bottom {
  position: relative;
  width: 100%;
  height: 60px;
  background: 
    linear-gradient(to bottom, #5D2906, #3A1804, #5D2906),
    repeating-linear-gradient(90deg, 
      transparent, 
      transparent 10px, 
      rgba(139, 69, 19, 0.5) 10px, 
      rgba(139, 69, 19, 0.5) 20px
    );
  z-index: 3;
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
}

.scroll-top {
  border-radius: 50% 50% 0 0;
  border-bottom: 3px solid #3A1804;
}

.scroll-bottom {
  border-radius: 0 0 50% 50%;
  border-top: 3px solid #3A1804;
}

.scroll-knob {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #8B4513, #A0522D, #8B4513);
  border-radius: 50%;
  position: relative;
  top: 10px;
  box-shadow: 
    inset 0 0 10px rgba(0, 0, 0, 0.3),
    0 5px 15px rgba(0, 0, 0, 0.4);
  border: 2px solid #3A1804;
}

.scroll-knob::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(160, 82, 45, 0.5);
  box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.5);
}


.scroll-content {
  background: 
    linear-gradient(to bottom, rgba(196, 156, 132, 0.9), rgba(180, 153, 145, 0.9)),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect width="100" height="100" fill="%23f5deb3" opacity="0.3"/><path d="M0,20 Q30,15 50,20 T100,20 L100,80 Q70,85 50,80 T0,80 Z" fill="%23d2b48c" opacity="0.2"/></svg>');
  padding: 40px;
  border-left: 15px solid #5D2906;
  border-right: 15px solid #5D2906;
  box-shadow: 
    inset 0 0 30px rgba(139, 69, 19, 0.4),
    0 10px 40px rgba(0, 0, 0, 0.6);
  position: relative;
  z-index: 2;
  min-height: 400px;
}

.scroll-content::before,
.scroll-content::after {
  content: '';
  position: absolute;
  top: 0;
  width: 15px;
  height: 100%;
  background: linear-gradient(to right, #5D2906, #3A1804, #5D2906);
  z-index: 1;
}

.scroll-content::before {
  left: -15px;
}

.scroll-content::after {
  right: -15px;
}

.burned-edge {
  position: absolute;
  left: 0;
  right: 0;
  height: 15px;
  background-image: 
    radial-gradient(circle at 10px 15px, transparent 8px, #3A1804 9px, #3A1804 10px, transparent 11px),
    radial-gradient(circle at 30px 15px, transparent 8px, #3A1804 9px, #3A1804 10px, transparent 11px),
    radial-gradient(circle at 50px 15px, transparent 8px, #3A1804 9px, #3A1804 10px, transparent 11px);
  background-size: 20px 20px;
  background-repeat: repeat-x;
  z-index: 3;
}

.burned-edge.top {
  top: -15px;
}

.burned-edge.bottom {
  bottom: -15px;
  transform: rotate(180deg);
}

.scroll-content::before,
.scroll-content::after {
  background-image: 
    linear-gradient(to bottom, 
      transparent, 
      transparent 10px, 
      #3A1804 10px, 
      #3A1804 11px, 
      transparent 11px,
      transparent 20px,
      #3A1804 20px,
      #3A1804 21px,
      transparent 21px,
      transparent 30px,
      #3A1804 30px,
      #3A1804 31px,
      transparent 31px
    );
  background-size: 100% 30px;
}

.panel-header {
  padding: 8px 0 20px 0;
  text-align: center;
  position: relative;
  overflow: hidden;
  z-index: 1;
  margin-bottom: 20px;
}

.scroll-banner {
  display: inline-block;
  position: relative;
  padding: 12px 60px;
}

.banner-text {
  font-size: 38px;
  color: #8B0000;
  font-weight: 900;
  letter-spacing: 3px;
  text-shadow: 
    0 2px 2px rgba(139, 0, 0, 0.3),
    0 0 10px rgba(255, 215, 0, 0.5);
  position: relative;
  z-index: 2;
  font-family: 'MedievalSharp', cursive;
}

.banner-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(139, 0, 0, 0.2), transparent);
  border-radius: 8px;
  z-index: 1;
  animation: bannerMove 10s infinite linear;
}

@keyframes bannerMove {
  0% { background-position: -200px 0; }
  100% { background-position: 200px 0; }
}

.scrollable-content {
  max-height: 50vh;
  overflow-y: auto;
  padding: 0 10px;
  margin: 0 -10px;
}

.scrollable-content::-webkit-scrollbar {
  width: 12px;
}

.scrollable-content::-webkit-scrollbar-track {
  background: rgba(139, 69, 19, 0.2);
  border-radius: 10px;
}

.scrollable-content::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, #8B4513, #5D2906);
  border-radius: 10px;
  border: 2px solid rgba(160, 82, 45, 0.5);
}

.scrollable-content::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(to bottom, #A0522D, #8B4513);
}

.panel-content {
  padding: 20px 0;
  min-height: 300px;
  position: relative;
}

.rewards-section {
  width: 100%;
}

.rewards-card {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.reward-item {
  display: flex;
  align-items: center;
  padding: 20px;
  border: 2px solid #8B4513;
  border-radius: 8px;
  background: rgba(245, 222, 179, 0.7);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  z-index: 1;
  box-shadow: 
    inset 0 0 10px rgba(139, 69, 19, 0.3),
    0 5px 15px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.reward-item::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #8B0000, transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.reward-item:hover {
  background: rgba(255, 248, 220, 0.8);
  transform: translateY(-5px);
  box-shadow: 
    inset 0 0 15px rgba(139, 69, 19, 0.4),
    0 8px 20px rgba(0, 0, 0, 0.3);
}

.reward-item:hover::before {
  opacity: 1;
}

.reward-left {
  width: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

.reward-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  border: 2px solid #8B0000;
  background: rgba(255, 215, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #8B0000;
  box-shadow: 
    inset 0 0 10px rgba(139, 0, 0, 0.3),
    0 0 10px rgba(255, 215, 0, 0.3);
  transition: all 0.3s ease;
}

.reward-item:hover .reward-icon {
  transform: scale(1.1);
  box-shadow: 
    inset 0 0 15px rgba(139, 0, 0, 0.4), 
    0 0 20px rgba(228, 219, 166, 0.5);
  color: #B22222;
  background: rgba(255, 215, 0, 0.3);
}

.reward-right { 
  flex: 1;
  padding-left: 25px;
  position: relative;
  z-index: 2;
}

.reward-text {
  font-size: 22px;
  color: #8B0000;
  font-weight: 700;
  margin-bottom: 8px;
  transition: color 0.3s ease;
  letter-spacing: 1px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  font-family: 'MedievalSharp', cursive;
}

.reward-item:hover .reward-text {
  color: #B22222;
  text-shadow: 0 0 8px rgba(118, 99, 51, 0.896);
}

.reward-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(255, 215, 0, 0.2), transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.reward-item:hover .reward-glow {
  opacity: 0.5;
}

.reward-item.selected {
  background: rgba(255, 248, 220, 0.9);
  transform: translateY(-5px);
  border-color: #B22222;
  box-shadow: 
    inset 0 0 15px rgba(178, 34, 34, 0.4),
    0 0 30px rgba(255, 215, 0, 0.5);
  animation: pulseShadow 1.5s infinite alternate;
}

.reward-item.selected .reward-icon {
  color: #B22222;
  background: rgba(255, 215, 0, 0.4);
  box-shadow: 
    inset 0 0 20px rgba(178, 34, 34, 0.5),
    0 0 25px rgba(255, 215, 0, 0.7);
}

.reward-item.selected .reward-text {
  color: #8B0000;
  text-shadow: 0 0 10px rgba(255, 215, 0, 0.8);
}

.reward-item.selected .reward-glow {
  opacity: 0.7;
  animation: glowPulse 2s infinite alternate;
}

@keyframes pulseShadow {
  0% { box-shadow: inset 0 0 15px rgba(178, 34, 34, 0.4), 0 0 20px rgba(255, 215, 0, 0.5); }
  100% { box-shadow: inset 0 0 20px rgba(178, 34, 34, 0.6), 0 0 40px rgba(255, 215, 0, 0.7); }
}

@keyframes glowPulse {
  0% { opacity: 0.5; }
  100% { opacity: 0.8; }
}


/* ==================== 响应式设计 ==================== */
@media (max-width: 900px) {
  .panel-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .exit-label, .next-wrapper {
    position: static;
    transform: none;
    margin: 15px auto;
    width: 80%;
    text-align: center;
  }

  .exit-label {
    top: auto;
    bottom: 20px;
    left: 10%;
  }
  
  .next-wrapper {
    top: auto;
    bottom: 20px;
    right: 10%;
  }
  
  .banner-text {
    font-size: 30px;
  }
  
  .scroll-top, .scroll-bottom {
    height: 40px;
  }
  
  .scroll-knob {
    width: 30px;
    height: 30px;
    top: 5px;
  }
  
  .scroll-content {
    padding: 20px;
  }
  
  .reward-text {
    font-size: 18px;
  }
  
  .scrollable-content {
    max-height: 40vh;
  }
  
  .exit-label {
    width: 120px;
    height: 50px;
  }
  
  .big-arrow-button {
    width: 150px;
    height: 50px;
  }
  
  .exit-content {
    font-size: 18px;
    padding-right: 15px;
  }
  
  .btn-text {
    font-size: 16px;
    padding-left: 20px;
  }
}
</style>