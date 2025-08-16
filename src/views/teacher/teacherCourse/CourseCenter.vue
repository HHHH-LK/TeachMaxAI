<template>
  <div class="page-container">
    <!-- 合并的头部区域 -->
    <header class="header">
      <div class="header-content">
        <div class="header-top">
          <div class="header-left">
            <el-button
                type="text"
                @click="goBack"
                class="back-btn"
                style="color: white; margin-right: 16px"
            >
              <el-icon><ArrowLeft /></el-icon>
              返回课程列表
            </el-button>
            <h1 class="header-title">{{ courseInfo.name }}</h1>
          </div>
          <div class="header-right">
            <div class="user-info">
              <span class="user-name">{{ courseInfo.teacher }}</span>
              <img
                  src="@/assets/teacher-avatar.png"
                  alt="User Avatar"
                  class="user-avatar"
              />
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 主布局：侧边栏和内容区 -->
    <div class="main-layout">
      <!-- 侧边栏区域 -->
      <aside class="sidebar">
        <nav>
          <ul>
            <li
                v-for="item in navItems"
                :key="item.name"
                :class="[
                'nav-item',
                { 'nav-item-active': activeNavItem === item.name },
              ]"
                @click="activeNavItem = item.name"
            >
              <div class="nav-item-content">
                <svg
                    class="nav-icon"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      :d="item.icon"
                  ></path>
                </svg>
                <span>{{ item.name }}</span>
              </div>
            </li>
          </ul>
        </nav>
      </aside>

      <!-- 内容区域 -->
      <main class="content-area">
        <!-- 主内容面板 -->
        <div class="main-content-panel">
          <!-- 内容显示区 -->
          <div v-if="activeNavItem === '章节目录'">
            <ChapterList :courseId="courseId" />
          </div>
          <div v-else-if="activeNavItem === '课堂作业'">
            <HomeworkManagement :courseId="courseId" />
          </div>
          <div v-else-if="activeNavItem === '教学资源'">
            <TeachingResources />
          </div>
          <div v-else-if="activeNavItem === '备课资料'">
            <LessonPlanGenerator />
          </div>
          <div v-else-if="activeNavItem === '作业情况'">
            <HomeworkStats :course-id="courseId" />
          </div>
          <div v-else-if="activeNavItem === '成绩统计'">
            <GradeStatistics />
          </div>
          <div v-else-if="activeNavItem === '考核管理'">
            <AssessmentManagement :course-id="courseId" />
          </div>
          <div v-else class="empty-data-message">
            <img
                src="https://placehold.co/200x150/e0e0e0/ffffff?text=No+Data"
                alt="No Data Illustration"
                class="empty-data-image"
            />
            <p>暂无数据</p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft } from "@element-plus/icons-vue";
import ChapterList from "@/components/teacher/ChapterList.vue";
import HomeworkManagement from "@/components/teacher/HomeworkManagement.vue";
import HomeworkStats from "@/components/teacher/HomeworkStats.vue";
import TeachingResources from "@/components/teacher/TeachingResources.vue";
import LessonPlanGenerator from "@/components/teacher/LessonPlanGenerator.vue";
import AssessmentManagement from "@/components/teacher/AssessmentManagement.vue";
import GradeStatistics from "@/components/teacher/GradeStatistics.vue";
import { teacherService } from "@/services/api";
import {getCurrentUserId} from "@/utils/userUtils.js";

// 路由参数
const route = useRoute();
const router = useRouter();
const courseId = route.params.courseId;

// 课程信息
const courseInfo = ref({
  id: courseId,
  name: "",
  teacher: "",
});

const navItems = ref([
  { name: "章节目录", icon: "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" },
  { name: "考核管理", icon: "M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m14 0a2 2 0 100-4m0 4a2 2 0 110-4M6 9h12m-3 3h3m-9 3h9" },
  { name: "成绩统计", icon: "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" },
  { name: "课堂作业", icon: "M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2m0 0H5a2 2 0 00-2 2v6a2 2 0 002 2h14a2 2 0 002-2v-6a2 2 0 00-2-2z" },
  { name: "作业情况", icon: "M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" },
  { name: "教学资源", icon: "M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" },
  { name: "备课资料", icon: "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" }
]);

const activeNavItem = ref("章节目录");
// const teacherId = "1";

// 根据课程ID加载课程信息
const loadCourseInfo = async () => {
  // 这里可以根据courseId从API获取课程信息
  // console.log("加载课程信息:", courseId);

  const userId = getCurrentUserId()
  const response1 = await teacherService.getTeacherInfo(userId)
  const teacherId = response1.data.data.teacherId

  // console.log({ userId, teacherId })
  const response = await teacherService.getAllCourse(teacherId);
  // console.log("获取课程成功", response)
  if (response.data) {
    const courseData = response.data.data.map((item) => ({
      id: item.courseId,
      name: item.courseName,
      teacher: "张教授",
    }));
    let foundCourse = null;
    for (let i = 0; i < courseData.length; i++) {
      // 确保比较相同类型
      if (String(courseData[i].id) === courseId) {
        // console.log("inside")
        courseInfo.value = {
          id: courseId,
          name: courseData[i].name,
          teacher: "张教授"
        };
        break; // 找到匹配项后立即退出循环
      }
    }
  }

  // 模拟根据课程ID获取不同的课程信息
  const courseData = {
    1: { name: '高等数学基础', teacher: '李老师', class: '2306801', studentCount: 32 },
    2: { name: '数据结构与算法实验', teacher: '王老师', class: '2306802', studentCount: 32 },
    3: { name: '软件工程研讨', teacher: '张老师', class: '2306803', studentCount: 32 },
    4: { name: 'Web开发技术', teacher: '陈老师', class: '2306804', studentCount: 32 },
    5: { name: '人工智能导论', teacher: '刘老师', class: '2306805', studentCount: 32 },
    6: { name: '数据库系统实验', teacher: '赵老师', class: '2306806', studentCount: 32 }
  };
};

// 返回课程列表
const goBack = () => {
  router.push({ name: "TeacherCourseDesign" });
};

onMounted(() => {
  loadCourseInfo();
});
</script>

<style scoped lang="less">
/* 全局样式 */
@primary: #2563eb; /* 主蓝色 */
@primary-light: #3b82f6; /* 浅一点的蓝色 */
@primary-dark: #1d4ed8; /* 深一点的蓝色 */

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.page-container {
  min-height: 100vh;
  background-color: #f8fafc;
  font-family: "Inter", system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1e293b;
  line-height: 1.5;
  display: flex;
  flex-direction: column;
}

/* 合并的头部区域样式 */
.header {
  background: linear-gradient(135deg, @primary-dark, @primary);
  color: #fff;
  padding: 1.5rem 2rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border-radius: 0 0 1rem 1rem;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.header::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.6;
}

.header-content {
  position: relative;
  z-index: 1;
}

.header-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

@media (min-width: 768px) {
  .header-top {
    flex-direction: row;
    margin-bottom: 1.5rem;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 0.75rem;
}

.back-btn {
  transition: all 0.2s ease;
  border-radius: 8px;
  padding: 8px 12px;
}

.back-btn:hover {
  background-color: rgba(255, 255, 255, 0.1);
  transform: translateX(-2px);
}

@media (min-width: 768px) {
  .header-left {
    margin-bottom: 0;
  }
}

.header-title {
  font-size: 1.8rem;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.header-tag {
  background-color: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  font-size: 0.875rem;
  padding: 0.3rem 0.85rem;
  border-radius: 9999px;
  white-space: nowrap;
  transition: background-color 0.2s ease;
}

.header-tag:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

.header-info {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .header-right {
    flex-direction: row;
    gap: 1.5rem;
    margin-left: auto;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  border-radius: 50%;
  width: 50px;
  height: 50px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  transition: transform 0.2s ease;
}

.user-avatar:hover {
  transform: scale(1.05);
}

.user-name {
  font-weight: 500;
  white-space: nowrap;
}

.user-status {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
}

.logout-link {
  text-decoration: none;
  color: inherit;
  position: relative;
}

.logout-link::after {
  content: "";
  position: absolute;
  width: 0;
  height: 1px;
  bottom: -1px;
  left: 0;
  background-color: currentColor;
  transition: width 0.2s ease;
}

.logout-link:hover {
  color: #fff;
}

.logout-link:hover::after {
  width: 100%;
}

/* 头部底部信息栏样式 */
.header-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 0;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  gap: 0.75rem;
}

@media (min-width: 768px) {
  .header-bottom {
    flex-direction: row;
    gap: 0;
  }
}

.info-section {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.info-item {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
}

.info-separator {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
}

.info-value {
  font-weight: 600;
  color: #fff;
}

.info-value.online {
  color: #10b981;
  position: relative;
  padding-left: 1.25rem;
}

.info-value.online::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.3);
}

/* 主布局样式 */
.main-layout {
  display: flex;
  flex: 1;
  margin-top: 1.5rem;
  padding: 0 1rem;
  min-height: 0;
}

@media (min-width: 768px) {
  .main-layout {
    margin-top: 2rem;
    padding: 0 2rem;
  }
}

/* 侧边栏样式 */
.sidebar {
  width: 100%;
  background-color: #fff;
  padding: 1.25rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05),
  0 4px 6px -2px rgba(0, 0, 0, 0.03);
  flex-shrink: 0;
  border-radius: 0.75rem;
  margin-bottom: 1.5rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  height: fit-content;
}

.sidebar:hover {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05),
  0 10px 10px -5px rgba(0, 0, 0, 0.02);
}

@media (min-width: 768px) {
  .sidebar {
    width: 16rem;
    margin-right: 1.5rem;
    margin-bottom: 0;
    position: sticky;
    top: 1.5rem;
    align-self: flex-start;
  }
}

.sidebar ul {
  list-style: none;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  border-radius: 0.5rem;
  cursor: pointer;
  margin-bottom: 0.35rem;
  transition: all 0.25s ease;
  color: #475569;
  position: relative;
  overflow: hidden;
}

.nav-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 3px;
  background-color: #3b82f6;
  transform: translateX(-100%);
  transition: transform 0.2s ease;
}

.nav-item:hover {
  background-color: #f1f5f9;
  color: #334155;
  transform: translateX(2px);
}

.nav-item-active {
  background-color: #eff6ff;
  color: #1e40af;
  font-weight: 600;
}

.nav-item-active::before {
  transform: translateX(0);
}

.nav-item-content {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.nav-icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

/* 内容区域样式 */
.content-area {
  flex: 1;
  padding: 0;
  background-color: transparent;
  border-radius: 0.75rem;
  min-height: 0;
  overflow: hidden;
}

/* 主内容面板样式 */
.main-content-panel {
  background-color: #fff;
  padding: 1.5rem;
  border-radius: 0.75rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05),
  0 4px 6px -2px rgba(0, 0, 0, 0.03);
  transition: box-shadow 0.2s ease;
  height: 100%;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.main-content-panel:hover {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05),
  0 10px 10px -5px rgba(0, 0, 0, 0.02);
}

/* 自定义滚动条样式 */
.main-content-panel::-webkit-scrollbar {
  width: 8px;
}

.main-content-panel::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.main-content-panel::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
  transition: background 0.2s ease;
}

.main-content-panel::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 内容显示区（无数据）样式 */
.empty-data-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: #94a3b8;
  text-align: center;
  background-color: #f8fafc;
  border-radius: 0.75rem;
  margin-top: 1rem;
  transition: all 0.2s ease;
}

.empty-data-message:hover {
  background-color: #f1f5f9;
}

.empty-data-image {
  margin-bottom: 1.5rem;
  border-radius: 0.5rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.empty-data-message:hover .empty-data-image {
  transform: scale(1.05);
}

.empty-data-message p {
  font-size: 1rem;
  font-weight: 500;
  margin: 0;
}

/* 功能占位符样式 */
.feature-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 40px;
}
</style>
