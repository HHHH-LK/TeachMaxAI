<template>
  <div class="common-layout">
    <el-container>
      <Sider
          :items="navbarItems"
          @item-click="handleNavbarItemClick"
      />

      <el-container class="right-container">
        <head-bar/>
        <el-main class="main-content">
          <!-- 根据 activeMenu 显示不同的内容 -->
          <router-view></router-view>
          <aiAssistant v-if="!isGamePage" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import studentAvatar from '@/assets/student-avatar.png';
// import Sidebar from '../components/common/Sidebar.vue';
import CommonHeader from '../components/common/CommonHeader.vue';
import aiAssistant from '../components/aIAssistant.vue';
import Sider from '@/components/common/Sider.vue'
import CommunityStudent from './student/CommunityStudent.vue';
import {useRoute, useRouter} from 'vue-router';
import HeadBar from '@/components/common/HeadBar.vue';

const router = useRouter();
const route = useRoute()

const navbarItems = [
  {
    id: 'course-selection',
    label: '学生选课',
    icon: 'calendar-outline',
    route: '/student/course-selection'
  },
  {
    id: 'knowledge-management',
    label: '知识点管理',
    icon: 'book-outline',
    route: '/student/knowledgeManagement'
  },
  {
    id: 'error-analysis',
    label: '错题分析',
    icon: 'document-text-outline',
    route: '/student/error-analysis'
  },
  {
    id: 'teacher-chat',
    label: '师生对话',
    icon: 'chatbubble-outline',
    route: '/student/teacher-chat'
  },
  {
    id: 'community-student',
    label: '社区交流中心',
    icon: 'people-outline',
    route: '/student/community-student'
  },
  {
    id: 'game-entry',
    label: '神秘之塔',
    icon: 'game-controller-outline',
    route: '/student/game-entry'
  }
]

const handleNavbarItemClick = async (item) => {
  if (item.route) {
    // 如果是神秘之塔，先检查角色信息
    if (item.id === 'game-entry') {
      try {
        // 这里可以添加角色检查逻辑，如果需要的话
        // 目前直接跳转，角色检查在Entry.vue中进行
        router.push(item.route);
      } catch (error) {
        console.error('进入神秘之塔失败:', error);
        // 如果失败，仍然跳转，让Entry.vue处理错误
        router.push(item.route);
      }
    } else {
      router.push(item.route);
    }
  }
};

const currentPageTitle = computed(() => {
  const findTitle = (items, route) => {
    for (const item of items) {
      if (item.route === route) {
        return item.label;
      }
    }
    return '';
  };
  return findTitle(navbarItems, route.path);
});

// 判断当前是否在游戏页面
const isGamePage = computed(() => {
  return route.path === '/student/game-entry';
});

onMounted(() => {
  // 可以在这里进行一些初始化操作，例如根据路由设置初始 activeMenu
});
</script>

<style scoped>
.common-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background-color: #f5f7fa;
}

.right-container {
  flex-direction: column;
  width: calc(100% - 5em); /* Assuming sidebar width is 250px */
  border-radius: 8px; /* Slightly rounded corners */
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  overflow: hidden;  
  margin-left: 5em;
}

.main-content {
  flex-grow: 1;
  background-color: #ffffff;  
  overflow-y: auto;
  border-radius: 0 0 8px 8px;  
}
</style>