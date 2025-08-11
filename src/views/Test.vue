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

      <main class="course-grid">
        <!-- 课程卡片 -->
        <div
            class="course-card"
            v-for="(course, index) in courses"
            :key="index"
        >
          <div class="card-header">
            <div class="card-icon-bg" :style="course.iconStyle">
              <el-icon :size="24"><component :is="course.icon"></component></el-icon>
            </div>
            <h3 class="card-title">{{ course.title }}</h3>
          </div>
          <div class="card-body">
            <div class="card-meta">
              <!-- 使用格式化后的教师名称 -->
              <span><el-icon :size="16"><User /></el-icon> {{ formatTeacherName(course.teacherList) }} </span>
              <span><el-icon :size="16"><Clock /></el-icon> {{ course.time }} </span>
            </div>
            <p class="card-description">{{ course.description }}</p>
          </div>
          <div class="card-footer">
            <div class="student-count">
              <el-icon :size="16"><Ship /></el-icon>
              <span>{{ course.date }}</span>
            </div>
            <a href="#" class="enter-course-btn" @click.prevent="handleEnterCourse(course, index)">进入课程</a>
          </div>
        </div>
      </main>
    </div>

    <!-- 创建课程对话框 -->
    <el-dialog
        v-model="dialogVisible"
        title="智能创建课程"
        :width="400"
        :before-close="handleClose"
        custom-class="create-course-dialog"
    >
      <el-form
          :model="courseForm"
          :rules="rules"
          ref="courseFormRef"
          label-width="80px"
      >
        <el-form-item label="课程名称" prop="title">
          <el-input
              v-model="courseForm.title"
              placeholder="请输入课程名称"
              clearable
          ></el-input>
        </el-form-item>
        <el-form-item label="课程时间" prop="time">
          <el-input
              v-model="courseForm.time"
              placeholder="例如：2025春"
              clearable
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCourseForm">创建课程</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 课程详情与教师选择对话框 -->
    <el-dialog
        v-model="courseDetailVisible"
        :title="currentCourse ? currentCourse.title : '课程详情'"
        :width="800"
        :before-close="handleCourseDetailClose"
        custom-class="course-detail-dialog"
    >
      <div class="course-detail-content">
        <!-- 课程基本信息 -->
        <div class="course-info-section">
          <div class="course-info-header">
            <div class="course-icon-lg" :style="currentCourse?.iconStyle">
              <el-icon :size="36"><component :is="currentCourse?.icon"></component></el-icon>
            </div>
            <div class="course-basic-info">
              <h2 class="course-detail-title">{{ currentCourse?.title }}</h2>
              <div class="course-detail-meta">
                <span><el-icon :size="16"><Clock /></el-icon> {{ currentCourse?.time }}</span>
                <span><el-icon :size="16"><Ship /></el-icon> {{ currentCourse?.date }}</span>
              </div>
            </div>

            <!--智能分配教师待实现-->
            <div class="teacher-assignment">
              <el-button type="primary">
                智能分配教师
              </el-button>
            </div>
          </div>

          <div class="course-description">
            <h3>课程描述</h3>
            <p>{{ currentCourse?.description }}</p>
          </div>
        </div>

        <!-- 教师分配区域 -->
        <div class="teacher-assignment-section">
          <div class="section-header">
            <h3>教师分配</h3>
            <p>当前负责教师:
              <span class="current-teacher">
                <!-- 在详情中显示完整列表 -->
                {{ currentCourse?.teacherList?.map(t => t.name).join('、') || '未分配' }}
              </span>
            </p>
          </div>

          <div class="teacher-selection">
            <h4>可选教师列表（可多选）</h4>
            <div class="teacher-grid">
              <div
                  class="teacher-card"
                  v-for="(teacher, tIndex) in teachers"
                  :key="tIndex"
                  @click="toggleTeacherSelection(teacher)"
                  :class="{ 'selected': isTeacherSelected(teacher.id) }"
              >
                <div class="teacher-avatar" :style="{ backgroundColor: teacher.avatarColor }">
                  {{ teacher.name.substring(0, 1) }}
                </div>
                <div class="teacher-info">
                  <h5 class="teacher-name">{{ teacher.name }}</h5>
                  <p class="teacher-subject">{{ teacher.subject }}</p>
                </div>
                <div class="teacher-select-indicator" v-if="isTeacherSelected(teacher.id)">
                  <el-icon><Check /></el-icon>
                </div>
              </div>
            </div>
          </div>

          <button
              class="assign-btn"
              @click="confirmAssignTeacher"
              :disabled="selectedTeacherIds.length === 0"
          >
            确认分配
          </button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {reactive, ref, markRaw} from 'vue';
import {
  CirclePlus,
  Document,
  PieChart,
  Histogram,
  User,
  Clock,
  Ship,
  Check
} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';

// 先定义教师数据，解决初始化顺序问题
const teachers = reactive([
  {
    id: 1,
    name: '王老师',
    subject: '前端开发、JavaScript',
    avatarColor: '#6366f1'
  },
  {
    id: 2,
    name: '李教授',
    subject: 'UI设计、交互设计',
    avatarColor: '#ec4899'
  },
  {
    id: 3,
    name: '张博士',
    subject: '数据分析、Python',
    avatarColor: '#06b6d4'
  },
  {
    id: 4,
    name: '赵讲师',
    subject: '后端开发、Java',
    avatarColor: '#8b5cf6'
  },
  {
    id: 5,
    name: '陈老师',
    subject: '移动开发、Flutter',
    avatarColor: '#f472b6'
  },
  {
    id: 6,
    name: '刘教授',
    subject: '人工智能、机器学习',
    avatarColor: '#22d3ee'
  }
]);

// 格式化教师名称的函数
const formatTeacherName = (teacherList) => {
  if (!teacherList || teacherList.length === 0) {
    return '未分配';
  }
  // 如果教师数量大于2，显示第一位教师+"等"
  if (teacherList.length > 2) {
    return `${teacherList[0].name}等`;
  }
  // 否则显示所有教师姓名
  return teacherList.map(teacher => teacher.name).join('、');
};

// 课程数据 - 现在可以安全地引用teachers数组了
const courses = reactive([
  {
    title: '前端开发高级实战',
    teacherList: [teachers.find(t => t.id === 1)], // 王老师
    time: '2025春',
    description: '深入Vue 3、React 18核心，构建企业级复杂应用。',
    date: '2025-07-16 14:30',
    icon: markRaw(Document),
    iconStyle: {
      background: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)'
    }
  },
  {
    title: 'UI/UX 设计从入门到精通',
    teacherList: [teachers.find(t => t.id === 2), teachers.find(t => t.id === 5)], // 李教授、陈老师
    time: '2025春',
    description: '掌握Figma、用户研究与交互设计，成为全链路设计师。',
    date: '2025-07-16 14:30',
    icon: markRaw(PieChart),
    iconStyle: {
      background: 'linear-gradient(135deg, #ec4899 0%, #f472b6 100%)'
    }
  },
  {
    title: '数据分析与可视化',
    teacherList: [teachers.find(t => t.id === 3), teachers.find(t => t.id === 4), teachers.find(t => t.id === 6)], // 张博士、赵讲师、刘教授
    time: '2025春',
    description: '使用Python进行数据处理，Matplotlib与Tableau可视化分析。',
    date: '2025-07-16 14:30',
    icon: markRaw(Histogram),
    iconStyle: {
      background: 'linear-gradient(135deg, #06b6d4 0%, #22d3ee 100%)'
    }
  }
]);

// 对话框相关
const dialogVisible = ref(false);
const courseDetailVisible = ref(false);
const courseFormRef = ref(null);
const currentCourse = ref(null);
const currentCourseIndex = ref(-1);
const selectedTeacherIds = ref([]);

// 课程表单数据
const courseForm = reactive({
  title: '',
  time: ''
});

// 表单验证规则
const rules = reactive({
  title: [
    {required: true, message: '请输入课程名称', trigger: 'blur'},
    {min: 2, max: 50, message: '课程名称长度在 2 到 50 个字符', trigger: 'blur'}
  ],
  time: [
    {required: true, message: '请输入课程时间', trigger: 'blur'},
    {pattern: /^\d{4}[春夏秋冬]$/, message: '请输入正确的时间格式，例如：2025春', trigger: 'blur'}
  ]
});

// 处理创建课程 - 显示对话框
const handleCreateCourse = () => {
  dialogVisible.value = true;
  if (courseFormRef.value) {
    courseFormRef.value.resetFields();
  }
};

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false;
};

// 提交表单创建课程
const submitCourseForm = async () => {
  try {
    await courseFormRef.value.validate();
    const newCourse = generateCourse(courseForm.title, courseForm.time);
    courses.unshift(newCourse);
    dialogVisible.value = false;
    ElMessage.success(`课程《${courseForm.title}》创建成功！`);
  } catch (error) {
    console.log('表单验证失败', error);
  }
};

// 智能生成课程信息
const generateCourse = (title, time) => {
  const icons = [
    {icon: markRaw(Document), style: 'background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)'},
    {icon: markRaw(PieChart), style: 'background: linear-gradient(135deg, #ec4899 0%, #f472b6 100%)'},
    {icon: markRaw(Histogram), style: 'background: linear-gradient(135deg, #06b6d4 0%, #22d3ee 100%)'}
  ];

  const randomIcon = icons[Math.floor(Math.random() * icons.length)];

  const teacherList = [];

  const randomDescription = `深入学习${title}核心知识，掌握实战技能。`;

  const today = new Date();
  const randomDays = Math.floor(Math.random() * 7) + 1;
  today.setDate(today.getDate() + randomDays);

  const formattedDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')} ${String(Math.floor(Math.random() * 6) + 14).padStart(2, '0')}:${Math.random() > 0.5 ? '30' : '00'}`;

  return {
    title,
    teacherList,
    time,
    description: randomDescription,
    date: formattedDate,
    icon: randomIcon.icon,
    iconStyle: randomIcon.style
  };
};

// 处理进入课程 - 打开课程详情对话框
const handleEnterCourse = (course, index) => {
  currentCourse.value = {...course};
  currentCourseIndex.value = index;

  // 初始化选中的教师ID
  if (course.teacherList && course.teacherList.length > 0) {
    selectedTeacherIds.value = course.teacherList.map(teacher => teacher.id);
  } else {
    selectedTeacherIds.value = [];
  }

  courseDetailVisible.value = true;
};

// 关闭课程详情对话框
const handleCourseDetailClose = () => {
  courseDetailVisible.value = false;
  selectedTeacherIds.value = [];
};

// 检查教师是否被选中
const isTeacherSelected = (teacherId) => {
  return selectedTeacherIds.value.includes(teacherId);
};

// 切换教师选择状态
const toggleTeacherSelection = (teacher) => {
  const index = selectedTeacherIds.value.indexOf(teacher.id);
  if (index > -1) {
    selectedTeacherIds.value.splice(index, 1);
  } else {
    selectedTeacherIds.value.push(teacher.id);
  }
};

// 确认分配教师
const confirmAssignTeacher = () => {
  if (selectedTeacherIds.value.length === 0 || currentCourseIndex.value === -1) return;

  // 获取选中的教师对象列表
  const selectedTeachers = teachers.filter(t => selectedTeacherIds.value.includes(t.id));

  if (selectedTeachers.length > 0) {
    // 更新课程的教师列表
    courses[currentCourseIndex.value].teacherList = selectedTeachers;
    currentCourse.value.teacherList = [...selectedTeachers];

    ElMessage({
      message: `已成功将${currentCourse.value.title}分配给${selectedTeachers.map(t => t.name).join('、')}`,
      type: 'success',
      duration: 2000
    });

    selectedTeacherIds.value = [];
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
@dialog-bg-color: rgba(255, 255, 255, 0.9);
@dialog-border-color: rgba(230, 230, 230, 0.8);
@primary-color: #bde3ff;

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

    // 课程卡片网格
    .course-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 28px;

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

        &:hover {
          transform: translateY(-10px) scale(1.02);
          box-shadow: 0 16px 40px 0 rgba(99, 102, 241, 0.12);
          border-color: rgba(255, 255, 255, 0.8);
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
    }
  }
}

// 创建课程对话框样式，保证浏览器兼容问题添加-webkit……
:deep(.create-course-dialog) {
  .el-dialog__wrapper {
    backdrop-filter: blur(5px);
    -webkit-backdrop-filter: blur(5px);
  }

  .el-dialog {
    border-radius: 16px;
    background-color: @dialog-bg-color;
    border: 1px solid @dialog-border-color;
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    box-shadow: 0 10px 30px -5px rgba(0, 0, 0, 0.08);
    overflow: hidden;

    .el-dialog__header {
      padding: 20px 24px;
      border-bottom: 1px solid @dialog-border-color;

      .el-dialog__title {
        font-size: 18px;
        font-weight: 600;
        color: @text-primary;
      }
    }

    .el-dialog__body {
      padding: 24px;

      .el-form {
        .el-form-item {
          margin-bottom: 20px;

          .el-form-item__label {
            color: @text-primary;
            font-weight: 500;
          }

          .el-input {
            .el-input__wrapper {
              border-radius: 8px;
              background-color: rgba(255, 255, 255, 0.85);
              border-color: rgba(200, 200, 200, 0.5);

              &:focus-within {
                box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
                border-color: @primary-color;
              }
            }
          }
        }
      }
    }

    .el-dialog__footer {
      padding: 16px 24px;
      border-top: 1px solid @dialog-border-color;

      .el-button {
        padding: 8px 16px;
        border-radius: 8px;
        font-weight: 500;

        &:first-child {
          background-color: transparent;
          color: @text-secondary;
          border-color: @dialog-border-color;

          &:hover {
            background-color: rgba(240, 240, 240, 0.5);
          }
        }

        &:last-child {
          background: @accent-gradient;
          border-color: transparent;

          &:hover {
            opacity: 0.9;
          }
        }
      }
    }
  }
}

// 课程详情对话框样式
:deep(.course-detail-dialog) {
  .el-dialog__wrapper {
    backdrop-filter: blur(5px);
    -webkit-backdrop-filter: blur(5px);
  }

  .el-dialog {
    border-radius: 16px;
    background-color: @dialog-bg-color;
    border: 1px solid @dialog-border-color;
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    box-shadow: 0 10px 30px -5px rgba(0, 0, 0, 0.08);
    overflow: hidden;

    .el-dialog__header {
      padding: 24px;
      border-bottom: 1px solid @dialog-border-color;

      .el-dialog__title {
        font-size: 22px;
        font-weight: 700;
        color: @text-primary;
      }
    }

    .el-dialog__body {
      padding: 0;
      max-height: 70vh;
      overflow-y: auto;
    }

    .el-dialog__footer {
      padding: 16px 24px;
      border-top: 1px solid @dialog-border-color;
    }
  }
}

// 课程详情内容样式
.course-detail-content {
  padding: 24px;

  .course-info-section {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid @dialog-border-color;

    .course-info-header {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 24px;

      .course-icon-lg {
        width: 64px;
        height: 64px;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        flex-shrink: 0;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
      }

      .course-basic-info {
        flex-grow: 1;

        .course-detail-title {
          font-size: 24px;
          font-weight: 700;
          margin-bottom: 8px;
        }

        .course-detail-meta {
          display: flex;
          gap: 24px;
          color: @text-secondary;
          font-size: 14px;

          span {
            display: flex;
            align-items: center;
            gap: 6px;
          }
        }
      }
    }

    .course-description {
      h3 {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 12px;
        color: @text-primary;
      }

      p {
        font-size: 15px;
        color: @text-secondary;
        line-height: 1.8;
      }
    }
  }

  .teacher-assignment-section {
    .section-header {
      margin-bottom: 24px;

      h3 {
        font-size: 18px;
        font-weight: 600;
        margin-bottom: 8px;
        color: @text-primary;
      }

      p {
        color: @text-secondary;
        font-size: 14px;

        .current-teacher {
          color: @primary-color;
          font-weight: 500;
        }
      }
    }

    .teacher-selection {
      margin-bottom: 24px;

      h4 {
        font-size: 16px;
        font-weight: 500;
        margin-bottom: 16px;
        color: @text-primary;
      }

      .teacher-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;
        margin-bottom: 24px;
      }

      .teacher-card {
        background: rgba(255, 255, 255, 0.85);
        border-radius: 12px;
        padding: 16px;
        cursor: pointer;
        transition: all 0.3s ease;
        border: 2px solid transparent;
        position: relative;

        &:hover {
          transform: translateY(-5px);
          box-shadow: 0 8px 15px rgba(99, 102, 241, 0.08);
          border-color: rgba(99, 102, 241, 0.2);
        }

        &.selected {
          border-color: @primary-color;
          background: rgba(99, 102, 241, 0.08);
          box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
        }

        .teacher-avatar {
          width: 48px;
          height: 48px;
          border-radius: 50%;
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 20px;
          font-weight: 600;
          margin-bottom: 12px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .teacher-info {
          .teacher-name {
            font-weight: 600;
            margin-bottom: 4px;
            color: @text-primary;
          }

          .teacher-subject {
            font-size: 13px;
            color: @text-secondary;
          }
        }

        .teacher-select-indicator {
          position: absolute;
          top: 12px;
          right: 12px;
          width: 24px;
          height: 24px;
          border-radius: 50%;
          background: @primary-color;
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
        }
      }
    }

    .assign-btn {
      background: @accent-gradient;
      color: white;
      border: none;
      border-radius: 8px;
      padding: 10px 24px;
      font-size: 15px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.3s ease;
      display: block;
      margin-left: auto;
      box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);

      &:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
      }

      &:disabled {
        background: #ccc;
        cursor: not-allowed;
        transform: none;
        box-shadow: none;
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
