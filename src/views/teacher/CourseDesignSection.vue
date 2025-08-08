<template>
  <div class="course-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <el-icon><Reading /></el-icon>
          课程管理中心
        </h1>
        <p class="page-subtitle">管理您的所有课程，创建精彩的教学内容</p>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <div class="search-filter-row">
        <div class="search-box">
          <el-icon><Search /></el-icon>
          <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索课程名称或描述..."
              class="search-input"
          >
        </div>

        <div class="filter-box">
          <el-icon><Calendar /></el-icon>
          <select v-model="timeFilter" class="filter-select">
            <option value="">全部学期</option>
            <option value="2025春">2025春季</option>
            <option value="2024秋">2024秋季</option>
            <option value="2024春">2024春季</option>
            <option value="2023秋">2023秋季</option>
          </select>
        </div>

        <div class="course-count">
          <div class="count-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="count-text">
            <span class="count-number">{{ filteredCourses.length }}</span>
            <span class="count-label">门课程</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 课程卡片网格 -->
    <div class="courses-grid">
      <div
          v-for="course in filteredCourses"
          :key="course.id"
          class="course-card"
          @click="selectCourse(course)"
      >
        <div class="card-header">
        </div>

        <div class="card-content">
          <div class="course-icon">
            <el-icon><Reading /></el-icon>
          </div>

          <h3 class="course-title">{{ course.name }}</h3>
          <!-- <p class="course-description">{{ course.description }}</p> -->
        </div>

        <div class="card-footer">
          <div class="course-meta">
            <!-- <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ course.studentCount }} 名学生</span>
            </div> -->
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatDate(course.semester) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="filteredCourses.length === 0" class="empty-state">
      <div class="empty-icon">
        <el-icon><Document /></el-icon>
      </div>
      <h3>暂无课程</h3>
      <p>当前没有找到符合条件的课程</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Reading,
  Search,
  Calendar,
  Collection,
  User,
  Document
} from '@element-plus/icons-vue'
import { teacherService } from '@/services/api'

const teacherId = "1";

// 路由实例
const router = useRouter()

// 响应式数据
const searchQuery = ref('')
const timeFilter = ref('')

const fetchAllClass = async () => {
  const response = await teacherService.getAllCourse(teacherId);
  if(response.data){
    courses.value =  response.data.data.map(item => ({
      id: item.courseId.toString(),
      name: item.courseName,
      semester: item.semester}))
  }
}


const courses = ref([]);
// 模拟课程数据
// const courses = ref([
//   {
//     id: 1,
//     name: '高等数学基础',
//     description: '深入讲解微积分、线性代数等数学基础知识，为后续专业课程打下坚实基础。',
//     studentCount: 156,
//     semester: '2025春'
//   },
//   {
//     id: 2,
//     name: '数据结构与算法实验',
//     description: '通过实践项目学习数据结构与算法的实际应用，提升编程能力。',
//     studentCount: 89,
//     semester: '2024秋'
//   },
//   {
//     id: 3,
//     name: '软件工程研讨',
//     description: '探讨现代软件工程方法，包括敏捷开发、测试驱动开发等最佳实践。',
//     studentCount: 0,
//     semester: '2025春'
//   },
//   {
//     id: 4,
//     name: 'Web开发技术',
//     description: '学习现代Web开发技术栈，包括前端框架、后端API设计和数据库设计。',
//     studentCount: 234,
//     semester: '2024春'
//   },
//   {
//     id: 5,
//     name: '人工智能导论',
//     description: '介绍人工智能的基本概念、算法和应用，包括机器学习和深度学习。',
//     studentCount: 178,
//     semester: '2024秋'
//   },
//   {
//     id: 6,
//     name: '数据库系统实验',
//     description: '通过实际项目学习数据库设计、SQL编程和数据库优化技术。',
//     studentCount: 67,
//     semester: '2023秋'
//   }
// ])

// 计算属性
const filteredCourses = computed(() => {
  let filtered = courses.value

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(course =>
            course.name.toLowerCase().includes(query)
        // course.description.toLowerCase().includes(query)
    )
  }

  // 时间过滤
  if (timeFilter.value) {
    filtered = filtered.filter(course => course.semester === timeFilter.value)
  }

  return filtered
})

// 方法
const formatDate = (semester) => {
  return semester
}

const selectCourse = (course) => {
  console.log('选择课程:', course)
  // 跳转到课程中心页面
  router.push({
    name: 'TeacherCourseCenter',
    params: { courseId: course.id }
  })
}

onMounted(() => {
  fetchAllClass();
})
</script>

<style lang="less">
// 蓝色主题色变量
@primary-blue: #1a56db;
@secondary-blue: #0c2d6b;
@light-blue: #3482f7;
@blue-bg: #f0f6ff;
@card-bg: #ffffff;
@text-dark: #1a202c;
@text-gray: #718096;
@light-gray: #e2e8f0;
@border-color: rgba(226, 232, 240, 0.8);
@hover-blue: rgba(26, 86, 219, 0.1);
@count-bg: #f8fafc;

.course-management {
  min-height: 100vh;
  background: @blue-bg;
  padding: 3rem 2rem;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  position: relative;
  overflow-x: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: radial-gradient(circle at 20% 80%, rgba(26, 86, 219, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(12, 45, 107, 0.1) 0%, transparent 50%);
    pointer-events: none;
    z-index: 1;
  }

  > * {
    position: relative;
    z-index: 2;
  }
}

// 页面头部
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3rem;
  padding: 2rem 2.5rem;
  background: @card-bg;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid @border-color;

  .header-content {
    .page-title {
      font-size: 2.2rem;
      font-weight: 700;
      color: @text-dark;
      margin: 0;
      display: flex;
      align-items: center;
      gap: 1rem;
      letter-spacing: -0.01em;

      .el-icon {
        font-size: 1.8rem;
        color: @primary-blue;
      }
    }

    .page-subtitle {
      color: @text-gray;
      margin: 0.5rem 0 0 0;
      font-size: 1rem;
      font-weight: 500;
    }
  }
}

// 搜索和筛选区域
.search-filter-section {
  background: @card-bg;
  border-radius: 16px;
  padding: 2rem;
  margin-bottom: 2.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid @border-color;

  .search-filter-row {
    display: flex;
    gap: 1.5rem;
    align-items: center;
    flex-wrap: wrap;
  }

  .search-box {
    position: relative;
    flex: 1;

    .el-icon {
      position: absolute;
      left: 1rem;
      top: 50%;
      transform: translateY(-50%);
      color: #a0aec0;
      font-size: 1.2rem;
      z-index: 2;
    }
  }

  .filter-box {
    position: relative;
    min-width: 200px;

    .el-icon {
      position: absolute;
      left: 1rem;
      top: 50%;
      transform: translateY(-50%);
      color: #a0aec0;
      font-size: 1.2rem;
      z-index: 2;
      pointer-events: none;
    }
  }

  .course-count {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem 1.25rem;
    background: @count-bg;
    border: 1px solid @light-gray;
    border-radius: 12px;
    min-width: 140px;
    transition: all 0.2s ease;

    &:hover {
      background: #f1f5f9;
      border-color: #cbd5e0;
    }

    .count-icon {
      width: 36px;
      height: 36px;
      background: @primary-blue;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 1rem;

      .el-icon {
        font-size: 1.2rem;
        color: white;
      }
    }

    .count-text {
      display: flex;
      flex-direction: column;
      line-height: 1;

      .count-number {
        font-size: 1.25rem;
        font-weight: 700;
        color: @text-dark;
      }

      .count-label {
        font-size: 0.8rem;
        color: @text-gray;
        font-weight: 500;
      }
    }
  }
}

.search-input,
.filter-select {
  width: 100%;
  padding: 1rem 1rem 1rem 3rem;
  border: 1px solid @light-gray;
  border-radius: 12px;
  font-size: 1rem;
  transition: all 0.2s ease;
  background: @count-bg;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;

  &:focus {
    outline: none;
    border-color: @primary-blue;
    box-shadow: 0 0 0 3px @hover-blue;
    background: white;
  }
}

.filter-select {
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='m6 8 4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 1rem center;
  background-repeat: no-repeat;
  background-size: 1.5em 1.5em;
  padding-right: 3rem;

  &:hover {
    border-color: #cbd5e0;
    background-color: #f1f5f9;
  }
}

// 课程卡片网格
.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 2rem;
  margin-bottom: 2rem;
}

.course-card {
  background: @card-bg;
  border-radius: 16px;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  border: 1px solid @border-color;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: @primary-blue;
    transform: scaleX(0);
    transition: transform 0.2s ease;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    border-color: @primary-blue;

    &::before {
      transform: scaleX(1);
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
  }

  .card-content {
    text-align: center;
    margin-bottom: 1.5rem;

    .course-icon {
      width: 60px;
      height: 60px;
      margin: 0 auto 1rem;
      background: linear-gradient(135deg, @primary-blue 0%, @light-blue 100%);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.6rem;
      color: white;

      .el-icon {
        font-size: 1.8rem;
      }
    }

    .course-title {
      font-size: 1.2rem;
      font-weight: 700;
      color: @text-dark;
      margin: 0 0 0.5rem 0;
      line-height: 1.4;
    }

    .course-description {
      color: @text-gray;
      font-size: 0.9rem;
      line-height: 1.5;
      margin: 0 0 1rem 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .card-footer {
    border-top: 1px solid @light-gray;
    padding-top: 1rem;
    margin-top: 1rem;

    .course-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: @text-gray;
        font-size: 0.85rem;
        font-weight: 500;

        .el-icon {
          color: @primary-blue;
          font-size: 0.9rem;
        }
      }
    }
  }
}

// 空状态
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: @card-bg;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  border: 1px solid @border-color;

  .empty-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 1.5rem;
    background: linear-gradient(135deg, @primary-blue 0%, @light-blue 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2rem;
    color: white;

    .el-icon {
      font-size: 2.5rem;
    }
  }

  h3 {
    font-size: 1.5rem;
    font-weight: 700;
    color: @text-dark;
    margin: 0 0 0.5rem 0;
  }

  p {
    color: @text-gray;
    margin: 0;
    font-size: 1rem;
    font-weight: 500;
  }
}

// Element Plus 图标样式
.el-icon {
  vertical-align: middle;
}

// 响应式设计
@media (max-width: 768px) {
  .course-management {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    gap: 1rem;
    text-align: center;
    padding: 1.5rem;
  }

  .search-filter-section {
    padding: 1.5rem;

    .search-filter-row {
      flex-direction: column;
      gap: 1rem;
    }

    .filter-box {
      min-width: auto;
      width: 100%;
    }

    .course-count {
      min-width: auto;
      justify-content: center;
      width: 100%;
    }
  }

  .courses-grid {
    grid-template-columns: 1fr;
  }
}
</style>