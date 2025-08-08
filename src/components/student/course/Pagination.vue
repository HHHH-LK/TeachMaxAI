<template>
  <div class="pagination-container">
    <div class="counter">{{ current }}/{{ total }}</div>
    <button
      class="paginate left"
      :data-state="current === 1 ? 'disabled' : ''"
      @click="prev"
    >
      <i></i><i></i>
    </button>
    <button
      class="paginate right"
      :data-state="current === total ? 'disabled' : ''"
      @click="next"
    >
      <i></i><i></i>
    </button>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  total: {
    type: Number,
    default: 5,
  },
  initialPage: {
    type: Number,
    default: 1,
  },
});

const emit = defineEmits(["page-change"]);

const current = ref(props.initialPage);

// 监听页码变化
watch(current, (newPage) => {
  emit("page-change", newPage);
});

const prev = () => {
  if (current.value > 1) {
    current.value--;
  }
};

const next = () => {
  if (current.value < props.total) {
    current.value++;
  }
};

// 暴露当前页码以允许父组件访问
defineExpose({
  current,
});
</script>

<style lang="scss" scoped>
@use "sass:math"; 

$size: 20px;
$thickness: 3px;
$angle: 42deg;
$angleHover: 32deg;
$angleActive: 28deg;
$primary-color: #006ea1;
$primary-light: #006ea1;
$primary-dark: #b8e8ff;
$shadow-color: rgba(0, 0, 0, 0.3);

@mixin arrowTransform($angle, $x: 0, $y: 0) {
  i:first-child {
    transform: translate($x, $y) rotate($angle);
  }

  i:last-child {
    transform: translate($x, -$y) rotate(-$angle);
  }
}

body{
  caret-color: transparent;
}


.pagination-container {
  position: relative;
  width: 100%;
  height: auto;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: auto;
}

button {
  -webkit-appearance: none;
  background: transparent;
  
  caret-color: transparent;
  border: 0;
  outline: 0;
  padding: 0;
}

.paginate {
  position: relative;
  width: $size;
  height: $size;
  cursor: pointer;
  transform: translate3d(0, 0, 0); // 修复 WebKit 中的闪烁问题
  transition: all 0.3s ease;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);

  position: absolute;
  top: 50%;
  margin-top: calc(-#{$size} / 2);
  filter: drop-shadow(0 3px 5px $shadow-color);

  &:hover:not([data-state="disabled"]) {
    transform: translateY(-3px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  }

  &:active:not([data-state="disabled"]) {
    transform: translateY(1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  i {
    position: absolute;
    top: 40%;
    left: 0;
    width: $size;
    height: $thickness;
    border-radius: calc(#{$thickness} / 2);;
    background: $primary-color;
    transition: all 0.25s ease;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  }

  &.left {
    right: 58%;

    i {
      transform-origin: 0% 50%;
      background: linear-gradient(to right, $primary-light, $primary-dark);
    }

    @include arrowTransform($angle, 0, -1px);

    &:hover:not([data-state="disabled"]) {
      @include arrowTransform($angleHover, 0, -1px);
      i {
        background: $primary-dark;
      }
    }

    &:active:not([data-state="disabled"]) {
      @include arrowTransform($angleActive, 1px, -1px);
    }

    &[data-state="disabled"] {
      @include arrowTransform(0deg, -5px, 0);
      opacity: 0.3;
      cursor: default;
      transform: none;
      box-shadow: none;

      &:hover {
        @include arrowTransform(0deg, -5px, 0);
        transform: none;
      }

      i {
        background: #ccc;
      }
    }
  }

  &.right {
    left: 58%;

    i {
      transform-origin: 100% 50%;
      background: linear-gradient(to left, $primary-light, $primary-dark);
    }

    @include arrowTransform($angle, 0, 1px);

    &:hover:not([data-state="disabled"]) {
      @include arrowTransform($angleHover, 0, 1px);
      i {
        background: $primary-dark;
      }
    }

    &:active:not([data-state="disabled"]) {
      @include arrowTransform($angleActive, 1px, 1px);
    }

    &[data-state="disabled"] {
      @include arrowTransform(0deg, 5px, 0);
      opacity: 0.3;
      cursor: default;
      transform: none;
      box-shadow: none;

      &:hover {
        @include arrowTransform(0deg, 5px, 0);
        transform: none;
      }

      i {
        background: #ccc;
      }
    }
  }
}

.counter {
  text-align: center;
  position: relative;
  width: 100%;
  top: 0;
  margin-top: 0;
  font-size: 32px;
  font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
  text-shadow: 0 3px 6px rgba(0, 0, 0, 0.15);
  color: $primary-dark;
  font-weight: 700;
  letter-spacing: 1px;
  pointer-events: none;
  user-select: none;
  z-index: 5;
  background: linear-gradient(to right, $primary-light, $primary-dark);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  padding: 10px 0;
}

/* 响应式设计 */
@media (max-width: 700px) {
  .pagination-container {
    height: 120px;
    flex-direction: column;
  }

  .paginate {
    position: relative;
    top: auto;
    margin-top: 0;
    margin-bottom: 20px;

    &.left {
      right: auto;
      margin-right: 20px;
    }

    &.right {
      left: auto;
      margin-left: 20px;
    }
  }

  .counter {
    order: -1;
    margin-bottom: 15px;
  }
}
</style>
