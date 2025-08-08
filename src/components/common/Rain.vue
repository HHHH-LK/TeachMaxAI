<template>
  <canvas ref="canvasRef" class="rain-canvas"></canvas>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";

// 定义响应式变量
const canvasRef = ref(null);
const rain = ref([]);
const drops = ref([]);
const gravity = 0.2;
const wind = 0.015;
const rainChance = 0.8;
let requestId = null;
let ctx = null;
let resizeTimeout = null;

// 向量类
class Vector {
  constructor(x = 0, y = 0) {
    this.x = x;
    this.y = y;
  }

  add(v) {
    if (v.x != null && v.y != null) {
      this.x += v.x;
      this.y += v.y;
    } else {
      this.x += v;
      this.y += v;
    }
    return this;
  }

  copy() {
    return new Vector(this.x, this.y);
  }
}

// 雨滴类
class Rain {
  constructor(canvasWidth) {
    this.pos = new Vector(Math.random() * canvasWidth, -50);
    this.prev = this.pos.copy();
    this.vel = new Vector();
  }

  update() {
    this.prev = this.pos.copy();
    this.vel.y += gravity;
    this.vel.x += wind;
    this.pos.add(this.vel);
  }

  draw(ctx) {
    ctx.beginPath();
    ctx.moveTo(this.pos.x, this.pos.y);
    ctx.lineTo(this.prev.x, this.prev.y);
    ctx.stroke();
  }
}

// 水花类
class Drop {
  constructor(x, y) {
    const dist = Math.random() * 7;
    const angle = Math.PI + Math.random() * Math.PI;

    this.pos = new Vector(x, y);
    this.vel = new Vector(Math.cos(angle) * dist, Math.sin(angle) * dist);
  }

  update() {
    this.vel.y += gravity;
    this.vel.x *= 0.95;
    this.vel.y *= 0.95;
    this.pos.add(this.vel);
  }

  draw(ctx) {
    ctx.beginPath();
    ctx.arc(this.pos.x, this.pos.y, 1, 0, Math.PI * 2);
    ctx.fill();
  }
}

// 更新动画
function update(canvas) {
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // 更新雨滴
  let i = rain.value.length;
  while (i--) {
    const raindrop = rain.value[i];
    raindrop.update();

    // 雨滴到达底部时创建水花
    if (raindrop.pos.y >= canvas.height) {
      let n = Math.round(4 + Math.random() * 4);
      while (n--) {
        drops.value.push(new Drop(raindrop.pos.x, canvas.height));
      }
      rain.value.splice(i, 1);
    }
    raindrop.draw(ctx);
  }

  // 更新水花
  i = drops.value.length;
  while (i--) {
    const drop = drops.value[i];
    drop.update();
    drop.draw(ctx);

    if (drop.pos.y > canvas.height) {
      drops.value.splice(i, 1);
    }
  }

  // 随机添加新雨滴
  if (Math.random() < rainChance) {
    rain.value.push(new Rain(canvas.width));
  }

  requestId = requestAnimationFrame(() => update(canvas));
}

// 处理窗口大小变化
function handleResize(canvas) {
  clearTimeout(resizeTimeout);
  resizeTimeout = setTimeout(() => {
    if (canvas) {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      rain.value = [];
      drops.value = [];
    }
  }, 100);
}

// 生命周期钩子
onMounted(() => {
  const canvas = canvasRef.value;
  if (canvas) {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    ctx = canvas.getContext("2d");
    ctx.lineWidth = 1;
    ctx.strokeStyle = "rgba(150, 150, 150, 1)";
    ctx.fillStyle = "rgba(150, 150, 150, 1)";
    update(canvas);
    window.addEventListener("resize", () => handleResize(canvas));
  }
});

onUnmounted(() => {
  if (requestId) {
    cancelAnimationFrame(requestId);
    requestId = null;
  }
  window.removeEventListener("resize", handleResize);
  rain.value = [];
  drops.value = [];
});
</script>

<style scoped>
.rain-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
  pointer-events: none; /* 关键：让鼠标事件穿透雨滴canvas */
}
</style>
