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
        <h1 class="page-title">课程分配中心</h1>
        <div class="header-actions">
          <button class="refresh-btn" @click="loadData" :disabled="loading">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </button>
          <button class="create-course-btn" @click="handleCreateCourse">
            <el-icon><CirclePlus /></el-icon>
            <span>智能创建课程</span>
          </button>
        </div>
      </header>

      <!-- 课程网格 -->
      <CourseGrid
          v-if="!loading"
          :courses="courses"
          @enter-course="handleEnterCourse"
          @delete-course="handleDeleteCourse"
      />

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && courses.length === 0" class="empty-state">
        <el-empty description="暂无课程数据" />
      </div>
    </div>

    <!-- 创建课程对话框 -->
    <CreateCourseDialog
        :visible="dialogVisible"
        @close="handleClose"
        @submit="handleCreateCourseSubmit"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
    />

    <!-- 课程详情与教师选择对话框 -->
    <CourseDetailDialog
        :visible="courseDetailVisible"
        :current-course="currentCourse"
        :teachers="teachers"
        @close="handleCourseDetailClose"
        @assign-teachers="handleAssignTeachers"
        @auto-assign="handleAutoAssign"
    />

    <!-- 删除确认对话框 -->
    <el-dialog
        v-model="deleteDialogVisible"
        title="确认删除课程"
        width="400px"
        append-to-body
        center
        lock-scroll
        modal
        :close-on-press-escape="false"
    >
      <div class="delete-confirm-content">
        <el-icon :size="48" color="#ef4444"><Warning /></el-icon>
        <p class="delete-message">
          确定要删除课程《<strong>{{ courseToDelete?.title }}</strong>》吗？
        </p>
        <p class="delete-warning">此操作不可撤销，删除后将无法恢复！</p>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="deleteDialogVisible = false" size="large">
            取消
          </el-button>
          <el-button
              type="danger"
              @click="confirmDeleteCourse"
              size="large"
              :loading="deleteLoading"
          >
            确认删除
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { CirclePlus, Refresh, Warning } from '@element-plus/icons-vue';
import {
  CourseGrid,
  CreateCourseDialog,
  CourseDetailDialog,
  generateCourse,
  processCourseData,
  processTeacherData
} from '@/components/admin';
import {adminService} from "@/services/api.js";

// 课程数据
const courses = ref([]);
const teachers = ref([]);
const loading = ref(false);

// 对话框相关
const dialogVisible = ref(false);
const courseDetailVisible = ref(false);
const currentCourse = ref(null);
const currentCourseIndex = ref(-1);

// 删除相关
const deleteDialogVisible = ref(false);
const courseToDelete = ref(null);
const deleteLoading = ref(false);

// 创建课程加载状态
const createCourseLoading = ref(false);

// 模拟课程数据
const mockCourses = [
  {
    id: 1,
    title: '高等数学',
    time: '2025春',
    description: '深入学习高等数学核心知识，掌握微积分、线性代数等基础理论。',
    date: '2025-01-15 14:30',
    teacherList: [],
    status: 'active'
  },
  {
    id: 2,
    title: '计算机程序设计',
    time: '2025春',
    description: '系统学习编程基础理论和应用方法，掌握多种编程语言。',
    date: '2025-01-16 09:00',
    teacherList: [],
    status: 'active'
  },
  {
    id: 3,
    title: '数据结构与算法',
    time: '2025春',
    description: '通过理论与实践相结合的方式学习数据结构与算法设计。',
    date: '2025-01-17 15:30',
    teacherList: [],
    status: 'active'
  }
];

// 模拟教师数据
const mockTeachers = [
  { id: 1, name: '张教授', subject: '数学系', avatarColor: '#6366f1' },
  { id: 2, name: '李老师', subject: '计算机系', avatarColor: '#ec4899' },
  { id: 3, name: '王博士', subject: '软件工程', avatarColor: '#06b6d4' },
  { id: 4, name: '陈教授', subject: '人工智能', avatarColor: '#8b5cf6' }
];

// 初始化数据
onMounted(async () => {
  await loadData();
});

// 加载数据函数
const loadData = async () => {
  loading.value = true;
  try {
    // 获取所有课程数据
    const coursesResponse = await adminService.getAllCourses();
    if (coursesResponse.data.code === 0) {
      const rawCourses = coursesResponse.data.data;
      console.log('原始课程数据:', rawCourses);
      // 使用 processCourseData 处理课程数据
      courses.value = processCourseData(rawCourses);
      console.log('处理后的课程数据:', courses.value);

      // 验证课程ID是否正确设置
      courses.value.forEach((course, index) => {
        if (!course.id) {
          console.warn(`课程 ${index} 缺少ID:`, course);
        }
      });
    } else {
      console.error('获取课程失败:', coursesResponse.data.message);
      ElMessage.error('获取课程数据失败');
    }

    // 获取所有用户数据
    const usersResponse = await adminService.getAllUsers();
    if (usersResponse.data.code === 0) {
      const rawUsers = usersResponse.data.data;
      // 过滤出教师用户
      const teacherUsers = rawUsers.filter(user => user.teacher);
      // 使用 processTeacherData 处理教师数据
      teachers.value = processTeacherData(teacherUsers);
      console.log('教师数据加载成功:', teachers.value);
    } else {
      console.error('获取用户失败:', usersResponse.data.message);
      ElMessage.error('获取用户数据失败');
    }
  } catch (error) {
    console.error('数据加载失败:', error);
    ElMessage.error('数据加载失败，请检查网络连接');

    // 如果API调用失败，使用模拟数据作为备用
    courses.value = processCourseData(mockCourses);
    teachers.value = processTeacherData(mockTeachers);
  } finally {
    loading.value = false;
  }
};

// 处理创建课程 - 显示对话框
const handleCreateCourse = () => {
  dialogVisible.value = true;
};

// 关闭对话框
const handleClose = () => {
  if (!createCourseLoading.value) {
    dialogVisible.value = false;
  }
};

const handleCreateCourseSubmit = async (courseData) => {
  try {
    // 显示加载状态
    createCourseLoading.value = true;

    // 调用真实API接口
    const response = await adminService.createCourse({
      title: courseData.title,
      time: courseData.time
    });

    if(response.data.data != null){
      ElMessage.success(response.data.data );


      dialogVisible.value = false;

      await loadData();
    } else {
      ElMessage.error(response.data.msg || '创建课程失败');
    }
  } catch (error) {
    console.error('创建课程失败:', error);

    const errorMsg = error.response?.data?.msg || '创建课程失败，请重试';
    ElMessage.error(errorMsg);

  } finally {
    createCourseLoading.value = false;
  }
};

// 处理进入课程 - 打开课程详情对话框
const handleEnterCourse = (course) => {
  // 确保课程数据完整
  if (!course) {
    ElMessage.error('课程数据不存在');
    return;
  }

  if (!course.id) {
    ElMessage.error('课程ID不存在，无法打开详情');
    console.error('课程缺少ID:', course);
    return;
  }

  if (!course.title) {
    ElMessage.error('课程标题不存在');
    return;
  }

  console.log('准备打开课程详情:', course);

  currentCourse.value = { ...course };
  currentCourseIndex.value = courses.value.findIndex(c => c.id === course.id);
  courseDetailVisible.value = true;
};

// 关闭课程详情对话框
const handleCourseDetailClose = () => {
  courseDetailVisible.value = false;
  currentCourse.value = null;
  currentCourseIndex.value = -1;
};

// 处理分配教师
const handleAssignTeachers = (selectedTeachers) => {
  if (currentCourseIndex.value !== -1) {
    // 更新课程的教师列表
    currentCourse.value.teacherList = [...selectedTeachers];
    courses.value[currentCourseIndex.value].teacherList = [...selectedTeachers];

    ElMessage.success('教师分配成功！');
  }
};

// 处理删除课程
const handleDeleteCourse = (course) => {
  if (!course) {
    ElMessage.error('课程数据不存在');
    return;
  }
  if (!course.id) {
    ElMessage.error('课程ID不存在，无法删除');
    return;
  }
  courseToDelete.value = course;
  deleteDialogVisible.value = true;
};

// 确认删除课程
const confirmDeleteCourse = async () => {
  if (!courseToDelete.value) return;

  deleteLoading.value = true;
  try {
    // 调用API删除课程
    const response = await adminService.deleteCourse(courseToDelete.value.id);

    if (response.data.code === 0) {
      // 从本地数据中删除
      const index = courses.value.findIndex(c => c.id === courseToDelete.value.id);
      if (index !== -1) {
        courses.value.splice(index, 1);
        ElMessage.success('课程删除成功！');
      }
    } else {
      ElMessage.error(response.data.message || '删除课程失败');
    }
  } catch (error) {
    console.error('删除课程失败:', error);
    ElMessage.error('删除课程失败，请检查网络连接');
  } finally {
    deleteLoading.value = false;
    deleteDialogVisible.value = false;
    courseToDelete.value = null;
  }
};

const handleAutoAssign = async (course) => {
  try {
    console.log('开始智能分配教师，课程:', course);

    if (!course) {
      ElMessage.error('找不到课程信息');
      return;
    }


    console.log(`调用智能分配API，课程ID: ${course}`);
    const response = await adminService.autoAssignTeachers(course);
    console.log('API响应:', response);

    if (!response) {
      throw new Error('API调用返回空响应');
    }

    if (response.data.code === 0) {
      ElMessage.success(response.data.data || '教师分配成功');

      await loadData();

      // 更新当前课程对象
      const updatedCourse = courses.value.find(c => c.id === course); // 使用course.id
      if (updatedCourse) {
        if (currentCourse.value?.id === course) { // 使用course.id
          currentCourse.value = {...updatedCourse};
        }
      }
    } else {
      // 显示后端返回的错误消息
      const errorMsg = response.data.msg || '未找到合适的教师分配';
      console.warn('API返回错误:', errorMsg);
      ElMessage.warning(errorMsg);
    }
  } catch (error) {
    console.error('智能分配教师失败:', error);
    // 添加错误消息显示
    ElMessage.error('智能分配教师失败: ' + (error.message || '未知错误'));
  }
};


// 处理编辑课程
const handleEditCourse = (course) => {
  // 这里可以实现编辑课程的逻辑
  console.log('编辑课程:', course);
  ElMessage.info('编辑课程功能待实现');
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

      .header-actions {
        display: flex;
        gap: 15px;

        .refresh-btn {
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

          &:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            background: @card-bg-color;
            border-color: @card-border-color;
            color: @text-secondary;
          }
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
}

// 加载状态和空状态样式
.loading-container {
  padding: 40px;
  background: @card-bg-color;
  border-radius: 20px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid @card-border-color;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  background: @card-bg-color;
  border-radius: 20px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid @card-border-color;
}

// 删除确认对话框样式
:deep(.el-dialog) {
  border-radius: 16px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);

  .el-dialog__header {
    border-bottom: 1px solid rgba(240, 240, 240, 0.8);
    padding: 20px 24px;

    .el-dialog__title {
      font-size: 18px;
      font-weight: 600;
      color: @text-primary;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    border-top: 1px solid rgba(240, 240, 240, 0.8);
    padding: 20px 24px;
  }
}

.delete-confirm-content {
  text-align: center;
  padding: 20px 0;

  .el-icon {
    margin-bottom: 16px;
  }

  .delete-message {
    font-size: 16px;
    color: @text-primary;
    margin-bottom: 12px;
    line-height: 1.5;

    strong {
      color: #ef4444;
    }
  }

  .delete-warning {
    font-size: 14px;
    color: #f59e0b;
    margin: 0;
    line-height: 1.4;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .el-button {
    border-radius: 8px;
    font-weight: 500;

    &.el-button--danger {
      background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
      border: none;

      &:hover {
        background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
        transform: translateY(-1px);
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
