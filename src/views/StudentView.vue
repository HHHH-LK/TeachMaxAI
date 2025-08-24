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
          <router-view></router-view>
          <!-- 关键修改：用 !shouldHideAiAssistant 控制，同时排除两种页面 -->
          <aiAssistant v-if="!shouldHideAiAssistant" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import studentAvatar from '@/assets/student-avatar.png';
import CommonHeader from '../components/common/CommonHeader.vue';
import aiAssistant from '../components/aIAssistant.vue';
import Sider from '@/components/common/Sider.vue'
import { useRoute, useRouter } from 'vue-router';
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
    if (item.id === 'game-entry') {
      try {
        router.push(item.route);
      } catch (error) {
        console.error('进入神秘之塔失败:', error);
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

// 关键修改：新增判断逻辑，覆盖「游戏页面」和「带参数的课程中心页面」
const shouldHideAiAssistant = computed(() => {
  // 1. 游戏页面（精确匹配固定路径）
  const isGamePage = route.path === '/student/game-entry';
  // 2. 带参数的课程中心页面（模糊匹配：路径以 /student/course-center/ 开头，如 /student/course-center/20）
  const isCourseCenterDynamicPage = route.path.startsWith('/student/course-center/');

  // 满足任一条件，就需要禁用 aiAssistant
  return isGamePage || isCourseCenterDynamicPage;
});

onMounted(() => {
  // 初始化操作（不变）
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
  width: calc(100% - 5em);
  border-radius: 8px;
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