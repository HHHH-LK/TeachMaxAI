<template>
  <div class="course-card">
    <!-- 删除按钮 -->
    <div class="delete-btn-wrapper">
      <button class="delete-btn" @click.stop="handleDelete" title="删除课程">
        <el-icon :size="16"><Delete /></el-icon>
      </button>
    </div>
    
    <div class="card-header">
      <div class="card-icon-bg" :style="course.iconStyle">
        <el-icon :size="24"><component :is="course.icon"></component></el-icon>
      </div>
      <h3 class="card-title">{{ course.title }}</h3>
    </div>
    <div class="card-body">
      <div class="card-meta">
        <span><el-icon :size="16"><User /></el-icon> {{ formatTeacherName(course.teacherList) }}</span>
        <span><el-icon :size="16"><Clock /></el-icon> {{ course.time }}</span>
      </div>
      <p class="card-description">{{ course.description }}</p>
    </div>
    <div class="card-footer">
      <div class="student-count">
        <el-icon :size="16"><Ship /></el-icon>
        <span>{{ course.date }}</span>
      </div>
      <a href="#" class="enter-course-btn" @click.prevent="handleEnterCourse">进入课程</a>
    </div>
  </div>
</template>

<script setup>
import { User, Clock, Ship, Delete } from '@element-plus/icons-vue';

const props = defineProps({
  course: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['enter-course', 'delete-course']);

// 格式化教师名称
const formatTeacherName = (teacherList) => {
  if (!teacherList || teacherList.length === 0) {
    return '未分配';
  }
  if (teacherList.length > 2) {
    return `${teacherList[0].name}等`;
  }
  return teacherList.map(teacher => teacher.name).join('、');
};

// 处理进入课程
const handleEnterCourse = () => {
  emit('enter-course', props.course);
};

// 处理删除课程
const handleDelete = () => {
  emit('delete-course', props.course);
};
</script>

<style lang="less" scoped>
@card-bg-color: rgba(255, 255, 255, 0.75);
@card-border-color: rgba(240, 240, 240, 0.8);
@text-primary: #1e293b;
@text-secondary: #64748b;
@accent-gradient: linear-gradient(90deg, #66b5fa 0%, #5c8df6 100%);
@danger-color: #ef4444;

.course-card {
  background: @card-bg-color;
  border: 1px solid @card-border-color;
  border-radius: 20px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px 0 rgba(99, 102, 241, 0.05);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;

  &:hover {
    transform: translateY(-10px) scale(1.02);
    box-shadow: 0 16px 40px 0 rgba(99, 102, 241, 0.12);
    border-color: rgba(255, 255, 255, 0.8);
    
    .delete-btn {
      opacity: 1;
      transform: scale(1);
    }
  }

  // 删除按钮样式
  .delete-btn-wrapper {
    position: absolute;
    top: 12px;
    right: 12px;
    z-index: 10;
  }

  .delete-btn {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: rgba(239, 68, 68, 0.9);
    border: none;
    color: white;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    opacity: 0;
    transform: scale(0.8);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);

    &:hover {
      background: rgba(239, 68, 68, 1);
      transform: scale(1.1);
      box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
    }

    &:active {
      transform: scale(0.95);
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 24px;

    .card-icon-bg {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
    }

    .card-title {
      font-size: 20px;
      font-weight: 700;
    }
  }

  .card-body {
    padding: 0 24px 24px;
    flex-grow: 1;

    .card-meta {
      display: flex;
      gap: 20px;
      font-size: 14px;
      color: @text-secondary;
      margin-bottom: 16px;

      span {
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }

    .card-description {
      font-size: 14px;
      line-height: 1.7;
      color: @text-secondary;
    }
  }

  .card-footer {
    padding: 16px 24px;
    border-top: 1px solid @card-border-color;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .student-count {
      font-size: 14px;
      color: @text-secondary;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .enter-course-btn {
      background: @accent-gradient;
      color: #fff;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.3s ease;
      box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);

      &:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
      }
    }
  }
}
</style>
