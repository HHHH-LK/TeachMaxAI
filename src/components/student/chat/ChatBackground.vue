<template>
  <div class="scene-container">
    <section class="animation-section">
      <div
        v-for="(item, index) in items"
        :key="index"
        class="animated-block"
        :style="blockStyles[index]"
      ></div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
    const items = ref(Array(20).fill(null));
    const blockStyles = ref([]);

    // 初始化方块样式
    onMounted(() => {
      const styles = [];
      for (let i = 0; i < items.value.length; i++) {
        styles.push({
          "--i": i,
          "--d": Math.random() * 8,
          "--a": Math.random() * 8 + 4,
          "--hue": Math.floor(Math.random() * 360),
          "--y": Math.floor(Math.random() * 100)
        });
      }
      blockStyles.value = styles;
    });

    // return { items, blockStyles };

</script>

<style scoped>

.scene-container {
  min-height: 100vh;
  display: grid;
  place-items: center;
  transform-style: preserve-3d;
  perspective: 80vmin;
  background-color: white;
  opacity: 0.3;
}

.animation-section {
  width: 100vmin;
  aspect-ratio: 4 / 3;
  transform-origin: 100% 50%;
  rotate: y 40deg;
  mask: linear-gradient(90deg, #0000 0 40px, #fff, #0000 calc(100% - 40px) 100%);
  position: relative;
}

.animated-block {
  width: 40px;
  aspect-ratio: 1;
  position: absolute;
  top: calc(var(--y) * 1%);
  background: hsl(var(--hue), 90%, 60%);
  animation-name: travel;
  animation-iteration-count: infinite;
  animation-delay: calc(var(--d) * -1s);
  animation-duration: calc(var(--a) * 1s);
  animation-timing-function: linear;
  animation-play-state: running;
}

@keyframes travel {
  0% {
    transform: translateX(100cqi);
  }
  100% {
    transform: translateX(-50%);
  }
}
</style>