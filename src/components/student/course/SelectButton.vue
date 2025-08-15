<template>
  <div class="back" @click="toggleSelect">
    <div class="button_base b05_3d_roll">
      <div>{{ isSelected ? '退选' : '选择' }}</div>
      <div>课程</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  ifSelect: {
    type: String,
    default: "选择"
  },
  disabled: Boolean,
  isSelected: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["click"]); 

function toggleSelect(e) {
  e.stopPropagation();
  if (!props.disabled) {
    emit("click"); // 只触发点击
  }
}
</script>

<style scoped>
.button_base {
  margin: 0;
  border: 0;
  font-size: 15px;
  position: relative;
  top: 50%;
  left: 50%;
  margin-top: -25px;
  margin-left: -100px;
  width: 200px;
  height: 60px;
  text-align: center;
  box-sizing: border-box;
  -webkit-box-sizing: border-box;
  -moz-box-sizing: border-box;
  -webkit-user-select: none;
  cursor: default;
}

.button_base:hover {
  cursor: pointer;
}

.b05_3d_roll {
  perspective: 500px;
  -webkit-perspective: 500px;
  -moz-perspective: 500px;
}

.b05_3d_roll div {
  border-radius: 1rem;
  position: absolute;
  text-align: center;
  width: 100%;
  height: 50px;
  padding: 10px;
  border: #000000 solid 1px;
  pointer-events: none;
  box-sizing: border-box;
  -webkit-box-sizing: border-box;
  -moz-box-sizing: border-box;
}

.b05_3d_roll div:nth-child(1) {
  border-radius: 1rem;
  color: #b0b0b0;
  background-color: #000000;
  transform: rotateX(90deg);
  -webkit-transform: rotateX(90deg);
  -moz-transform: rotateX(90deg);
  transition: all 0.2s ease;
  -webkit-transition: all 0.2s ease;
  -moz-transition: all 0.2s ease;
  transform-origin: 50% 50% -25px;
  -webkit-transform-origin: 50% 50% -25px;
  -moz-transform-origin: 50% 50% -25px;
}

.b05_3d_roll div:nth-child(2) {
  border-radius: 1rem;
  color: #000000;
  background-color: #ffffff;
  transform: rotateX(0deg);
  -webkit-transform: rotateX(0deg);
  -moz-transform: rotateX(0deg);
  transition: all 0.2s ease;
  -webkit-transition: all 0.2s ease;
  -moz-transition: all 0.2s ease;
  transform-origin: 50% 50% -25px;
  -webkit-transform-origin: 50% 50% -25px;
  -moz-transform-origin: 50% 50% -25px;
}

.b05_3d_roll:hover div:nth-child(1) {
  color: #ffffff;
  transition: all 0.2s ease;
  -webkit-transition: all 0.2s ease;
  -moz-transition: all 0.2s ease;
  transform: rotateX(0deg);
  -webkit-transform: rotateX(0deg);
  -moz-transform: rotateX(0deg);
}

.b05_3d_roll:hover div:nth-child(2) {
  background-color: #000000;
  transition: all 0.2s ease;
  -webkit-transition: all 0.2s ease;
  -moz-transition: all 0.2s ease;
  transform: rotateX(-90deg);
  -webkit-transform: rotateX(-90deg);
  -moz-transform: rotateX(-90deg);
}

@media screen and (max-width: 1260px) {
  .back {
    width: 50%;
  }
}

@media screen and (max-width: 840px) {
  .back {
    width: 100%;
  }
}
</style>
