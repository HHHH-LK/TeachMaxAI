<template>
  <div class="container">
    <div class="tabs">
      <input 
        type="radio" 
        id="radio-1" 
        name="tabs" 
        :checked="activeTab === 'community'"
        @click="setActiveTab('community')"
      />
      <label class="tab" for="radio-1">社区</label>
      
      <input 
        type="radio" 
        id="radio-2" 
        name="tabs"
        :checked="activeTab === 'my-posts'"
        @click="setActiveTab('my-posts')"
      />
      <label class="tab" for="radio-2">我的帖子</label>
      
      <span class="glider"></span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
const activeTab = ref();

const setActiveTab = (tab) => {
  activeTab.value = tab;
};

// 外部传入默认 tab
const props = defineProps({
  initialTab: {
    type: String,
    default: 'community'
  }
});

// 监听默认 tab 初始化赋值
watch(() => props.initialTab, (newVal) => {
  if (['community', 'my-posts'].includes(newVal)) {
    activeTab.value = newVal;
  }
}, { immediate: true });

// 暴露当前 tab 给父组件
defineExpose({ activeTab });
</script>
<style>
@import url("https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap");

:root {
  --primary-color: #185ee0;
  --secondary-color: #e6eef9;
}

/**,
*:after,
*:before {
  box-sizing: border-box;
}*/

/*body {
  font-family: "Inter", sans-serif;
  background-color: rgba(230, 238, 249, 0.5);
}*/

.container{
    position: relative;
}

/* .container {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
} */

.tabs {
  display: flex;
  position: relative;
  background-color: #fff;
  box-shadow: 0 0 1px 0 rgba(24, 94, 224, 0.15), 0 6px 12px 0 rgba(29, 94, 224, 0.15);
  padding: 0.75rem;
  border-radius: 99px; /* just a high number to create pill effect */
  /* z-index: 2; */
}

input[type="radio"] {
  display: none;
}

.tab {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 54px;
  width: 200px;
  font-size: 1.25rem;
  font-weight: 500;
  border-radius: 99px; 
  cursor: pointer;
  transition: color 0.15s ease-in;
  z-index: 5;
}

.notification {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  margin-left: 0.75rem;
  border-radius: 50%;
  background-color: var(--secondary-color);
  transition: 0.15s ease-in;
}

input[type="radio"]:checked + label {
  color: var(--primary-color);
}

input[type="radio"]:checked + label > .notification {
  background-color: var(--primary-color);
  color: #fff;
}

input[id="radio-1"]:checked ~ .glider {
  transform: translateX(0);
}

input[id="radio-2"]:checked ~ .glider {
  transform: translateX(100%);
}

input[id="radio-3"]:checked ~ .glider {
  transform: translateX(200%);
}

.glider {
  position: absolute;
  display: flex;
  height: 54px;
  width: 200px;
  background-color: var(--secondary-color);
  z-index: 1;
  border-radius: 99px;
  transition: 0.25s ease-out;
}

@media (max-width: 200px) {
  .tabs {
    transform: scale(0.6);
  }
}
</style>
