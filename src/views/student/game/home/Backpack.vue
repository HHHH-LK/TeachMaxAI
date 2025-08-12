<template>
  <div class="backpack" @click="handleToggle">
    <img src="../assets/背包（无背景）.png" alt="背包" />
  </div>
</template>

<script setup>
// 声明组件发出的事件
const emit = defineEmits(['toggle-inventory']);

// 处理点击事件，触发toggle-inventory事件
const handleToggle = () => {
  emit('toggle-inventory');
};

// 定义组件名称（可选，用于调试等）
defineOptions({
  name: 'Backpack'
});
</script>

<style lang="less" scoped>
.backpack {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
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
