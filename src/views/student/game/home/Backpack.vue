<template>
  <div class="backpack" @click="handleToggle">
    <img src="../assets/背包（无背景）.png" alt="背包" />
  </div>
</template>

<script>
export default {
  name: 'Backpack',
  emits: ['toggle-inventory'],
  methods: {
    handleToggle() {
      this.$emit('toggle-inventory');
    }
  }
}
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
