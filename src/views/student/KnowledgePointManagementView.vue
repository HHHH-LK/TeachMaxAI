<template>
  <div class="knowledge-management-container">
    <!-- 顶部导航 -->
    <header class="page-header">
      <div class="header-content">
        <h1 class="page-title">知识管理中心</h1>
        <p class="page-subtitle">探索并管理您的课程资源</p>
      </div>
    </header>

    <!-- 搜索筛选区域 -->
    <div class="search-filter-section">
      <div class="filter-container">
        <div class="filter-group">
          <label for="semester-select" class="filter-label">选择学期:</label>
          <select
              id="semester-select"
              v-model="selectedSemester"
              class="custom-select"
              @change="handleFilterChange"
          >
            <option value="">所有学期</option>
            <option
                v-for="semester in uniqueSemesters"
                :key="semester"
                :value="semester"
            >
              {{ semester }}
            </option>
          </select>
        </div>

        <div class="filter-group search-box">
          <label for="course-search" class="filter-label">搜索课程:</label>
          <div class="search-input-group">
            <input
                type="text"
                id="course-search"
                v-model="searchQuery"
                placeholder="输入课程名称或关键词"
                class="custom-input"
                @keyup.enter="performSearch"
            />
            <button @click="performSearch" class="search-button">
              <i class="search-icon">
                <svg
                    t="1751935915833"
                    class="icon"
                    viewBox="0 0 1024 1024"
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                >
                  <path
                      d="M686.321022 776.467198a430.406003 430.406003 0 1 1 90.146187-90.146187l228.472696 228.399585a63.753021 63.753021 0 0 1-90.219298 90.219298L686.321022 776.467198z m-14.256696-114.492237a334.70336 334.70336 0 1 0-10.089354 10.089354 64.484134 64.484134 0 0 1 10.089354-10.089354z"
                      fill="#FFFFFF"
                      fill-opacity=".4"
                  />
                </svg>
              </i>
              <span>搜索</span>
            </button>
          </div>
        </div>
      </div>

      <div class="filter-stats" v-if="filteredCourses.length > 0">
        <p>
          找到
          <span class="highlight">{{ filteredCourses.length }}</span> 门课程
        </p>
        <button
            class="clear-filters"
            @click="clearFilters"
            v-if="selectedSemester || searchQuery"
        >
          清除筛选
        </button>
      </div>
    </div>

    <!-- 课程卡片网格 -->
    <div class="course-cards-grid">
      <Card
          v-for="course in filteredCourses"
          :key="course.id"
          :data-image="course.imageUrl"
          @click="goToCourseCenter(course.id)"
      >
        <template #header>
          <h1>{{ course.title }}</h1>
        </template>
        <template #content>
          <p>{{ course.description }}</p>
        </template>
      </Card>
      <p v-if="filteredCourses.length === 0" class="no-results">
        没有找到相关课程。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import Card from "@/components/student/knowledge/Card.vue";
import { ElMessage } from "element-plus";
import img1 from "@/assets/img_1.png";
import img2 from "@/assets/img_2.png";
import img3 from "@/assets/img_3.png";
import img4 from "@/assets/img.png";
import img5 from "@/assets/img_4.png";
import { studentService } from "@/services/api";

const router = useRouter();

const selectedSemester = ref("");
const searchQuery = ref("");
const allCourses = ref([]);
const isLoading = ref(true);
const error = ref(null);

const defaultCourses = [
  {
    id: 1,
    title: "高等数学A",
    description: "本课程涵盖微积分、线性代数等基础数学知识。",
    semester: "2025春",
    imageUrl: img1,
  },
  {
    id: 2,
    title: "大学物理B",
    description: "介绍力学、电磁学、光学等物理学基本原理。",
    semester: "2025春",
    imageUrl: img2,
  },
  {
    id: 3,
    title: "数据结构与算法",
    description: "学习常见数据结构和算法，提升编程能力。",
    semester: "2024秋",
    imageUrl: img3,
  },
  {
    id: 4,
    title: "计算机网络",
    description: "深入理解网络协议、网络架构和网络安全。",
    semester: "2024秋",
    imageUrl: img4,
  },
  {
    id: 5,
    title: "操作系统原理",
    description: "探讨操作系统的设计原理、进程管理、内存管理等。",
    semester: "2024春",
    imageUrl: img5,
  },
];

// 获取课程
const fetchCourses = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const response = await studentService.getOwnCourses();
    if (response.data && response.data.data) {
      allCourses.value = response.data.data.map((course) => ({
        id: course.courseId,
        title: course.courseName,
        description: course.courseDescription,
        semester: course.semester || "未知学期",
        imageUrl: course.imageUrl || 'https://tse4-mm.cn.bing.net/th/id/OIP-C.v7JAGdYjVso8nIqmbtN_bAHaHa?w=173&h=180&c=7&r=0&o=5&dpr=1.5&pid=1.7',
      }));
      
      console.log(response.data.data)
    } else {
      allCourses.value = defaultCourses;
      ElMessage.warning("后端没有返回课程数据，已使用默认数据。");
    }
  } catch (err) {
    error.value = "获取课程数据失败：" + err.message;
    allCourses.value = defaultCourses;
    ElMessage.error("获取课程数据失败，已使用默认数据。");
  } finally {
    isLoading.value = false;
  }
};

// 计算唯一的学期列表
const uniqueSemesters = computed(() => {
  const semesters = new Set();
  allCourses.value.forEach(course => {
    if (course.semester) {
      semesters.add(course.semester);
    }
  });
  return Array.from(semesters).sort().reverse(); 
});

onMounted(() => {
  fetchCourses();
});

const filteredCourses = computed(() => {
  let courses = [...allCourses.value];
  
  // 学期筛选
  if (selectedSemester.value) {
    courses = courses.filter(course => course.semester === selectedSemester.value);
  }
  
  // 搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    courses = courses.filter(course =>
        (course.title && course.title.toLowerCase().includes(query)) ||
        (course.description && course.description.toLowerCase().includes(query))
    );
  }
  
  return courses;
});

const performSearch = () => {
  document.querySelector(".course-cards-grid")?.scrollIntoView({ behavior: "smooth" });
};


const clearFilters = () => {
  selectedSemester.value = "";
  searchQuery.value = "";
};

const goToCourseCenter = (courseId) => {
  if (!router) {
    ElMessage.error("Router 未初始化！");
    return;
  }
  router.push({ name: "CourseCenter", params: { id: courseId } })
      .catch(err => console.error("跳转失败:", err));
};
</script>

<style scoped lang="less">
// 优雅蓝色系配色方案
@primary: #2563eb; /* 主蓝色 */
@primary-light: #e6f0ff;    // 浅蓝背景
@primary-base: #4096ff;     // 基础蓝
@primary-medium: #165dff;   // 中蓝（主色）
@primary-dark: #0e42d2;     // 深蓝（强调）
@primary-glass: rgba(64, 150, 255, 0.15); // 透明蓝
@text-primary: #1d2129;     // 主要文字
@text-secondary: #4e5969;   // 次要文字
@border-light: #c9d1e0;     // 浅色边框
@bg-white: #ffffff;         // 白色背景
@border-radius: 10px;
@shadow-md: 0 4px 16px rgba(64, 150, 255, 0.12); // 蓝色阴影

.knowledge-management-container {
  padding: 0;
  background: linear-gradient(to right, @primary-light, @bg-white);
  min-height: 100vh;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
}

// 顶部导航样式
.page-header {
  background: linear-gradient(135deg, @primary-dark, @primary);
  padding: 40px 20px;
  position: relative;
  overflow: hidden;

  &::after {
    content: "";
    position: absolute;
    top: 0;
    right: 0;
    width: 50%;
    height: 100%;
    background-image: url("data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTM2IDM0TDI2IDM5TDI2IDM5TDI2IDM5TDE2IDM0TDE2IDM0TDI2IDI5TDM2IDM0WiIvPjwvZz48L2c+PC9zdmc+");
    background-repeat: repeat;
    opacity: 0.5;
  }

  .header-content {
    max-width: 1200px;
    margin: 0 auto;
    position: relative;
    z-index: 10;
  }

  .page-title {
    font-size: 2.5rem;
    font-weight: 700;
    color: #fff;
    margin: 0 0 0.5rem;
  }

  .page-subtitle {
    font-size: 1.2rem;
    color: rgba(255, 255, 255, 0.9);
    margin: 0;
    font-weight: 300;
  }
}

// 搜索筛选区域样式
.search-filter-section {
  max-width: 1200px;
  margin: -2.5rem auto 3rem;
  padding: 0 20px;
  position: relative;
  z-index: 20;

  .filter-container {
    background-color: @bg-white;
    border-radius: @border-radius;
    padding: 25px 35px;
    box-shadow: @shadow-md;
    display: flex;
    flex-wrap: wrap;
    gap: 25px;
    align-items: center;
  }

  .filter-group {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .filter-label {
    font-size: 1.1em;
    color: @text-secondary;
    font-weight: 600;
    white-space: nowrap;
  }

  .custom-select,
  .custom-input {
    padding: 12px 16px;
    border: 1px solid @border-light;
    border-radius: @border-radius;
    font-size: 1em;
    color: @text-primary;
    background-color: @primary-light;
    transition: all 0.3s ease;
    min-width: 200px;

    &:focus {
      border-color: @primary-base;
      box-shadow: 0 0 0 4px @primary-glass;
      background-color: @bg-white;
      outline: none;
    }
  }

  .custom-select {
    appearance: none;
    background-image: url("data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIiIGhlaWdodD0iNiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cGF0aCBkPSJNNSAwTDEgM0w1IDZMMiA2TDYgM0wxMSA2TDcgNkwxIDN6IiBmaWxsPSIjNDA5NmZmIi8+PC9zdmc+");
    background-repeat: no-repeat;
    background-position: right 1rem center;
    padding-right: 2.5rem;
  }

  .search-box {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    min-width: 300px;
  }

  .search-input-group {
    display: flex;
    gap: 12px;
    width: 100%;
  }

  .custom-input {
    flex: 1;
  }

  .search-button {
    background: linear-gradient(to right, @primary-base, @primary-medium);
    color: white;
    padding: 12px 24px;
    border: none;
    border-radius: @border-radius;
    cursor: pointer;
    font-size: 1em;
    font-weight: bold;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(64, 150, 255, 0.3);
    }

    &:active {
      transform: translateY(0);
    }
  }

  .filter-stats {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 15px;
    padding: 0 5px;
    font-size: 0.95em;
    color: @text-secondary;

    .highlight {
      color: @primary-medium;
      font-weight: 600;
      font-size: 1em;
    }

    .clear-filters {
      background: transparent;
      border: none;
      color: @primary-medium;
      cursor: pointer;
      font-size: 0.95em;
      padding: 5px 10px;
      border-radius: 4px;
      transition: all 0.3s ease;

      &:hover {
        color: @primary-dark;
        background-color: @primary-glass;
      }
    }
  }
}

// 课程卡片网格
.course-cards-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
  padding: 0 20px 40px;
  justify-content: center;
  align-items: stretch;

  .no-results {
    text-align: center;
    font-size: 1.3em;
    color: @text-secondary;
    font-weight: 500;
    grid-column: 1 / -1;
    margin-top: 60px;
    opacity: 0.85;
  }
}

// 响应式调整
@media (max-width: 768px) {
  .page-header {
    padding: 30px 15px;

    .page-title {
      font-size: 2rem;
    }
  }

  .filter-container {
    flex-direction: column;
    align-items: stretch;
    padding: 20px 15px;
    gap: 20px;
  }

  .search-box {
    min-width: auto;
    width: 100%;
  }

  .search-input-group {
    flex-direction: column;
  }

  .search-button {
    width: 100%;
  }

  .course-cards-grid {
    grid-template-columns: 1fr;
    padding: 0 15px 30px;
  }
}
</style>