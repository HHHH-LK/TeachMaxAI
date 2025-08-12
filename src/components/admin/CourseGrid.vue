<template>
  <main class="course-grid">
    <!-- 课程卡片容器 -->
    <div class="course-cards-container">
      <CourseCard
        v-for="(course, index) in paginatedCourses"
        :key="course.id || index"
        :course="course"
        @enter-course="handleEnterCourse"
      />
    </div>
    
    <!-- 课程列表分页 -->
    <div class="course-pagination" v-if="courses.length > 0">
      <el-pagination
        v-model:current-page="currentCoursePage"
        v-model:page-size="coursePageSize"
        :page-sizes="[6, 9, 12]"
        :total="courses.length"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleCoursePageSizeChange"
        @current-change="handleCoursePageChange"
        background
      />
    </div>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue';
import CourseCard from './CourseCard.vue';

const props = defineProps({
  courses: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['enter-course']);

// 课程分页相关
const currentCoursePage = ref(1);
const coursePageSize = ref(9);

// 分页后的课程列表
const paginatedCourses = computed(() => {
  const start = (currentCoursePage.value - 1) * coursePageSize.value;
  const end = start + coursePageSize.value;
  return props.courses.slice(start, end);
});

// 处理进入课程
const handleEnterCourse = (course) => {
  emit('enter-course', course);
};

// 处理课程分页大小改变
const handleCoursePageSizeChange = (newSize) => {
  coursePageSize.value = newSize;
  currentCoursePage.value = 1; // 重置到第一页
};

// 处理课程分页改变
const handleCoursePageChange = (newPage) => {
  currentCoursePage.value = newPage;
};
</script>

<style lang="less" scoped>
@text-secondary: #64748b;
@primary-color: #bde3ff;

// 课程卡片网格
.course-grid {
  display: flex;
  flex-direction: column;
  gap: 28px;

  .course-cards-container {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 28px;
  }

  // 课程分页样式
  .course-pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid rgba(230, 230, 230, 0.5);

    :deep(.el-pagination) {
      .el-pagination__total {
        color: @text-secondary;
        font-size: 14px;
      }

      .el-pagination__sizes {
        .el-select {
          .el-input__wrapper {
            border-radius: 6px;
            border-color: rgba(200, 200, 200, 0.5);
          }
        }
      }

      .el-pager {
        .el-pager__item {
          border-radius: 6px;
          margin: 0 2px;
          
          &.is-active {
            background: @primary-color;
            color: white;
          }
        }
      }

      .btn-prev,
      .btn-next {
        border-radius: 6px;
        border-color: rgba(200, 200, 200, 0.5);
      }
    }
  }
}
</style>
