<template>
  <div class="profile-center" @click="handleOpen">
    <img src="../assets/个人中心.png" alt="个人中心" />
  </div>
</template>

<script>
export default {
  name: 'ProfileCenter',
  emits: ['open-profile'],
  methods: {
    handleOpen() {
      this.$emit('open-profile');
    }
  }
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
