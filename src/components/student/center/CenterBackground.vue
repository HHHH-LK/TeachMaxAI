<template>
  <div class="box"></div>
  <div class="cav">
    <canvas ref="canvasRef"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";

// 引用canvas元素
const canvasRef = ref(null);
// 画布上下文
let ctx = null;
// 画布尺寸
let cWidth = 0;
let cHeight = 0;
// 鼠标状态
const mouse = { x: null, y: null, max: 20000 };
// 粒子数组
let dots = [];
// 动画定时器
let animateTimer = null;

// 窗口尺寸调整处理
const resize = () => {
  cWidth = canvasRef.value.width =
    window.innerWidth ||
    document.documentElement.clientWidth ||
    document.body.clientWidth;
  cHeight = canvasRef.value.height =
    window.innerHeight ||
    document.documentElement.clientHeight ||
    document.body.clientHeight;
};

// 鼠标移动处理
const handleMouseMove = (e) => {
  e = e || window.event;
  mouse.x = e.clientX;
  mouse.y = e.clientY;
};

// 鼠标移出处理
const handleMouseOut = () => {
  mouse.x = null;
  mouse.y = null;
};

// 粒子初始化
const initDots = () => {
  dots = [];
  for (let i = 0; i < 300; i++) {
    const x = Math.random() * cWidth;
    const y = Math.random() * cHeight;
    const moveX = Math.random() * 2 - 1;
    const moveY = Math.random() * 2 - 1;
    dots.push({ x, y, moveX, moveY, max: 6000 });
  }
};

// 动画循环
const animate = () => {
  ctx.clearRect(0, 0, cWidth, cHeight);
  const allDots = [mouse].concat(dots);

  dots.forEach((dot) => {
    // 粒子位移
    dot.x += dot.moveX;
    dot.y += dot.moveY;
    // 边界反弹
    dot.moveX *= dot.x > cWidth || dot.x < 0 ? -1 : 1;
    dot.moveY *= dot.y > cHeight || dot.y < 0 ? -1 : 1;
    // 绘制点
    ctx.fillRect(dot.x - 0.5, dot.y - 0.5, 1, 1);

    // 计算粒子间连线
    for (let i = 0; i < allDots.length; i++) {
      const tempDot = allDots[i];
      if (dot === tempDot || tempDot.x === null || tempDot.y === null) continue;

      const _x = dot.x - tempDot.x;
      const _y = dot.y - tempDot.y;
      const dis = _x * _x + _y * _y;
      let ratio;

      if (dis < tempDot.max) {
        // 鼠标吸引效果
        if (tempDot === mouse && dis > tempDot.max / 2) {
          dot.x -= _x * 0.03;
          dot.y -= _y * 0.03;
        }

        ratio = (tempDot.max - dis) / tempDot.max;
        // 绘制连线
        ctx.beginPath();
        ctx.lineWidth = ratio / 2;
        ctx.strokeStyle = `rgba(53, 121, 189,${ratio + 0.2})`;
        ctx.moveTo(dot.x, dot.y);
        ctx.lineTo(tempDot.x, tempDot.y);
        ctx.stroke();
      }
    }
    allDots.splice(allDots.indexOf(dot), 1);
  });

  animateTimer = setTimeout(animate, 1000 / 60);
};

// 生命周期：组件挂载时初始化
onMounted(() => {
  if (!canvasRef.value) return;
  ctx = canvasRef.value.getContext("2d");
  resize();
  window.addEventListener("resize", resize);
  window.addEventListener("mousemove", handleMouseMove);
  window.addEventListener("mouseout", handleMouseOut);
  initDots();
  setTimeout(animate, 100);
});

// 生命周期：组件卸载时清理
onUnmounted(() => {
  window.removeEventListener("resize", resize);
  window.removeEventListener("mousemove", handleMouseMove);
  window.removeEventListener("mouseout", handleMouseOut);
  clearTimeout(animateTimer);
});
</script>

<style scoped>
.box {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: white;
  z-index: -1;
}
/* .cav {
  opacity: 0.7;
} */
</style>
