<template>
  <div class="app-container">
    <!-- 侧边栏组件 -->
    <Sidebar
        :items="menuItems"
        @item-click="handleNavigation"
    />

    <!-- 主内容区域 -->
    <main class="main-content">
      <div class="content-wrapper">
        <h1>{{ currentPage.title }}</h1>
        <p>{{ currentPage.description }}</p>

        <!-- 这里可以放置路由组件或其他内容 -->
        <div class="demo-content">
          <h3>当前选中的页面: {{ currentPage.name }}</h3>
          <p>点击侧边栏的任意菜单项来切换页面内容。</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import Sidebar from '@/components/common/Sider.vue' // 引入侧边栏组件

// 自定义菜单项（可选，如果不传则使用默认菜单）
const menuItems = ref([
  { id: 'search', label: 'Search', icon: 'search-outline' },
  { id: 'home', label: 'Home', icon: 'home-outline' },
  { id: 'projects', label: 'Projects', icon: 'folder-open-outline' },
  { id: 'dashboard', label: 'Dashboard', icon: 'pie-chart-outline' },
  { id: 'team', label: 'Team', icon: 'people-outline' },
  { id: 'analytics', label: 'Analytics', icon: 'analytics-outline' }, // 自定义菜单项
  { id: 'support', label: 'Support', icon: 'chatbubbles-outline' },
  { id: 'settings', label: 'Settings', icon: 'settings-outline' }
])

// 当前页面状态
const currentPage = reactive({
  name: 'Home',
  title: 'Welcome to Home Page',
  description: 'This is the home page content. The sidebar component is fully functional and maintains the original design.'
})

// 页面配置
const pageConfig = {
  search: {
    name: 'Search',
    title: 'Search Page',
    description: 'Find what you\'re looking for with our powerful search functionality.'
  },
  home: {
    name: 'Home',
    title: 'Welcome to Home Page',
    description: 'This is the home page content. The sidebar component is fully functional and maintains the original design.'
  },
  projects: {
    name: 'Projects',
    title: 'Projects Dashboard',
    description: 'Manage and monitor all your projects in one place.'
  },
  dashboard: {
    name: 'Dashboard',
    title: 'Analytics Dashboard',
    description: 'View key metrics and performance indicators for your business.'
  },
  team: {
    name: 'Team',
    title: 'Team Management',
    description: 'Manage team members, roles, and permissions.'
  },
  analytics: {
    name: 'Analytics',
    title: 'Advanced Analytics',
    description: 'Deep dive into your data with advanced analytics tools.'
  },
  support: {
    name: 'Support',
    title: 'Customer Support',
    description: 'Get help and support for your questions and issues.'
  },
  settings: {
    name: 'Settings',
    title: 'Application Settings',
    description: 'Configure your application preferences and settings.'
  }
}

// 处理导航点击
const handleNavigation = (item) => {
  console.log('Navigation clicked:', item)

  // 更新当前页面
  const config = pageConfig[item.id]
  if (config) {
    Object.assign(currentPage, config)
  }

  // 这里可以添加路由跳转逻辑
  // 例如: router.push(`/${item.id}`)
}
</script>

<style scoped>
/* 应用容器 */
.app-container {
  display: flex;
  min-height: 100vh;
  font-family: "Poppins", sans-serif;
}

/* 主内容区域 */
.main-content {
  margin-left: 5em; /* 为侧边栏留出空间 */
  flex: 1;
  background-color: hsl(266, 16%, 92%);
  transition: margin-left 0.35s cubic-bezier(0.175, 0.685, 0.32, 1);
}

.content-wrapper {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.content-wrapper h1 {
  color: hsl(0, 0%, 0%);
  font-size: 3.6rem;
  font-weight: 900;
  margin-bottom: 1rem;
}

.content-wrapper p {
  color: hsl(0, 0%, 0%);
  font-size: 1.11rem;
  line-height: 1.6;
  margin-bottom: 2rem;
}

.demo-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.demo-content h3 {
  color: hsl(237, 94%, 81%);
  margin-bottom: 1rem;
}

/* 响应式设计 */
@media only screen and (max-width: 756px) {
  .main-content {
    margin-left: 5em;
  }

  .content-wrapper {
    padding: 1rem;
  }

  .content-wrapper h1 {
    font-size: 2.6rem;
  }
}

@media only screen and (max-width: 576px) {
  .content-wrapper {
    padding: 1rem;
  }
}
</style>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Poppins", sans-serif;
  -webkit-font-smoothing: antialiased;
  scroll-behavior: smooth;
}

/* 滚动条样式 */
::-webkit-scrollbar-track {
  background-color: hsl(266, 16%, 92%);
}

::-webkit-scrollbar {
  width: 8px;
  background-color: hsl(266, 16%, 92%);
}

::-webkit-scrollbar-thumb {
  background-color: hsl(237, 94%, 81%);
}

/* 选择样式 */
::selection {
  color: hsl(0, 0%, 100%);
  background: hsla(237, 94%, 81%, 0.33);
}
</style>