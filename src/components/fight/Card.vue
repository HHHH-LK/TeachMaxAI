<template>
  <div class="fight-container">
    <div class="fight-footer">
      <div class="card-fight">
        <div class="game-container">
          <div
            class="container-card"
            @mousedown="startInteraction"
            ref="card"
            :style="{ cursor: interactionEnabled ? 'pointer' : 'default' }"
          >
            <div
              class="card-image"
              :style="{
                backgroundImage: `url(${imageSrc})`,
                transform: `scale(${isActive ? 1.15 : 1}) translateY(${
                  isActive ? '-15px' : '0px'
                })`,
                opacity: interactionEnabled ? 1 : 0.7,
              }"
              ref="cardImg"
            ></div>

            <!-- 箭头 -->
            <canvas ref="arrowCanvas" class="arrow-canvas"></canvas>
          </div>
        </div>
      </div>
    </div>

    <!-- 卡牌效果模态框 -->
    <CardEffectModal
      v-if="showModal"
      :difficulty="cardDifficulty"
      @close="closeModal"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from "vue";

const imageSrc = ref("/Image/GameCard.png");
const isActive = ref(false);
const activationDistance = ref(150);
const maxArrowLength = ref(500);
const arrowSegments = ref(20);
const mousePos = ref({ x: 0, y: 0 });
const arrowStart = ref({ x: 0, y: 0 });
const showCopied = ref(false);
const cardVisible = ref(true);
const card = ref(null);
const arrowCanvas = ref(null);
const isEnemyTargeted = ref(false);
const enemyPosition = ref({ x: 0, y: 0 });
const cardDifficulty = ref(null);
const showModal = ref(false);
const interactionEnabled = ref(true); //控制交互是否启用
const originalImage = "/Image/GameCard.png"; // 原始卡牌图片路径
let enemyElement = null;

// 代替 setInterval 的计时器引用
let drawInterval = null;

// 定位卡片
const positionCard = () => {
  if (!card.value) return;

  const rect = card.value.getBoundingClientRect();
  const cardWidth = rect.width;
  const cardHeight = rect.height;
  const screenWidth = window.innerWidth;
  const screenHeight = window.innerHeight;

  // 计算位置：底部中央偏左5%，距离底部80px
  const left = (screenWidth - cardWidth) / 2;
  const top = screenHeight - cardHeight - 20;

  // 应用定位
  card.value.style.position = "fixed";
  card.value.style.left = `${left}px`;
  card.value.style.top = `${top}px`;
};

// 调整画布大小
const resizeCanvas = () => {
  if (arrowCanvas.value) {
    arrowCanvas.value.width = window.innerWidth;
    arrowCanvas.value.height = window.innerHeight;
  }
};

// 开始交互
const startInteraction = () => {
  if (!interactionEnabled.value) return; // 检查交互是否启用

  isActive.value = true;
  resizeCanvas();

  // 获取卡片中心位置
  if (!card.value) return;
  const rect = card.value.getBoundingClientRect();

  arrowStart.value = {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  };

  mousePos.value = { ...arrowStart.value };

  // 开始绘制箭头
  stopDrawing();
  drawInterval = requestAnimationFrame(drawLoop);
};

// 全局鼠标移动处理
const handleGlobalMouseMove = (event) => {
  if (!interactionEnabled.value) return; // 检查交互是否启用

  mousePos.value = { x: event.clientX, y: event.clientY };

  // 如果箭头处于激活状态，检查是否接近敌人
  if (isActive.value) {
    checkEnemyTarget();
  }
};

// 检查是否接近敌人
const checkEnemyTarget = () => {
  if (!enemyElement) {
    // 尝试获取敌人元素
    enemyElement = document.querySelector(".enemy-character");
    if (!enemyElement) return;
  }

  // 获取敌人位置
  const rect = enemyElement.getBoundingClientRect();
  const enemyCenterX = rect.left + rect.width / 2;
  const enemyCenterY = rect.top + rect.height / 2;

  // 计算鼠标到敌人中心的距离
  const dx = mousePos.value.x - enemyCenterX;
  const dy = mousePos.value.y - enemyCenterY;
  const distance = Math.sqrt(dx * dx + dy * dy);

  // 如果距离小于100px，视为接近敌人
  const newStatus = distance < 100;

  if (isEnemyTargeted.value !== newStatus) {
    isEnemyTargeted.value = newStatus;

    document.dispatchEvent(
      new CustomEvent("global-bracket-visibility", { detail: newStatus })
    );
  }

  // 记录敌人位置用于箭头判断
  enemyPosition.value = { x: enemyCenterX, y: enemyCenterY };
};

// 全局鼠标释放处理
const handleGlobalMouseUp = async () => {
  if (!interactionEnabled.value) return;

  if (isActive.value) {
    // 检查是否击中敌人
    if (isEnemyTargeted.value) {
      // 模拟从后端获取卡牌难度数据
      try {
        const difficulty = await fetchCardDifficulty();
        cardDifficulty.value = difficulty;

        // 根据难度更新卡牌图片
        imageSrc.value = `/Image/GameCard_${difficulty}.png`;

        // 显示模态框
        showModal.value = true;

        // 禁用卡片交互
        disableCardInteraction();
      } catch (error) {
        console.error("获取卡牌难度失败:", error);
        // 回到初始状态
        stopInteraction();
      }
    } else {
      const dx = mousePos.value.x - arrowStart.value.x;
      const dy = mousePos.value.y - arrowStart.value.y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance > activationDistance.value) {
        showCopied.value = true;
        setTimeout(() => {
          showCopied.value = false;
        }, 2000);
      }

      stopInteraction();
    }
  }
};

// 禁用卡片交互
const disableCardInteraction = () => {
  interactionEnabled.value = false;

  // 移除事件监听器
  document.removeEventListener("mousemove", handleGlobalMouseMove);
  document.removeEventListener("mouseup", handleGlobalMouseUp);

  // 停止当前交互
  stopInteraction();
};

// 启用卡片交互
const enableCardInteraction = () => {
  interactionEnabled.value = true;

  // 重新添加事件监听器
  document.addEventListener("mousemove", handleGlobalMouseMove);
  document.addEventListener("mouseup", handleGlobalMouseUp);
};

// 模拟从后端获取卡牌难度数据
const fetchCardDifficulty = async () => {
  return new Promise((resolve) => {
    setTimeout(() => {
      // 随机选择一种难度
      const difficulties = ["Easy", "Normal", "Hard"];
      const difficulty =
        difficulties[Math.floor(Math.random() * difficulties.length)];
      resolve(difficulty);
    }, 500);
  });
};

// 关闭模态框
const closeModal = () => {
  showModal.value = false;
  resetCardImage();

  enableCardInteraction();
};

// 重置卡牌图片
const resetCardImage = () => {
  imageSrc.value = originalImage;
  cardDifficulty.value = null;
  stopInteraction();
};

// 停止交互
const stopInteraction = () => {
  isActive.value = false;
  stopDrawing();
  clearCanvas();
  // 派发事件隐藏括号
  document.dispatchEvent(
    new CustomEvent("global-bracket-visibility", { detail: false })
  );
  // 重置锁定状态
  isEnemyTargeted.value = false;
};

// 清除画布
const clearCanvas = () => {
  if (arrowCanvas.value) {
    const ctx = arrowCanvas.value.getContext("2d");
    ctx.clearRect(0, 0, arrowCanvas.value.width, arrowCanvas.value.height);
  }
};

// 停止动画循环
const stopDrawing = () => {
  if (drawInterval) {
    cancelAnimationFrame(drawInterval);
    drawInterval = null;
  }
};

// 绘制循环
const drawLoop = () => {
  drawArrow();
  if (isActive.value) {
    drawInterval = requestAnimationFrame(drawLoop);
  }
};

// 绘制箭头
const drawArrow = () => {
  if (!arrowCanvas.value || !isActive.value) return;

  const ctx = arrowCanvas.value.getContext("2d");
  clearCanvas();

  const dx = mousePos.value.x - arrowStart.value.x;
  const dy = mousePos.value.y - arrowStart.value.y;
  const distance = Math.sqrt(dx * dx + dy * dy);

  // 计算弧线控制点
  const curveHeight = 100;
  const angle = Math.atan2(dy, dx);
  const length = Math.min(distance, maxArrowLength.value);

  // 计算控制点
  const controlPoint = {
    x:
      arrowStart.value.x +
      Math.cos(angle) * length * 0.5 +
      Math.cos(angle - Math.PI / 2) * curveHeight,
    y:
      arrowStart.value.y +
      Math.sin(angle) * length * 0.5 +
      Math.sin(angle - Math.PI / 2) * curveHeight,
  };

  // 计算弧线路径
  const points = [];
  for (let i = 0; i <= arrowSegments.value; i++) {
    const t = i / arrowSegments.value;
    const x =
      Math.pow(1 - t, 2) * arrowStart.value.x +
      2 * (1 - t) * t * controlPoint.x +
      Math.pow(t, 2) * mousePos.value.x;
    const y =
      Math.pow(1 - t, 2) * arrowStart.value.y +
      2 * (1 - t) * t * controlPoint.y +
      Math.pow(t, 2) * mousePos.value.y;
    points.push({ x, y });
  }

  // 计算目标状态
  const enemyDist = Math.sqrt(
    Math.pow(mousePos.value.x - enemyPosition.value.x, 2) +
      Math.pow(mousePos.value.y - enemyPosition.value.y, 2)
  );
  const isTargeting = isEnemyTargeted.value || distance > 300;

  // 箭头尺寸定义
  const arrowSize = 20;

  // 绘制小三角形组成的箭身
  for (let i = 0; i < points.length - 1; i++) {
    const startPoint = points[i];
    const endPoint = points[i + 1];
    const segmentAngle = Math.atan2(
      endPoint.y - startPoint.y,
      endPoint.x - startPoint.x
    );

    // 绘制三角形
    const size = 10;
    const offset = 8;
    ctx.beginPath();
    ctx.moveTo(
      startPoint.x + Math.cos(segmentAngle) * offset,
      startPoint.y + Math.sin(segmentAngle) * offset
    );
    ctx.lineTo(
      startPoint.x + Math.cos(segmentAngle + Math.PI / 2) * size,
      startPoint.y + Math.sin(segmentAngle + Math.PI / 2) * size
    );
    ctx.lineTo(
      startPoint.x + Math.cos(segmentAngle - Math.PI / 2) * size,
      startPoint.y + Math.sin(segmentAngle - Math.PI / 2) * size
    );
    ctx.closePath();

    // 颜色计算
    const progress = i / (points.length - 1);
    let red, green, blue;
    if (isTargeting) {
      red = 255 - Math.floor(progress * 100);
      green = 50;
      blue = 50;
    } else {
      red = Math.floor(150 - progress * 50);
      green = Math.floor(150 - progress * 50);
      blue = Math.floor(150 - progress * 50);
    }

    ctx.fillStyle = `rgb(${red}, ${green}, ${blue})`;
    ctx.fill();
  }

  // 绘制箭头头部
  if (points.length > 1) {
    const lastPoint = points[points.length - 1];
    const secondLastPoint = points[points.length - 2];

    // 使用最近两点计算切线角度
    const tangentAngle = Math.atan2(
      lastPoint.y - secondLastPoint.y,
      lastPoint.x - secondLastPoint.x
    );

    // 绘制箭头
    ctx.beginPath();
    ctx.moveTo(mousePos.value.x, mousePos.value.y);
    ctx.lineTo(
      mousePos.value.x - Math.cos(tangentAngle - Math.PI / 6) * arrowSize,
      mousePos.value.y - Math.sin(tangentAngle - Math.PI / 6) * arrowSize
    );
    ctx.lineTo(
      mousePos.value.x - Math.cos(tangentAngle + Math.PI / 6) * arrowSize,
      mousePos.value.y - Math.sin(tangentAngle + Math.PI / 6) * arrowSize
    );
    ctx.closePath();

    // 设置渐变颜色
    const gradient = ctx.createLinearGradient(
      mousePos.value.x - arrowSize,
      mousePos.value.y - arrowSize,
      mousePos.value.x + arrowSize,
      mousePos.value.y + arrowSize
    );

    // 根据目标状态设置颜色
    if (isTargeting) {
      gradient.addColorStop(0, "#ff0033");
      gradient.addColorStop(1, "#ff6600");
    } else {
      gradient.addColorStop(0, "#666666");
      gradient.addColorStop(1, "#999999");
    }

    ctx.fillStyle = gradient;
    ctx.fill();
  }
};


onMounted(() => {
  positionCard();
  window.addEventListener("resize", positionCard);
  window.addEventListener("resize", resizeCanvas);
  document.addEventListener("mousemove", handleGlobalMouseMove);
  document.addEventListener("mouseup", handleGlobalMouseUp);

  // 初始调整画布
  nextTick(() => {
    resizeCanvas();
  });
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", positionCard);
  window.removeEventListener("resize", resizeCanvas);
  stopDrawing();
  document.removeEventListener("mousemove", handleGlobalMouseMove);
  document.removeEventListener("mouseup", handleGlobalMouseUp);
});
</script>

<style scoped>

.game-container {
  position: relative;
  z-index: 200;
  pointer-events: auto;
}


.container-card {
  position: fixed;
  width: 190px;
  height: 280px;
  border-radius: 18px;
  overflow: visible;
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275),
    box-shadow 0.4s ease;
  box-shadow: 0 0 20px rgba(255, 204, 0, 0.5);

  pointer-events: auto;
  z-index: 10001 !important;
}


.card-image {
  position: absolute;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transform-origin: center bottom;
  transition: transform 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275),
    box-shadow 0.5s ease;
  border-radius: 18px;
  box-shadow: 0 0 0 2px rgba(255, 204, 0, 0.3), 0 15px 35px rgba(0, 0, 0, 0.7),
    0 5px 15px rgba(0, 0, 0, 0.5);
  z-index: 2;
}


.container-card:active .card-image {
  box-shadow: 0 0 0 4px rgba(255, 204, 0, 0.7), 0 25px 50px rgba(0, 0, 0, 0.9),
    0 12px 25px rgba(0, 0, 0, 0.8);
}

/* 箭头画布 */
.arrow-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  pointer-events: none;
  z-index: 150; 
}

.active-mode {
  z-index: 10001 !important;
}

@keyframes fadeInOut {
  0% {
    opacity: 0;
    transform: translate(-50%, -30%);
  }
  20% {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
  80% {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -70%);
  }
}
</style>
