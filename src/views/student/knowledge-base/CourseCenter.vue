<template>
  <div class="course-page">
    <!-- 头部区域 -->
    <header class="course-header">
      <div class="header-left">
        <div class="course-breadcrumb">
          <span>课程中心</span>
          <span>/</span>
          <span class="meta-item"
            ><i class="icon-teacher"></i> {{ courseTeachers }}</span
          >
        </div>
        <h1 class="course-title">{{ courseTitle }}</h1>
      </div>
      <div class="header-right">
        <div class="header-actions">
          <button class="action-button ai-btn" @click="goToAIAnalysis">
            <i class="icon-ai"></i> AI学情分析
          </button>
          <button class="action-button ai-btn" @click="goToExamGenerator">
            <i class="icon-exam"></i> AI生成试题
          </button>
          <button class="action-button ai-btn" @click="goToErrorCollection">
            <i class="icon-error"></i> 错题集总结
          </button>
        </div>
      </div>
    </header>

    <!-- 导航标签 -->
    <nav class="course-nav">
      <ul class="nav-tabs">
        <li
          v-for="tab in navTabs"
          :key="tab.name"
          :class="[
            'nav-item',
            { active: activeTab === tab.name, disabled: isNavDisabled },
          ]"
          @click="!isNavDisabled && (activeTab = tab.name)"
        >
          <i :class="tab.icon"></i>
          <span>{{ tab.label }}</span>
          <span v-if="tab.beta" class="beta-tag">beta</span>
        </li>
      </ul>
      <button class="exit-button" @click="exitCourse">
        <i class="icon-exit"></i> 退出课程
      </button>
    </nav>

    <!-- 主要内容区 -->
    <main class="course-content">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载课程信息中...</p>
      </div>
      
      <div v-else-if="error" class="error-state">
        <i class="icon-error"></i>
        <h3>加载失败</h3>
        <p>{{ error }}</p>
        <button @click="retryLoading">重试</button>
      </div>
      
      <div v-else>
        <AIAnalysisView 
          v-if="showAIAnalysis"
          @back="goBackToChatView" 
          :course-id="courseId"
        />
        <ExamGenerator
          v-else-if="showExamGenerator"
          @back="goBackFromExamGenerator"
          :course-id="courseId"
        />
        <ErrorCollectionView
          v-else-if="showErrorCollection"
          @back="goBackFromErrorCollection"
          :course-id="courseId"
        />
        <component 
          :is="activeComponent" 
          v-else
          :course-id="courseId"
        ></component>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import CatalogView from "./CatalogView.vue";
import ResourceView from "./ResourceView.vue";
import HomeworkView from "./HomeworkView.vue";
import ExamView from "./ExamView.vue";
import { useRoute, useRouter } from "vue-router";
import AIAnalysisView from "@/components/student/knowledge/AIAnalysisView.vue";
import ExamGenerator from "@/components/student/knowledge/ExamGenerator.vue";
import ErrorCollectionView from "@/components/student/knowledge/ErrorCollectionView.vue";
import { studentService } from "@/services/api";

const route = useRoute();
const router = useRouter();

// 状态变量
const courseId = ref(null);
const courseTitle = ref("加载中...");
const courseTeachers = ref("加载中...");
const showAIAnalysis = ref(false);
const showExamGenerator = ref(false);
const showErrorCollection = ref(false);
const allCourses = ref([]);
const loading = ref(true);
const error = ref(null);

// 导航标签数据
const navTabs = ref([
  { name: "catalog", label: "目录", icon: "icon-catalog" },
  { name: "preview", label: "资源", icon: "icon-preview" },
  { name: "homework", label: "作业", icon: "icon-homework" },
  { name: "exam", label: "考试", icon: "icon-exam" },
]);

// 当前激活标签
const activeTab = ref("catalog");

// 根据 activeTab 动态选择组件
const activeComponent = computed(() => {
  switch (activeTab.value) {
    case "catalog": return CatalogView;
    case "preview": return ResourceView;
    case "homework": return HomeworkView;
    case "exam": return ExamView;
    default: return CatalogView;
  }
});

// 导航是否禁用
const isNavDisabled = computed(
  () => showAIAnalysis.value || showExamGenerator.value || showErrorCollection.value
);

// AI学情分析页面
const goToAIAnalysis = () => {
  showAIAnalysis.value = true;
  showExamGenerator.value = false;
  showErrorCollection.value = false;
};

// 退回页面
const goBackToChatView = () => {
  showAIAnalysis.value = false;
};

// AI生成试题
const goToExamGenerator = () => {
  showExamGenerator.value = true;
  showAIAnalysis.value = false;
  showErrorCollection.value = false;
};

// 跳转回页面
const goBackFromExamGenerator = () => {
  showExamGenerator.value = false;
};

// 错题集总结
const goToErrorCollection = () => {
  showErrorCollection.value = true;
  showAIAnalysis.value = false;
  showExamGenerator.value = false;
};

// 跳转回页面
const goBackFromErrorCollection = () => {
  showErrorCollection.value = false;
};

// 重新加载数据
const retryLoading = async () => {
  loading.value = true;
  error.value = null;
  await loadCourseData();
};

// 加载课程数据
const loadCourseData = async () => {
  try {
    // 从路由获取课程ID
    const id = parseInt(route.params.id);
    if (isNaN(id)) {
      throw new Error("无效的课程ID");
    }
    
    courseId.value = id;
    
    // 获取用户所有课程
    const response = await studentService.getOwnCourses();
    
    if (response.data) {
      // 格式化课程数据
      allCourses.value = response.data.data.map(course => ({
        id: course.courseId,
        title: course.courseName,
        teacher: course.teacherName,
        describe: course.courseDescription,
      }));
      
      // 查找当前课程
      const matchedCourse = allCourses.value.find(c => c.id === courseId.value);
      
      if (matchedCourse) {
        courseTitle.value = matchedCourse.title;
        courseTeachers.value = matchedCourse.teacher;
      } else {
        throw new Error(`未找到ID为 ${courseId.value} 的课程`);
      }
    } else {
      throw new Error("获取课程数据失败");
    }
  } catch (err) {
    console.error("加载课程数据失败:", err);
    error.value = err.message || "加载课程失败";
  } finally {
    loading.value = false;
  }
};

// 监听路由变化
watch(
  () => route.params.id,
  (newId) => {
    const newCourseId = parseInt(newId);
    if (newCourseId && newCourseId !== courseId.value) {
      courseId.value = newCourseId;
      loadCourseData();
    }
  }
);

// 退出课程
const exitCourse = () => {
  router.push({ name: "KnowledgePointManagement" });
};

// 初始加载
onMounted(() => {
  loadCourseData();
});
</script>

<style lang="less" scoped>
//鄙人自定义色彩
@primary: #2563eb; /* 主蓝色 */
@primary-light: #3b82f6; /* 浅一点的蓝色 */
@primary-dark: #1d4ed8; /* 深一点的蓝色 */
@primary-bg: #eff6ff; /* 蓝色背景 */
@primary-border: #dbeafe; /* 蓝色边框 */
@secondary: #60a5fa; /* 辅助蓝色 */
@success: #10b981; /* 成功色（搭配蓝色） */
@warning: #f59e0b; /* 警告色 */
@danger: #ef4444; /* 危险色 */
@text-primary: #1e293b; /* 主要文字 */
@text-secondary: #64748b; /* 次要文字 */
@text-tertiary: #94a3b8; /* tertiary文字 */
@bg-white: #ffffff; /* 白色背景 */
@bg-light: #f8fafc; /* 浅色背景 */
@border-light: #e2e8f0; /* 浅色边框 */
@shadow-sm: 0 2px 8px rgba(37, 99, 235, 0.1); /* 小阴影 */
@shadow-md: 0 4px 16px rgba(37, 99, 235, 0.15); /* 中阴影 */
@radius-sm: 6px;
@radius-md: 12px;
@radius-lg: 16px;
@transition: all 0.3s ease;
@breakpoint-md: 1024px;
@breakpoint-sm: 768px;

// 混合 - 图标基础样式
.icon-base() {
  display: inline-block;
  width: 1em;
  height: 1em;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.course-page {
  font-family: "Inter", "Helvetica Neue", sans-serif;
  background-color: @bg-light;
  min-height: 100vh;
  padding: 24px;
  box-sizing: border-box;

  // 头部样式
  .course-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    background: linear-gradient(135deg, @primary-dark, @primary);
    color: white;
    padding: 32px 40px;
    border-radius: @radius-lg;
    box-shadow: @shadow-md;
    margin-bottom: 28px;
    transition: @transition;

    &:hover {
      box-shadow: 0 8px 24px rgba(37, 99, 235, 0.2);
    }

    .course-breadcrumb {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .course-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 12px 0;
      color: white;
      letter-spacing: 0.3px;
    }

    .course-meta {
      display: flex;
      align-items: center;
      gap: 20px;
      flex-wrap: wrap;
      font-size: 13px;
      color: rgba(255, 255, 255, 0.7);

      .meta-item {
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }

    .header-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;

      .action-button {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        background-color: rgba(255, 255, 255, 0.15);
        backdrop-filter: blur(4px);
        color: white;
        border: 1px solid rgba(255, 255, 255, 0.2);
        padding: 10px 18px;
        border-radius: @radius-sm;
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: @transition;

        &:hover {
          background-color: rgba(255, 255, 255, 0.25);
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
        }
      }

      .ai-btn {
        position: relative;
        overflow: hidden;

        &::after {
          content: "";
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: linear-gradient(
            90deg,
            transparent,
            rgba(255, 255, 255, 0.1),
            transparent
          );
          transition: 0.5s;
        }

        &:hover::after {
          left: 100%;
        }
      }
    }
  }

  // 导航标签样式
  .course-nav {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: @bg-white;
    padding: 0 40px;
    border-radius: @radius-md;
    box-shadow: @shadow-sm;
    margin-bottom: 28px;
    height: 64px;

    .nav-tabs {
      display: flex;
      gap: 32px;
      list-style: none;
      padding: 0;
      margin: 0;
      overflow-x: auto;
      scrollbar-width: none;
      height: 100%;

      &::-webkit-scrollbar {
        display: none;
      }

      .nav-item {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 0 4px;
        height: 100%;
        font-size: 15px;
        font-weight: 500;
        color: @text-secondary;
        cursor: pointer;
        position: relative;
        transition: @transition;
        white-space: nowrap;

        &.active {
          color: @primary;

          &::after {
            content: "";
            position: absolute;
            bottom: 0;
            left: 0;
            width: 100%;
            height: 3px;
            background-color: @primary;
            border-radius: 3px 3px 0 0;
          }
        }

        &:not(.active):hover {
          color: @primary;
          background-color: @primary-bg;
        }

        &.disabled {
          pointer-events: none;
          opacity: 0.5;
          cursor: not-allowed;
        }

        .beta-tag {
          font-size: 10px;
          background-color: @primary-bg;
          color: @primary;
          padding: 1px 6px;
          border-radius: 4px;
          margin-left: 4px;
          font-weight: 600;
        }
      }
    }

    .exit-button {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background-color: transparent;
      color: @danger;
      border: 1px solid rgba(239, 68, 68, 0.2);
      padding: 8px 16px;
      border-radius: @radius-sm;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: @transition;

      &:hover {
        background-color: rgba(239, 68, 68, 0.05);
        border-color: rgba(239, 68, 68, 0.3);
      }
    }
  }

  // 内容区域样式
  .course-content {
    background-color: @bg-white;
    padding: 36px 40px;
    border-radius: @radius-md;
    box-shadow: @shadow-sm;
  }

  // 图标样式
  [class^="icon-"] {
    .icon-base();
  }

  .icon-teacher {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2'%3E%3C/path%3E%3Ccircle cx='12' cy='7' r='4'%3E%3C/circle%3E%3C/svg%3E");
  }

  .icon-ai {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'%3E%3C/path%3E%3Cpath d='M13.73 21a2 2 0 0 1-3.46 0'%3E%3C/path%3E%3C/svg%3E");
  }

  .icon-exam {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M9 11l3 3L22 4'%3E%3C/path%3E%3Cpath d='M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11'%3E%3C/path%3E%3C/svg%3E");
  }

  .icon-error {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='12' r='10'%3E%3C/circle%3E%3Cline x1='15' y1='9' x2='9' y2='15'%3E%3C/line%3E%3Cline x1='9' y1='9' x2='15' y2='15'%3E%3C/line%3E%3C/svg%3E");
  }

  .icon-exit {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'%3E%3C/path%3E%3Cpolyline points='16 17 21 12 16 7'%3E%3C/polyline%3E%3Cline x1='21' y1='12' x2='9' y2='12'%3E%3C/line%3E%3C/svg%3E");
  }

  .icon-catalog {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cline x1='8' y1='6' x2='21' y2='6'%3E%3C/line%3E%3Cline x1='8' y1='12' x2='21' y2='12'%3E%3C/line%3E%3Cline x1='8' y1='18' x2='21' y2='18'%3E%3C/line%3E%3Cline x1='3' y1='6' x2='3.01' y2='6'%3E%3C/line%3E%3Cline x1='3' y1='12' x2='3.01' y2='12'%3E%3C/line%3E%3Cline x1='3' y1='18' x2='3.01' y2='18'%3E%3C/line%3E%3C/svg%3E");
  }

  .icon-preview {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z'%3E%3C/path%3E%3C/svg%3E");
  }

  .icon-homework {
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7'%3E%3C/path%3E%3Cpath d='M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z'%3E%3C/path%3E%3C/svg%3E");
  }
}

// 响应式设计
@media (max-width: @breakpoint-md) {
  .course-page {
    .course-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 20px;

      .header-right {
        align-self: flex-end;
      }
    }

    .course-nav {
      .nav-tabs {
        gap: 20px;
      }
    }
  }
}

@media (max-width: @breakpoint-sm) {
  .course-page {
    padding: 16px;

    .course-header,
    .course-nav,
    .course-content {
      padding: 16px 20px;
    }

    .course-header {
      .course-title {
        font-size: 24px;
      }

      .header-actions {
        width: 100%;
        justify-content: flex-end;
      }
    }

    .course-content {
      .content-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
      }
    }
  }
}
</style>
