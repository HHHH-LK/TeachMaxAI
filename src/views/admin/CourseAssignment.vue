<template>
  <div class="page-wrapper">
    <!-- 动态光晕背景 -->
    <div class="background-aurora">
      <div class="aurora-shape shape-1"></div>
      <div class="aurora-shape shape-2"></div>
      <div class="aurora-shape shape-3"></div>
    </div>

    <div class="page-container">
      <header class="page-header">
        <h1 class="page-title">课程中心</h1>
        <button class="create-course-btn" @click="handleCreateCourse">
          <el-icon><CirclePlus /></el-icon>
          <span>智能创建课程</span>
        </button>
      </header>

    </div>

    <!-- 创建课程对话框 -->

    <!-- 课程详情与教师选择对话框 -->
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus';



// 对话框相关
const dialogVisible = ref(false);
const courseDetailVisible = ref(false);
const currentCourse = ref(null);
const currentCourseIndex = ref(-1);

// 处理创建课程 - 显示对话框
const handleCreateCourse = () => {
  dialogVisible.value = true;
};

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false;
};

};

// 处理进入课程 - 打开课程详情对话框
  currentCourse.value = { ...course };
  

  courseDetailVisible.value = true;
};

// 关闭课程详情对话框
const handleCourseDetailClose = () => {
  courseDetailVisible.value = false;
};


  // 更新课程的教师列表
  currentCourse.value.teacherList = [...selectedTeachers];
    
    
  }
};
</script>

<style lang="less" scoped>
// 变量定义
@card-bg-color: rgba(255, 255, 255, 0.75);
@card-border-color: rgba(240, 240, 240, 0.8);
@text-primary: #1e293b;
@text-secondary: #64748b;
@accent-gradient: linear-gradient(90deg, #66b5fa 0%, #5c8df6 100%);

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.page-wrapper {
  font-family: 'Noto Sans SC', sans-serif;
  background-color: #f8fafc;
  color: @text-primary;
  line-height: 1.6;
  overflow: hidden;
  position: relative;
  min-height: 100vh;

  // 动态光晕背景，确保浏览器兼容问题
  .background-aurora {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    filter: blur(100px) saturate(130%);
    -webkit-filter: blur(100px) saturate(130%);
    z-index: 0;

    .aurora-shape {
      position: absolute;
      border-radius: 50%;
      opacity: 0.5;
      transition: all 0.5s ease;
    }

    .shape-1 {
      width: 500px;
      height: 500px;
      background-color: #6366f1;
      top: -150px;
      left: -150px;
      animation: move 15s infinite alternate ease-in-out;
      animation-delay: 0s;
    }

    .shape-2 {
      width: 450px;
      height: 450px;
      background-color: #ec4899;
      bottom: 350px;
      right: 150px;
      animation: move 20s infinite alternate ease-in-out;
      animation-delay: 2s;
    }

    .shape-3 {
      width: 400px;
      height: 350px;
      background-color: #06b6d4;
      top: 50%;
      right: 40%;
      animation: move 18s infinite alternate ease-in-out;
      animation-delay: 1s;
    }
  }

  // 页面容器与头部
  .page-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px 20px;
    position: relative;
    z-index: 1;

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 40px;

      .page-title {
        font-size: 32px;
        font-weight: 700;
        color: @text-primary;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
      }

      .create-course-btn {
        background: @card-bg-color;
        border: 1px solid @card-border-color;
        color: @text-primary;
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
        padding: 12px 24px;
        border-radius: 12px;
        font-size: 16px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        gap: 8px;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);

        &:hover {
          transform: translateY(-3px);
          box-shadow: 0 8px 20px rgba(99, 102, 241, 0.15);
          background: rgba(255, 255, 255, 0.9);
        }
      }
    }
  }
}

// 动画
@keyframes move {
  0% {
    transform: translate(0, 0) rotate(0deg);
    opacity: 0.4;
    scale: 1;
  }
  50% {
    opacity: 0.6;
  }
  100% {
    transform: translate(400px, 200px) rotate(30deg);
    opacity: 0.5;
    scale: 1.2;
  }
}

:deep(.el-message) {
  border-radius: 8px;
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  background-color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
