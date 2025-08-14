<template>
  <div class="backpack" @click="handleToggle">
    <img src="/Image/Bags.png" alt="背包" />
  </div>
</template>

<script setup>
import { defineEmits } from 'vue'

const emit = defineEmits(['toggle-inventory'])

const handleToggle = () => {
  emit('toggle-inventory') 
}
</script>

<style lang="less" scoped>
.backpack {
  position: absolute;
  bottom: 20px;
  left: 20px;
  z-index: 1000;
  cursor: pointer;

  img {
    width: 100px;
    height: 100px;
    transition: transform 0.2s ease;

    &:hover {
      transform: scale(1.1);
    }
  }
}
</style>