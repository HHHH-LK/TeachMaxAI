<template>
  <canvas ref="canvas" :class="canvasClass" :style="canvasStyle"></canvas>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';

// 定义Props
const props = defineProps({
  width: {
    type: Number,
    required: true
  },
  height: {
    type: Number,
    required: true
  },
  canvasClass: {
    type: String,
    default: ''
  },
  canvasStyle: {
    type: Object,
    default: () => ({})
  },
  renderType: {
    type: String,
    required: true,
    validator: (value) => ['inventory', 'profile', 'exit-confirm'].includes(value)
  }
});

// 响应式变量
const animationId = ref(null);
const ctx = ref(null);
const canvas = ref(null); // 对应模板中的canvas元素

// 清理资源
const cleanup = () => {
  // 清理动画帧
  if (animationId.value) {
    cancelAnimationFrame(animationId.value);
    animationId.value = null;
  }

  // 清理Canvas上下文
  if (ctx.value) {
    try {
      ctx.value.clearRect(0, 0, props.width, props.height);
    } catch (error) {
      console.warn('清理Canvas时出错:', error);
    }
    ctx.value = null;
  }

  canvas.value = null;
};

// 绘制血滴
const drawBloodDrop = (ctx, x, y) => {
  if (!ctx) return;

  try {
    ctx.fillStyle = '#8b0000';
    ctx.beginPath();
    ctx.ellipse(x, y, 8, 12, Math.PI / 4, 0, 2 * Math.PI);
    ctx.fill();

    // 血滴高光
    ctx.fillStyle = '#ff0000';
    ctx.beginPath();
    ctx.ellipse(x - 2, y - 3, 3, 4, Math.PI / 4, 0, 2 * Math.PI);
    ctx.fill();

    // 重置填充颜色
    ctx.fillStyle = '#8b0000';
  } catch (error) {
    console.error('绘制血滴失败:', error);
  }
};

// 绘制符文
const drawRune = (ctx, x, y) => {
  if (!ctx) return;

  try {
    const size = 20;
    ctx.beginPath();

    // 绘制简单的符文图案
    ctx.moveTo(x, y - size);
    ctx.lineTo(x + size, y);
    ctx.lineTo(x, y + size);
    ctx.lineTo(x - size, y);
    ctx.closePath();
    ctx.stroke();

    // 中心点
    ctx.beginPath();
    ctx.arc(x, y, 3, 0, 2 * Math.PI);
    ctx.fill();

    // 重置填充颜色
    ctx.fillStyle = '#ff4500';
  } catch (error) {
    console.error('绘制符文失败:', error);
  }
};

// 绘制动态阴影
const drawDynamicShadows = (ctx, width, height) => {
  if (!ctx) return;

  try {
    const time = Date.now() * 0.001;
    ctx.fillStyle = 'rgba(139, 0, 0, 0.3)';
    ctx.shadowColor = '#ff0000';
    ctx.shadowBlur = 20;

    // 绘制移动的阴影
    for (let i = 0; i < 5; i++) {
      const x = (Math.sin(time + i) * 100) + width/2;
      const y = (Math.cos(time + i * 0.7) * 80) + height/2;
      const size = 30 + Math.sin(time * 2 + i) * 10;

      ctx.beginPath();
      ctx.arc(x, y, size, 0, 2 * Math.PI);
      ctx.fill();
    }

    // 重置阴影
    ctx.shadowBlur = 0;
  } catch (error) {
    console.error('绘制动态阴影失败:', error);
  }
};

// 绘制角落装饰
const drawCornerDecorations = (ctx, width, height) => {
  if (!ctx) return;

  try {
    ctx.strokeStyle = '#daa520';
    ctx.lineWidth = 3;

    // 左上角
    ctx.beginPath();
    ctx.moveTo(30, 30);
    ctx.lineTo(60, 30);
    ctx.lineTo(60, 60);
    ctx.stroke();

    // 右上角
    ctx.beginPath();
    ctx.moveTo(width - 30, 30);
    ctx.lineTo(width - 60, 30);
    ctx.lineTo(width - 60, 60);
    ctx.stroke();

    // 左下角
    ctx.beginPath();
    ctx.moveTo(30, height - 30);
    ctx.lineTo(60, height - 30);
    ctx.lineTo(60, height - 60);
    ctx.stroke();

    // 右下角
    ctx.beginPath();
    ctx.moveTo(width - 30, height - 30);
    ctx.lineTo(width - 60, height - 30);
    ctx.lineTo(width - 60, height - 60);
    ctx.stroke();
  } catch (error) {
    console.error('绘制角落装饰失败:', error);
  }
};

// 绘制符文圆圈
const drawRuneCircle = (ctx, x, y) => {
  if (!ctx) return;

  try {
    const size = 25;

    // 外圆
    ctx.beginPath();
    ctx.arc(x, y, size, 0, 2 * Math.PI);
    ctx.stroke();

    // 内圆
    ctx.beginPath();
    ctx.arc(x, y, size * 0.6, 0, 2 * Math.PI);
    ctx.stroke();

    // 中心符文
    ctx.beginPath();
    ctx.moveTo(x, y - size * 0.3);
    ctx.lineTo(x + size * 0.3, y);
    ctx.lineTo(x, y + size * 0.3);
    ctx.lineTo(x - size * 0.3, y);
    ctx.closePath();
    ctx.stroke();
  } catch (error) {
    console.error('绘制符文圆圈失败:', error);
  }
};

// 绘制边框装饰
const drawBorderDecorations = (ctx, width, height) => {
  if (!ctx) return;

  try {
    ctx.strokeStyle = '#8b0000';
    ctx.lineWidth = 3;
    ctx.shadowColor = '#ff0000';
    ctx.shadowBlur = 10;

    // 绘制主边框
    ctx.strokeRect(20, 20, width - 40, height - 40);

    // 绘制角落装饰
    ctx.strokeStyle = '#ff4500';
    ctx.lineWidth = 2;

    // 左上角
    ctx.beginPath();
    ctx.moveTo(30, 30);
    ctx.lineTo(60, 30);
    ctx.lineTo(60, 60);
    ctx.stroke();

    // 右上角
    ctx.beginPath();
    ctx.moveTo(width - 30, 30);
    ctx.lineTo(width - 60, 30);
    ctx.lineTo(width - 60, 60);
    ctx.stroke();

    // 左下角
    ctx.beginPath();
    ctx.moveTo(30, height - 30);
    ctx.lineTo(60, height - 30);
    ctx.lineTo(60, height - 60);
    ctx.stroke();

    // 右下角
    ctx.beginPath();
    ctx.moveTo(width - 30, height - 30);
    ctx.lineTo(width - 60, height - 30);
    ctx.lineTo(width - 60, height - 60);
    ctx.stroke();
  } catch (error) {
    console.error('绘制边框装饰失败:', error);
  }
};

// 绘制恐怖元素
const drawHorrorElements = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 绘制血滴效果
    ctx.fillStyle = '#8b0000';
    ctx.shadowColor = '#ff0000';
    ctx.shadowBlur = 15;

    for (let i = 0; i < 8; i++) {
      const x = 50 + Math.random() * (width - 100);
      const y = 50 + Math.random() * (height - 100);
      drawBloodDrop(ctx, x, y);
    }

    // 绘制符文
    ctx.strokeStyle = '#ff4500';
    ctx.lineWidth = 1;
    ctx.shadowBlur = 5;

    for (let i = 0; i < 6; i++) {
      const x = 100 + Math.random() * (width - 200);
      const y = 100 + Math.random() * (height - 200);
      drawRune(ctx, x, y);
    }
  } catch (error) {
    console.error('绘制恐怖元素失败:', error);
  }
};

// 绘制发光效果
const drawGlowEffects = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 绘制边缘发光
    const glowGradient = ctx.createRadialGradient(width/2, height/2, 0, width/2, height/2, width/2);
    glowGradient.addColorStop(0, 'rgba(139, 0, 0, 0.1)');
    glowGradient.addColorStop(0.5, 'rgba(255, 69, 0, 0.05)');
    glowGradient.addColorStop(1, 'transparent');

    ctx.fillStyle = glowGradient;
    ctx.fillRect(0, 0, width, height);

    // 绘制动态阴影
    drawDynamicShadows(ctx, width, height);
  } catch (error) {
    console.error('绘制发光效果失败:', error);
  }
};

// 绘制复古边框
const drawRetroBorder = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 绘制复古风格的边框
    ctx.strokeStyle = '#8b4513';
    ctx.lineWidth = 4;
    ctx.shadowColor = '#d2691e';
    ctx.shadowBlur = 8;

    // 主边框
    ctx.strokeRect(15, 15, width - 30, height - 30);

    // 内边框
    ctx.strokeStyle = '#cd853f';
    ctx.lineWidth = 2;
    ctx.strokeRect(25, 25, width - 50, height - 50);

    // 角落装饰
    drawCornerDecorations(ctx, width, height);
  } catch (error) {
    console.error('绘制复古边框失败:', error);
  }
};

// 绘制神秘符文
const drawMysticalRunes = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 绘制神秘符文
    ctx.strokeStyle = '#daa520';
    ctx.lineWidth = 2;
    ctx.shadowColor = '#ffd700';
    ctx.shadowBlur = 5;

    // 绘制符文圆圈
    for (let i = 0; i < 6; i++) {
      const x = 80 + Math.random() * (width - 160);
      const y = 80 + Math.random() * (height - 160);
      drawRuneCircle(ctx, x, y);
    }
  } catch (error) {
    console.error('绘制神秘符文失败:', error);
  }
};

// 绘制复古纹理
const drawRetroTexture = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 绘制复古纹理效果
    ctx.fillStyle = 'rgba(139, 69, 19, 0.1)';

    for (let i = 0; i < 100; i++) {
      const x = Math.random() * width;
      const y = Math.random() * height;
      const size = Math.random() * 3 + 1;

      ctx.beginPath();
      ctx.arc(x, y, size, 0, 2 * Math.PI);
      ctx.fill();
    }
  } catch (error) {
    console.error('绘制复古纹理失败:', error);
  }
};

// 绘制个人资料发光效果
const drawProfileGlow = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 绘制边缘发光效果
    const glowGradient = ctx.createRadialGradient(width/2, height/2, 0, width/2, height/2, width/2);
    glowGradient.addColorStop(0, 'rgba(218, 165, 32, 0.1)');
    glowGradient.addColorStop(0.5, 'rgba(139, 69, 19, 0.05)');
    glowGradient.addColorStop(1, 'transparent');

    ctx.fillStyle = glowGradient;
    ctx.fillRect(0, 0, width, height);
  } catch (error) {
    console.error('绘制个人资料发光效果失败:', error);
  }
};

// 绘制库存框架
const drawInventoryFrame = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 清除画布
    ctx.clearRect(0, 0, width, height);

    // 创建渐变背景
    const gradient = ctx.createLinearGradient(0, 0, width, height);
    gradient.addColorStop(0, '#1a1a1a');
    gradient.addColorStop(0.5, '#2d1b3d');
    gradient.addColorStop(1, '#1a1a1a');

    // 绘制主框架
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);

    // 绘制边框装饰
    drawBorderDecorations(ctx, width, height);

    // 绘制恐怖元素
    drawHorrorElements(ctx, width, height);

    // 绘制发光效果
    drawGlowEffects(ctx, width, height);
  } catch (error) {
    console.error('绘制库存框架失败:', error);
  }
};

// 绘制个人资料框架
const drawProfileFrame = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 清除画布
    ctx.clearRect(0, 0, width, height);

    // 创建复古渐变背景
    const gradient = ctx.createLinearGradient(0, 0, width, height);
    gradient.addColorStop(0, '#1a0f0f');
    gradient.addColorStop(0.3, '#2d1b1b');
    gradient.addColorStop(0.7, '#1a0f0f');
    gradient.addColorStop(1, '#0a0505');

    // 绘制主框架
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);

    // 绘制复古边框装饰
    drawRetroBorder(ctx, width, height);

    // 绘制神秘符文
    drawMysticalRunes(ctx, width, height);

    // 绘制复古纹理
    drawRetroTexture(ctx, width, height);

    // 绘制发光效果
    drawProfileGlow(ctx, width, height);
  } catch (error) {
    console.error('绘制个人资料框架失败:', error);
  }
};

// 绘制退出确认框架
const drawExitConfirmFrame = (ctx, width, height) => {
  if (!ctx) return;

  try {
    // 清除画布
    ctx.clearRect(0, 0, width, height);

    // 创建复古渐变背景
    const gradient = ctx.createLinearGradient(0, 0, width, height);
    gradient.addColorStop(0, '#1a0f0f');
    gradient.addColorStop(0.3, '#2d1b1b');
    gradient.addColorStop(0.7, '#1a0f0f');
    gradient.addColorStop(1, '#0a0505');

    // 绘制主框架
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);

    // 绘制复古边框装饰
    drawRetroBorder(ctx, width, height);

    // 绘制神秘符文
    drawMysticalRunes(ctx, width, height);

    // 绘制复古纹理
    drawRetroTexture(ctx, width, height);

    // 绘制发光效果
    drawProfileGlow(ctx, width, height);
  } catch (error) {
    console.error('绘制退出确认框架失败:', error);
  }
};

// 渲染画布
const renderCanvas = () => {
  if (!ctx.value || !canvas.value) return;

  try {
    switch (props.renderType) {
      case 'inventory':
        drawInventoryFrame(ctx.value, props.width, props.height);
        break;
      case 'profile':
        drawProfileFrame(ctx.value, props.width, props.height);
        break;
      case 'exit-confirm':
        drawExitConfirmFrame(ctx.value, props.width, props.height);
        break;
      default:
        console.warn('未知的渲染类型:', props.renderType);
    }
  } catch (error) {
    console.error('Canvas 渲染失败:', error);
  }
};

// 初始化画布
const initCanvas = () => {
  try {
    canvas.value = canvas.value; // 获取模板中的canvas元素
    if (!canvas.value) {
      console.warn('Canvas 元素未找到');
      return;
    }

    ctx.value = canvas.value.getContext('2d');
    if (!ctx.value) {
      console.error('无法获取 Canvas 2D 上下文');
      return;
    }

    // 设置画布尺寸
    canvas.value.width = props.width;
    canvas.value.height = props.height;

    // 根据类型调用不同的绘制方法
    renderCanvas();

  } catch (error) {
    console.error('Canvas 初始化失败:', error);
  }
};

// 生命周期钩子
onMounted(() => {
  nextTick(() => {
    initCanvas();
  });
});

onBeforeUnmount(() => {
  cleanup();
});
</script>