<template>
  <el-aside :width="width" class="sidebar">
    <div class="logo">
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
    </div>
    <el-menu
      :default-active="activeMenu"
      class="el-menu-vertical-demo"
      :router="router"
      :background-color="backgroundColor"
      :text-color="textColor"
      :active-text-color="activeTextColor"
      @select="handleMenuSelect"
    >
      <template v-for="item in menuItems" :key="item.index">
        <el-sub-menu v-if="item.children" :index="item.index">
          <template #title>
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
            <el-icon>
              <component :is="child.icon" />
            </el-icon>
            <span>{{ child.title }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.index">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  // 基本配置
  title: { type: String, default: '智慧校园' },
  subtitle: { type: String, default: '' },
  width: { type: String, default: '220px' },
  
  // 菜单数据
  menuItems: { type: Array, required: true },
  
  // Element Plus 菜单配置
  router: { type: Boolean, default: false },
  backgroundColor: { type: String, default: '#545c64' },
  textColor: { type: String, default: '#fff' },
  activeTextColor: { type: String, default: '#ffd04b' },
  
  activeMenu: { type: String, default: '' },
});

const emit = defineEmits(['select-menu', 'update:activeMenu']);

const handleMenuSelect = (index) => {
  emit('select-menu', index);
};
</script>

<style scoped>
.sidebar {
  background-color: #2c3e50;
  color: white;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
}

.logo {
  text-align: center;
  margin-bottom: 30px;
}

.logo h2 {
  font-size: 24px;
  margin-bottom: 5px;
}

.logo p {
  font-size: 14px;
  color: #bdc3c7;
}

.el-menu-vertical-demo {
  flex-grow: 1;
  border-right: none;
  background-color: #2c3e50;
}

.el-menu-vertical-demo .el-menu-item,
.el-menu-vertical-demo .el-sub-menu__title {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.el-menu-vertical-demo .el-menu-item:hover,
.el-menu-vertical-demo .el-sub-menu__title:hover {
  background-color: #34495e !important;
}

.el-menu-vertical-demo .el-menu-item.is-active {
  background-color: #3498db !important;
  font-weight: bold;
}

.el-menu-vertical-demo .el-menu-item,
.el-menu-vertical-demo .el-sub-menu__title {
  color: white !important;
}

.el-menu-vertical-demo .el-menu-item.is-active span,
.el-menu-vertical-demo .el-sub-menu__title span {
  color: white !important;
}

.el-menu-vertical-demo .el-menu-item.is-active .el-icon,
.el-menu-vertical-demo .el-sub-menu__title .el-icon {
  color: white !important;
}
/*  cursor: pointer;
  transition: background-color 0.3s ease;
}*/

.nav-item:hover {
  background-color: #34495e;
}

.nav-item.active {
  background-color: #3498db;
  font-weight: bold;
}

.nav-item .icon {
  margin-right: 15px;
  font-size: 20px;
}

.nav-item .label {
  font-size: 16px;
}
</style>