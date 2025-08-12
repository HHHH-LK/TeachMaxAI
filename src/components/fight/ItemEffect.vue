<template>
  <div v-if="visible" class="item-effect-overlay">
    <!-- 攻击力提升特效 -->
    <div v-if="effectType === 'attack-buff'" class="attack-buff-effect">
      <div class="flame-ring"></div>
      <div class="sword-glow"></div>
      <div class="buff-text">+{{ damage }} 攻击</div>
    </div>

    <!-- 防御特效 -->
    <div v-if="effectType === 'defense'" class="defense-effect">
      <div class="shield-bubble"></div>
      <div class="shield-particles"></div>
      <div class="defense-text">+{{ defense }} 防御</div>
    </div>

    <!-- 治疗特效 -->
    <div v-if="effectType === 'heal'" class="heal-effect">
      <div class="heal-bubbles"></div>
      <div class="heal-light"></div>
      <div class="heal-text">+{{ heal }} 生命</div>
    </div>
  </div>
</template>

<script>
export default {
  name: "ItemEffect",
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    effectType: {
      type: String,
      default: "attack-buff",
    },
    damage: {
      type: Number,
      default: 0,
    },
    defense: {
      type: Number,
      default: 0,
    },
    heal: {
      type: Number,
      default: 0,
    },
    position: {
      type: Object,
      default: () => ({ x: 50, y: 50 }),
    },
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        // 显示一段时间后自动隐藏
        setTimeout(() => {
          this.$emit("close");
        }, 2000);
      }
    },
  },
};
</script>

<style lang="less" scoped>
.item-effect-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10000;
}

/* 防御特效 */
.defense-effect {
  position: absolute;
  top: v-bind('position.y + "%"');
  left: v-bind('position.x + "%"');
  transform: translate(-50%, -50%);

  .shield-bubble {
    width: 200px;
    height: 200px;
    border: 10px solid rgba(0, 100, 255, 0.5);
    border-radius: 50%;
    animation: shieldBubble 1s forwards;
  }

  .shield-particles {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    animation: shieldParticles 1.5s forwards;
  }

  .defense-text {
    position: absolute;
    top: -50px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 24px;
    font-weight: bold;
    color: #0066ff;
    text-shadow: 0 0 10px rgba(0, 102, 255, 0.8);
    animation: defenseTextFloat 1.5s ease-out forwards;
  }
}

@keyframes shieldBubble {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}

@keyframes shieldParticles {
  0% {
    opacity: 0;
    background: radial-gradient(
      circle,
      rgba(0, 102, 255, 0.5),
      transparent 70%
    );
  }
  50% {
    opacity: 1;
    background: radial-gradient(
      circle,
      rgba(0, 102, 255, 0.8),
      transparent 70%
    );
  }
  100% {
    opacity: 0;
    background: radial-gradient(
      circle,
      rgba(0, 102, 255, 0.3),
      transparent 70%
    );
  }
}

@keyframes defenseTextFloat {
  0% {
    transform: translate(-50%, 0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -80px);
    opacity: 0;
  }
}

/* 治疗特效 */
.heal-effect {
  position: absolute;
  top: v-bind('position.y + "%"');
  left: v-bind('position.x + "%"');
  transform: translate(-50%, -50%);

  .heal-bubbles {
    position: absolute;
    width: 150px;
    height: 150px;
    background: radial-gradient(circle, rgba(0, 255, 0, 0.3), transparent 70%);
    border-radius: 50%;
    animation: healBubbles 1.5s forwards;
  }

  .heal-light {
    position: absolute;
    width: 200px;
    height: 200px;
    background: radial-gradient(circle, rgba(0, 255, 0, 0.5), transparent 70%);
    border-radius: 50%;
    animation: healLight 1.5s forwards;
  }

  .heal-text {
    position: absolute;
    top: -50px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 24px;
    font-weight: bold;
    color: #00ff00;
    text-shadow: 0 0 10px rgba(0, 255, 0, 0.8);
    animation: healTextFloat 1.5s ease-out forwards;
  }
}

@keyframes healBubbles {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1);
    opacity: 0.7;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

@keyframes healLight {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.5;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

@keyframes healTextFloat {
  0% {
    transform: translate(-50%, 0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -80px);
    opacity: 0;
  }
}

/* 攻击力提升特效 */
.attack-buff-effect {
  position: absolute;
  top: v-bind('position.y + "%"');
  left: v-bind('position.x + "%"');
  transform: translate(-50%, -50%);

  .flame-ring {
    position: absolute;
    width: 200px;
    height: 200px;
    border: 10px solid transparent;
    border-radius: 50%;
    box-shadow: 0 0 20px #ff4500, inset 0 0 20px #ff4500;
    animation: flamePulse 1s infinite alternate;
  }

  .sword-glow {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 100px;
    height: 200px;
    background: linear-gradient(to bottom, transparent, #ff4500, transparent);
    transform: translate(-50%, -50%);
    animation: swordGlow 1.5s infinite;
  }

  .buff-text {
    position: absolute;
    top: -50px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 24px;
    font-weight: bold;
    color: #ff4500;
    text-shadow: 0 0 10px rgba(255, 69, 0, 0.8);
    animation: buffTextFloat 1.5s ease-out forwards;
  }
}

@keyframes flamePulse {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.7;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 1;
  }
}

@keyframes swordGlow {
  0% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.5;
  }
}

@keyframes buffTextFloat {
  0% {
    transform: translate(-50%, 0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -80px);
    opacity: 0;
  }
}
</style>
