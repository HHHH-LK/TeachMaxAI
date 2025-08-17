<template>
  <transition name="modal">
    <div class="exit-confirmation-modal" v-if="visible">
      <div class="modal-overlay" @click="cancelExit"></div>

      <div class="modal-container">
        <!-- 符文装饰 -->
        <div class="rune-decoration">
          <div class="rune rune-top-left">ᚠ</div>
          <div class="rune rune-top-right">ᚢ</div>
          <div class="rune rune-bottom-left">ᚦ</div>
          <div class="rune rune-bottom-right">ᚨ</div>
        </div>

        <!-- 模态框内容 -->
        <div class="modal-content">
          <div class="modal-header">
            <h2 class="modal-title">警告</h2>
            <div class="title-decoration"></div>
          </div>

          <div class="modal-body">
            <p class="warning-text">你确定要退出吗？</p>
            <p class="warning-text">所有未保存的进度将会丢失！</p>
            <div class="skull-icon">
              <i class="fas fa-skull"></i>
            </div>
          </div>

          <div class="modal-footer">
            <button class="confirm-button" @click="confirmExit">
              <span class="button-text">确认退出</span>
              <span class="button-glow"></span>
            </button>
            <button class="cancel-button" @click="cancelExit">
              <span class="button-text">取消</span>
              <span class="button-glow"></span>
            </button>
          </div>
        </div>

        <!-- 悬浮粒子效果 -->
        <div class="floating-particles">
          <div class="particle"></div>
          <div class="particle"></div>
          <div class="particle"></div>
          <div class="particle"></div>
          <div class="particle"></div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: "ExitConfirmationModal",
  props: {
    visible: { // 修改属性名为visible
      type: Boolean,
      default: false,
    },
  },
  methods: {
    confirmExit() {
      this.$emit("confirm");
    },
    cancelExit() {
      this.$emit("cancel");
    },
  },
};
</script>
主要修复点
<style scoped>
@import url("https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css");
@import url("https://fonts.googleapis.com/css2?family=Cinzel:wght@400;700;900&family=MedievalSharp&display=swap");

.modal-enter-active,
.modal-leave-active {
  transition: all 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.modal-enter .modal-overlay,
.modal-leave-to .modal-overlay {
  opacity: 0;
}

.modal-enter .modal-container,
.modal-leave-to .modal-container {
  opacity: 0;
  transform: translateY(30px) scale(0.9);
  filter: blur(5px);
}

.modal-enter-to .modal-container,
.modal-leave .modal-container {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.exit-confirmation-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20000;
}

.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(
    ellipse at center,
    rgba(8, 0, 15, 0.7),
    rgba(3, 0, 8, 0.9)
  );
  backdrop-filter: blur(10px);
  z-index: 1;
}

.modal-container {
  position: relative;
  width: 500px;
  max-width: 90%;
  background: linear-gradient(
      135deg,
      rgba(30, 10, 30, 0.8),
      rgba(50, 15, 35, 0.7)
    ),
    url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><rect width="20" height="20" fill="%23201020" opacity="0.3"/></svg>');
  border-radius: 15px;
  border: 3px solid #8b0000;
  box-shadow: 0 0 30px rgba(139, 0, 0, 0.6),
    inset 0 0 20px rgba(178, 34, 34, 0.4);
  padding: 30px;
  z-index: 2;
  overflow: hidden;
}

.rune-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.rune {
  position: absolute;
  font-size: 32px;
  color: #8b0000;
  text-shadow: 0 0 10px rgba(255, 0, 0, 0.7);
  opacity: 0.7;
  animation: runeGlow 3s infinite alternate;
}

.rune-top-left {
  top: 15px;
  left: 15px;
  animation-delay: 0s;
}

.rune-top-right {
  top: 15px;
  right: 15px;
  animation-delay: 0.5s;
}

.rune-bottom-left {
  bottom: 15px;
  left: 15px;
  animation-delay: 1s;
}

.rune-bottom-right {
  bottom: 15px;
  right: 15px;
  animation-delay: 1.5s;
}

@keyframes runeGlow {
  0% {
    text-shadow: 0 0 5px rgba(255, 0, 0, 0.5);
    transform: scale(1);
  }
  100% {
    text-shadow: 0 0 15px rgba(255, 0, 0, 0.9);
    transform: scale(1.1);
  }
}

.modal-content {
  position: relative;
  z-index: 3;
}

.modal-header {
  text-align: center;
  margin-bottom: 25px;
  position: relative;
}

.modal-title {
  font-size: 36px;
  color: #ff4040;
  font-weight: 900;
  letter-spacing: 3px;
  text-shadow: 0 0 10px rgba(255, 0, 0, 0.7), 0 0 20px rgba(139, 0, 0, 0.5);
  font-family: "MedievalSharp", cursive;
  margin-bottom: 15px;
}

.title-decoration {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  height: 3px;
  background: linear-gradient(90deg, transparent, #8b0000, transparent);
  box-shadow: 0 0 10px rgba(255, 0, 0, 0.5);
}

.modal-body {
  text-align: center;
  margin-bottom: 30px;
}

.warning-text {
  font-size: 22px;
  color: #ff7878;
  line-height: 1.6;
  margin-bottom: 25px;
  text-shadow: 0 0 5px rgba(0, 0, 0, 0.7);
  font-family: "Cinzel", serif;
}

.skull-icon {
  font-size: 48px;
  color: #8b0000;
  text-shadow: 0 0 15px rgba(255, 0, 0, 0.7);
  animation: pulse 2s infinite alternate;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    text-shadow: 0 0 10px rgba(255, 0, 0, 0.7);
  }
  100% {
    transform: scale(1.1);
    text-shadow: 0 0 20px rgba(255, 0, 0, 0.9);
  }
}

.modal-footer {
  display: flex;
  justify-content: center;
  gap: 25px;
}

.confirm-button,
.cancel-button {
  position: relative;
  padding: 12px 35px;
  border: none;
  border-radius: 8px;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  font-family: "MedievalSharp", cursive;
  letter-spacing: 2px;
  z-index: 2;
}

.confirm-button {
  background: linear-gradient(to bottom, #8b0000, #5d0000);
  color: #ffcccc;
  box-shadow: 0 5px 15px rgba(139, 0, 0, 0.5),
    inset 0 0 10px rgba(255, 0, 0, 0.3);
}

.cancel-button {
  background: linear-gradient(to bottom, #2d2d5d, #1a1a3a);
  color: #aaccff;
  box-shadow: 0 5px 15px rgba(0, 0, 100, 0.5),
    inset 0 0 10px rgba(100, 149, 237, 0.3);
}

.button-glow {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.confirm-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(139, 0, 0, 0.7),
    inset 0 0 15px rgba(255, 0, 0, 0.5);
}

.confirm-button:hover .button-glow {
  opacity: 0.5;
  animation: shine 2s infinite linear;
}

.cancel-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 150, 0.7),
    inset 0 0 15px rgba(100, 149, 237, 0.5);
}

.cancel-button:hover .button-glow {
  opacity: 0.5;
  animation: shine 2s infinite linear;
}

@keyframes shine {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.floating-particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 0, 0, 0.3);
  box-shadow: 0 0 10px rgba(255, 0, 0, 0.5);
  animation: float 15s infinite ease-in-out;
}

.particle:nth-child(1) {
  width: 8px;
  height: 8px;
  top: 20%;
  left: 20%;
  animation-delay: 0s;
}

.particle:nth-child(2) {
  width: 10px;
  height: 10px;
  top: 60%;
  left: 70%;
  animation-delay: -3s;
}

.particle:nth-child(3) {
  width: 6px;
  height: 6px;
  top: 40%;
  left: 40%;
  animation-delay: -6s;
}

.particle:nth-child(4) {
  width: 12px;
  height: 12px;
  top: 75%;
  left: 30%;
  animation-delay: -9s;
}

.particle:nth-child(5) {
  width: 5px;
  height: 5px;
  top: 10%;
  left: 80%;
  animation-delay: -12s;
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(-15px, 10px);
  }
  50% {
    transform: translate(10px, -15px);
  }
  75% {
    transform: translate(15px, 10px);
  }
}

@media (max-width: 600px) {
  .modal-container {
    padding: 20px;
  }

  .modal-title {
    font-size: 28px;
  }

  .warning-text {
    font-size: 18px;
  }

  .skull-icon {
    font-size: 36px;
  }

  .modal-footer {
    flex-direction: column;
    gap: 15px;
  }

  .confirm-button,
  .cancel-button {
    width: 100%;
    padding: 10px;
  }
}
</style>
