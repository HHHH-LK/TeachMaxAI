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
          <teacher-a-i-assistant />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import studentAvatar from '@/assets/student-avatar.png';
import CommonHeader from '../components/common/CommonHeader.vue';
import TeacherAIAssistant from "@/components/TeacherAIAssistant.vue";
import Sider from '@/components/common/Sider.vue'
import {useRoute, useRouter} from 'vue-router';
import HeadBar from '@/components/common/HeadBar.vue';

const router = useRouter();
const route = useRoute()

const navbarItems = [
  { id: 'course-design', label: '课程管理中心', icon: 'book-outline', route: '/teacher/course-design' },
  { id: 'analysis', label: '学情数据分析', icon: 'bar-chart-outline', route: '/teacher/analysis' },
  { id: 'student-management', label: '学生信息管理', icon: 'people-outline', route: '/teacher/student-management' },
  { id: 'student-chat', label: '师生对话', icon: 'chatbox-ellipses-outline', route: '/teacher/student-chat' },
  { id: 'community', label: '社区交流', icon: 'chatbubbles-outline', route: '/teacher/community' },
];

const handleNavbarItemClick = (item) => {
  if (item.route) {
    router.push(item.route);
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
  padding: 20px;
  background-color: #ffffff;
  overflow-y: auto;
  border-radius: 0 0 8px 8px;
}
</style>