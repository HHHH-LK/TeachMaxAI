<template>
  
  <div id="loginForm">
    <login-form-part />
  </div>
  <div id="charactersDiv"></div>
  <canvas id="canvas"></canvas>
  
	<rain />

</template>

<script setup>
import { onMounted, onUnmounted } from "vue";
import LoginFormPart from "./LoginFormPart.vue";
import Rain from "@/components/common/Rain.vue";

// 圆圈类
class Circulo {
  constructor(x, y, size) {
    this.x = x;
    this.y = y;
    this.size = size;
  }
}

// 全局变量
let circulos = [];
let timer = 0;
let requestID = null;
let canvas = null;
let context = null;

// 角色动画配置
const characterConfigs = [
  {
    image: "stick0.svg",
    top: "0%",
    rotation: "rotateZ(-90deg)",
    movement: { duration: 1500 },
    rotationAnim: null,
  },
  {
    image: "stick1.svg",
    top: "10%",
    rotation: "",
    movement: { duration: 3000 },
    rotationAnim: { duration: 2000 },
  },
  {
    image: "stick2.svg",
    top: "20%",
    rotation: "",
    movement: { duration: 5000 },
    rotationAnim: { duration: 1000 },
  },
  {
    image: "stick0.svg",
    top: "25%",
    rotation: "",
    movement: { duration: 2500 },
    rotationAnim: { duration: 1500 },
  },
  {
    image: "stick0.svg",
    top: "35%",
    rotation: "",
    movement: { duration: 2000 },
    rotationAnim: { duration: 300 },
  },
  {
    image: "stick3.svg",
    bottom: "5%",
    rotation: "",
    movement: null,
    rotationAnim: null,
  },
];

// 初始化圆圈
function initArr() {
  circulos = [];

  for (let index = 0; index < 300; index++) {
    const randomX =
      Math.floor(Math.random() * (canvas.width * 3 - canvas.width * 1.2 + 1)) +
      canvas.width * 1.2;
    const randomY = Math.floor(
      Math.random() * (canvas.height - (canvas.height * -0.2 + 1)) +
        canvas.height * -0.2
    );
    const size = canvas.width / 1000;

    circulos.push(new Circulo(randomX, randomY, size));
  }
}

// 响应窗口变化
function handleResize() {
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;

  timer = 0;
  if (requestID) {
    cancelAnimationFrame(requestID);
    requestID = null;
  }
  context.reset();
  initArr();
  draw();

  const charactersDiv = document.getElementById("charactersDiv");
  if (charactersDiv) {
    charactersDiv.innerHTML = "";
    charactersAnimate();
  }
}

// 动画循环
function draw() {
  timer++;
  context.setTransform(1, 0, 0, 1, 0, 0);

  const distanceX = canvas.width / 80;
  const growthRate = canvas.width / 1000;

  context.fillStyle = "white";
  context.clearRect(0, 0, canvas.width, canvas.height);

  circulos.forEach((circulo) => {
    context.beginPath();

    // 快速移动阶段
    if (timer < 65) {
      circulo.x -= distanceX;
      circulo.size += growthRate;
    }
    // 慢速移动阶段
    else if (timer > 65 && timer < 500) {
      circulo.x -= distanceX * 0.02;
      circulo.size += growthRate * 0.2;
    }

    context.arc(circulo.x, circulo.y, circulo.size, 0, Math.PI * 2);
    context.fill();
  });

  requestID = requestAnimationFrame(draw);

  // 停止动画
  if (timer > 500) {
    cancelAnimationFrame(requestID);
    requestID = null;
  }
}

// 角色动画
function charactersAnimate() {
  const baseUrl =
    "https://raw.githubusercontent.com/RicardoYare/imagenes/9ef29f5bbe075b1d1230a996d87bca313b9b6a63/sticks/";

  for (let index = 0; index < characterConfigs.length; index++) {
    const config = characterConfigs[index];
    const stick = document.createElement("img");
    stick.classList.add("character");

    // 应用基本样式
    if (config.top) stick.style.top = config.top;
    if (config.bottom) stick.style.bottom = config.bottom;
    if (config.rotation) stick.style.transform = config.rotation;

    // 设置角色图片
    stick.src = baseUrl + config.image;

    // 添加到页面
    document.getElementById("charactersDiv").appendChild(stick);

    // 特殊角色不添加移动动画
    if (index === 5) continue;

    // 添加移动动画
    stick.animate([{ left: "100%" }, { left: "-20%" }], {
      duration: config.movement.duration,
      easing: "linear",
      fill: "forwards",
    });

    // 添加旋转动画（如果有）
    if (config.rotationAnim) {
      stick.animate(
        [
          { transform: `${config.rotation || ""} rotate(0deg)` },
          { transform: `${config.rotation || ""} rotate(-360deg)` },
        ],
        {
          duration: config.rotationAnim.duration,
          iterations: Infinity,
          easing: "linear",
        }
      );
    }
  }
}

// 组件挂载
onMounted(() => {
  canvas = document.getElementById("canvas");
  context = canvas.getContext("2d");

  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;

  initArr();
  draw();
  charactersAnimate();

  window.addEventListener("resize", handleResize);
});

// 组件卸载
onUnmounted(() => {
  if (requestID) {
    cancelAnimationFrame(requestID);
    requestID = null;
  }
  window.removeEventListener("resize", handleResize);
});
</script>

<style>
.container{
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
}

body {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: black;
  margin: 0;
  padding: 0;
}

#charactersDiv {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 10;
}

.character {
  position: absolute;
  width: 18%;
  height: auto;
  max-height: 18%;
}

#canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  /* z-index: 1; */
}

#loginForm {
  position: absolute;
  /* background-color: red; */
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  flex-direction: column;
  z-index: 1000;
  width: 80%;
  height: 80%;
}
</style>

