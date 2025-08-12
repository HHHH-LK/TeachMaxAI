<template>
  <div class="profile-center" @click="handleOpen">
    <img src="../assets/个人中心.png" alt="个人中心" />
  </div>
</template>

<script setup>
// 定义要触发的事件
const emit = defineEmits(['open-profile'])

// 处理点击事件
const handleOpen = () => {
  emit('open-profile')
}
</script>

<style lang="less" scoped>
.profile-center {
  position: absolute;
  top: 20px;
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
