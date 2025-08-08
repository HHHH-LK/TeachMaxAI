<template>
  <h1>Parallax Tilt Effect Cards</h1>
  <p>Hover over the cards.</p>
  <section class="main">
    <!-- 使用v-for动态渲染卡片 -->
    <div class="wrap" v-for="card in cardList" :key="card.id" :class="`wrap--${card.id}`">
      <div
        ref="containerEl"
        class="container"
        :class="`container--${card.id}`"
        @mousemove="handleMouseMove"
        @mouseenter="handleMouseEnter"
        @mouseleave="handleMouseLeave"
      >
        <p>{{ card.number }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
const cardList = ref([
  { id: 1, number: '01. Normal' },
  { id: 2, number: '02. Reverse' },
  { id: 3, number: '03. Normal' }
]);

// 卡片尺寸（宽300px，高360px）
const size = [300, 360];
const [w, h] = size;

// 鼠标状态与DOM引用
const mouseOnComponent = ref(false);
const containerEl = ref(null);

// 鼠标移动处理函数（保留原逻辑）
const handleMouseMove = (event) => {
  const { offsetX, offsetY } = event;
  const card = cardList.value.find(item => item.id === Number(event.currentTarget.classList[1].split('--')[1]));
  const { effect } = card;

  let X, Y;
  if (effect === 'reverse') {
    X = ((offsetX - (w/2)) / 5) / 5;
    Y = (-(offsetY - (h/2)) / 5) / 5;
  } else {
    X = (-(offsetX - (w/2)) / 5) / 5;
    Y = ((offsetY - (h/2)) / 5) / 5;
  }

  event.currentTarget.style.setProperty('--rY', X.toFixed(2));
  event.currentTarget.style.setProperty('--rX', Y.toFixed(2));
  event.currentTarget.style.setProperty('--bY', `${80 - (X/4).toFixed(2)}%`);
  event.currentTarget.style.setProperty('--bX', `${50 - (Y/4).toFixed(2)}%`);
};

// 鼠标进入/离开处理函数（保留原逻辑）
const handleMouseEnter = (event) => {
  mouseOnComponent.value = true;
  event.currentTarget.classList.add('container--active');
};

const handleMouseLeave = (event) => {
  mouseOnComponent.value = false;
  event.currentTarget.classList.remove('container--active');
  event.currentTarget.style.setProperty('--rY', 0);
  event.currentTarget.style.setProperty('--rX', 0);
  event.currentTarget.style.setProperty('--bY', '30%');
  event.currentTarget.style.setProperty('--bX', '30%');
};


// 生命周期：组件挂载时初始化事件监听（原init方法）
onMounted(() => {
  if (rootEl.value && containerEl.value) {
    rootEl.value.addEventListener('mousemove', handleMouseMove);
    rootEl.value.addEventListener('mouseenter', handleMouseEnter);
    rootEl.value.addEventListener('mouseleave', handleMouseLeave);
  }
});

// 生命周期：组件卸载时移除事件监听（防内存泄漏）
onUnmounted(() => {
  if (rootEl.value) {
    rootEl.value.removeEventListener('mousemove', handleMouseMove);
    rootEl.value.removeEventListener('mouseenter', handleMouseEnter);
    rootEl.value.removeEventListener('mouseleave', handleMouseLeave);
  }
});
</script>

<style scoped>
*,
*::after,
*::before {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  font-size: 62.5%;
}

body {
  --background-color: hsl(0, 0%, 100%);

  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;

  min-height: 100vh;
  padding: 2rem;

  /* color: hsla(0, 0%, 0%, .6); */
  background: var(--background-color);
  text-align: center;
}

h1 {
  font-size: 3.2rem;
  padding-top: 2rem;
}

h1+p {
  font-size: 1.8rem;
  padding: 2rem 0 3rem;
}

.main {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
}

.wrap {
  margin: 2rem;

  transform-style: preserve-3d;
  transform: perspective(100rem);

  cursor: pointer;
}

.container {
  --rX: 0;
  --rY: 0;
  --bX: 50%;
  --bY: 80%;

  width: 30rem;
  height: 36rem;
  border: 1px solid var(--background-color);
  border-radius: 1.6rem;
  padding: 4rem;

  display: flex;
  align-items: flex-end;

  position: relative;
  transform: rotateX(calc(var(--rX) * 1deg)) rotateY(calc(var(--rY) * 1deg));

  background: linear-gradient(hsla(233, 17%, 90%, 0.901), hsla(209, 70%, 94%, 0.993));
  background-position: var(--bX) var(--bY);
  background-size: 40rem auto;
  box-shadow: 0 0 3rem .5rem hsla(0, 0%, 0%, .2);

  transition: transform .6s 1s;
}

.container::before,
.container::after {
  content: "";

  width: 2rem;
  height: 2rem;
  border: 2px solid #000000;

  position: absolute;
  z-index: 2;

  opacity: .3;
  transition: .3s;
}

.container::before {
  top: 2rem;
  right: 2rem;

  border-bottom-width: 0;
  border-left-width: 0;
}

.container::after {
  bottom: 2rem;
  left: 2rem;

  border-top-width: 0;
  border-right-width: 0;
}

.container--active {
  transition: none;
}

.container--2 {
  filter: hue-rotate(80deg) saturate(140%);
}

.container--3 {
  filter: hue-rotate(160deg) saturate(140%);
}

.container p {
  color: hsla(0, 1%, 25%, 0.6);
  font-size: 2.2rem;
}

.wrap:hover .container::before,
.wrap:hover .container::after {
  width: calc(100% - 4rem);
  height: calc(100% - 4rem);
}

.abs-site-link {
  position: fixed;
  bottom: 20px;
  left: 20px;
  color: hsla(0, 0%, 0%, .6);
  font-size: 1.6rem;
}
</style>
