<!-- Card.vue -->
<template>
  <div
      class="card-wrap"
      @mousemove="handleMouseMove"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
      @click="handleClick"
      ref="cardRef"
  >
    <div class="card" :style="cardStyle">
      <div class="card-bg" :style="[cardBgTransform, cardBgImage]"></div>
      <div class="card-info">
        <slot name="header"></slot>
        <slot name="content"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';

const props = defineProps({
  dataImage: String,
});

const emit = defineEmits(['click'])

const handleClick = (e) => {
  // 阻止事件冒泡
  e.stopPropagation();

  // 发射点击事件到父组件
  emit('click', e);

  console.log('Card 组件: click 事件已发射');
};

const cardRef = ref(null);
const width = ref(0);
const height = ref(0);
const mouseX = ref(0);
const mouseY = ref(0);
let mouseLeaveDelay = null;

onMounted(() => {
  if (cardRef.value) {
    width.value = cardRef.value.offsetWidth;
    height.value = cardRef.value.offsetHeight;
  }
});

const mousePX = computed(() => mouseX.value / width.value);
const mousePY = computed(() => mouseY.value / height.value);

const cardStyle = computed(() => {
  const rX = mousePX.value * 30;
  const rY = mousePY.value * -30;
  return {
    transform: `rotateY(${rX}deg) rotateX(${rY}deg)`,
  };
});

const cardBgTransform = computed(() => {
  const tX = mousePX.value * -40;
  const tY = mousePY.value * -40;
  return {
    transform: `translateX(${tX}px) translateY(${tY}px)`,
  };
});

const cardBgImage = computed(() => {
  return {
    backgroundImage: `url(${props.dataImage})`,
  };
});

const handleMouseMove = (e) => {
  if (cardRef.value) {
    mouseX.value = e.pageX - cardRef.value.offsetLeft - width.value / 2;
    mouseY.value = e.pageY - cardRef.value.offsetTop - height.value / 2;
  }
};

const handleMouseEnter = () => {
  clearTimeout(mouseLeaveDelay);
};

const handleMouseLeave = () => {
  mouseLeaveDelay = setTimeout(() => {
    mouseX.value = 0;
    mouseY.value = 0;
  }, 1000);
};
</script>

<style lang="less">
@hoverEasing: cubic-bezier(0.23, 1, 0.32, 1);
@returnEasing: cubic-bezier(0.445, 0.05, 0.55, 0.95);

p {
  line-height: 1.5em;

  & + p,
  h1 + & {
    margin-top: 10px;
  }
}

//.container {
//  padding: 40px;
//  display: flex;
//  flex-wrap: wrap;
//}

.card-wrap {
  margin: 10px;
  transform: perspective(800px);
  transform-style: preserve-3d;
  cursor: pointer;
  /* 确保点击区域正确 */
  position: relative;
  z-index: 1;

  &:hover {
    .card-info {
      transform: translateY(0);

      &, p {
        transition: 0.6s @hoverEasing;
      }

      p {
        opacity: 1;
      }

      &:after {
        transition: 5s @hoverEasing;
        opacity: 1;
        transform: translateY(0);
      }
    }

    .card-bg {
      transition:
          0.6s @hoverEasing,
          opacity 5s @hoverEasing;
      opacity: 0.8;
    }

    .card {
      transition:
          0.6s @hoverEasing,
          box-shadow 2s @hoverEasing;
      box-shadow:
          rgba(255, 255, 255, 0.2) 0 0 40px 5px,
          rgba(255, 255, 255, 1) 0 0 0 1px,
          rgba(0, 0, 0, 0.66) 0 30px 60px 0,
          inset #333 0 0 0 5px,
          inset rgba(255, 255, 255, 0.5) 0 0 0 6px;
    }
  }
}

.card {
  position: relative;
  flex: 0 0 240px;
  width: 320px;
  height: 240px;
  background-color: #333;
  overflow: hidden;
  border-radius: 10px;
  box-shadow:
      rgba(0, 0, 0, 0.66) 0 30px 60px 0,
      inset #333 0 0 0 5px,
      inset rgba(255, 255, 255, 0.5) 0 0 0 6px;
  transition: 1s @returnEasing;
  /* 确保卡片本身可以被点击 */
  pointer-events: auto;
}

.card-bg {
  opacity: 0.5;
  position: absolute;
  top: -60px;
  left: -20px;
  width: 120%;
  height: 140%;
  padding: 20px;
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  transition:
      1s @returnEasing,
      opacity 5s 1s @returnEasing;
  pointer-events: none; /* 背景不阻止点击 */
}

.card-info {
  padding: 20px;
  position: absolute;
  bottom: 0;
  color: #fff;
  transform: translateY(40%);
  transition: 0.6s 1.6s cubic-bezier(0.215, 0.61, 0.355, 1);
  /* 确保内容区域也可以被点击 */
  pointer-events: none; /* 让点击事件穿透到父元素 */

  p {
    opacity: 0;
    text-shadow: rgba(0, 0, 0, 1) 0 2px 3px;
    transition: 0.6s 1.6s cubic-bezier(0.215, 0.61, 0.355, 1);
  }

  * {
    position: relative;
    z-index: 1;
  }

  &:after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    z-index: 0;
    width: 100%;
    height: 100%;
    background-image: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, 0.6) 100%);
    background-blend-mode: overlay;
    opacity: 0;
    transform: translateY(100%);
    transition: 5s 1s @returnEasing;
  }

  h1 {
    font-family: "Playfair Display";
    font-size: 22px;
    font-weight: 700;
    text-shadow: rgba(0, 0, 0, 0.5) 0 10px 10px;
  }
}
</style>