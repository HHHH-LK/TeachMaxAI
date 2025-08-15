<template>
  <div class="course-selection-container">
    <div class="course">
      <div class="course-header">
        <h1>全部课程</h1>
      </div>
      <div v-if="courses.length === 0" class="no-courses-info">
        <p>暂无课程信息</p>
      </div>
      <div v-else>
        <div class="course-list">
          <div
            v-for="(course, index) in paginatedCourses"
            :key="course.id"
            class="course-card-wrapper"
            :data-index="index"
            @mousemove="handleMouseMove($event, index)"
            @mouseenter="handleMouseEnter(index)"
            @mouseleave="handleMouseLeave(index)"
          >
            <div class="course-card" :class="cardClasses(index)">
              <div class="card-content">
                <h3>{{ course.name }}</h3>
                <p>教师：{{ course.teacher }}</p>
                <p>学期：{{ course.credits }}</p>
                <div class="button">
                  <select-button
                    :isSelected="course.selected"
                    @click="selectCourse(course)"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="course-page">
        <Pagination
          :total="totalPages"
          :current-page="currentPage"
          @page-change="handlePageChange"
        />
      </div>
    </div>
    <div class="selected-courses">
      <h2>已选课程</h2>
      <div v-if="selectedCourses.length === 0" class="no-courses">
        <p>暂无已选课程</p>
      </div>
      <ul v-else>
        <li
          v-for="course in selectedCourses"
          :key="course.id"
          class="selected-course-item"
        >
          <div class="course-info">
            <span class="course-name">{{ course.name }}</span>
            <span class="course-teacher">- {{ course.teacher }}</span>
          </div>
          <button class="unselect-btn" @click="unselectCourse(course)">
            退选
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import Pagination from "@/components/student/course/Pagination.vue";
import SelectButton from "@/components/student/course/SelectButton.vue";
import { ElMessage } from "element-plus";
import { studentService } from "@/services/api";

// 课程数据
const courses = ref([]);
const selectedCourses = ref([]);
const mouseStates = ref([]);

// 分页相关
const currentPage = ref(1);
const pageSize = ref(4); // 每页显示4门课程
const totalPages = computed(() =>
  Math.ceil(courses.value.length / pageSize.value)
);
const paginatedCourses = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return courses.value.slice(start, end);
});

// 卡片尺寸（宽300px，高360px）
const size = [300, 360];
const [w, h] = size;

// 初始化鼠标状态
const initMouseStates = () => {
  mouseStates.value = paginatedCourses.value.map(() => false);
};

const cardClasses = (index) => {
  const classes = ["container"];
  if (index === 1) classes.push("container--reverse");
  if (index === 2) classes.push("container--special");
  if (mouseStates.value[index]) classes.push("container--active");
  return classes;
};

const handleMouseEnter = (index) => {
  mouseStates.value[index] = true;
};

const handleMouseLeave = (index) => {
  mouseStates.value[index] = false;
  const cards = document.querySelectorAll(".course-card");
  cards[index].style.setProperty("--rY", 0);
  cards[index].style.setProperty("--rX", 0);
  cards[index].style.setProperty("--bY", "30%");
  cards[index].style.setProperty("--bX", "30%");
};

const handleMouseMove = (event, index) => {
  if (!mouseStates.value[index]) return;

  const cardEl = event.currentTarget.querySelector(".course-card");
  const rect = event.currentTarget.getBoundingClientRect();
  const offsetX = event.clientX - rect.left;
  const offsetY = event.clientY - rect.top;

  let X, Y;
  if (index === 1) {
    // Reverse效果
    X = (offsetX - w / 2) / 4 / 4;
    Y = -(offsetY - h / 2) / 4 / 4;
  } else {
    // Normal效果
    X = -(offsetX - w / 2) / 4 / 4;
    Y = (offsetY - h / 2) / 4 / 4;
  }

  cardEl.style.setProperty("--rY", X.toFixed(2));
  cardEl.style.setProperty("--rX", Y.toFixed(2));
  cardEl.style.setProperty("--bY", `${80 - (X / 4).toFixed(2)}%`);
  cardEl.style.setProperty("--bX", `${50 - (Y / 4).toFixed(2)}%`);
};

// 处理分页变化
const handlePageChange = (newPage) => {
  currentPage.value = newPage;
  initMouseStates();
};

const selectCourse = async (course) => {
  try {
    if (!course.selected) {
      // 选课操作
      const response = await studentService.selectCourse(course.id);

      if (response.data.success) {
        // 更新前端状态
        course.selected = true;
        selectedCourses.value.push(course);

        // 提示用户
        ElMessage.success(`成功选择课程: ${course.name}`);
      } else {
        ElMessage.error("选课失败");
      }
    } else {
      // 退选操作
      await unselectCourse(course);
    }
  } catch (error) {
    // 处理错误
    ElMessage.error(`选择课程失败: ${error.message || "未知错误"}`);
  }
};

const unselectCourse = async (course) => {
  try {
    const response = await studentService.unselectCourse(course.id);
    console.log(response);

    if (response.data.success) {
      // 更新前端状态
      course.selected = false;
      // 从已选课程列表中移除
      const index = selectedCourses.value.findIndex((c) => c.id === course.id);
      if (index > -1) {
        selectedCourses.value.splice(index, 1);
      }

      // 提示用户
      ElMessage.success(`成功退选课程: ${course.name}`);
    } else {
      ElMessage.error("退选失败");
    }
  } catch (error) {
    // 处理错误
    ElMessage.error(`退选课程失败: ${error.message || "未知错误"}`);
  }
};

// 获取课程列表
const fetchCourses = async () => {
  try {
    const response = await studentService.getCourses();
    const allCourse = [];
    // console.log(response.data);
    if (response.data) {
      courses.value = response.data.data.reduce((acc, item) => {
        // 检查教师名称是否有效
        if (item.teacher?.user?.realName != null) {
          acc.push({
            id: item.courseId,
            name: item.courseName,
            teacher: item.teacher.user.realName,
            credits: item.semester,
            selected: false,
          });
        }
        return acc;
      }, []);
    }
    const responseSelect = await studentService.getOwnCourses();
    if (responseSelect.data) {
      const selectedCourseIds = new Set(
        responseSelect.data.data.map((course) =>
          String(course.id || course.courseId)
        )
      );

      selectedCourses.value = responseSelect.data.data.map((item) => ({
        id: item.courseId || item.id,
        name: item.courseName,
        teacher: item.teacherName,
      }));

      // 遍历并更新courses的选中状态
      courses.value = courses.value.map((course) => ({
        ...course,
        selected: selectedCourseIds.has(String(course.id)),
      }));
    }
    // console.log(response);
  } catch (error) {
    console.error("获取数据失败", error);
  }
  initMouseStates();
};

onMounted(() => {
  fetchCourses();
});
</script>

<style lang="less" scoped>
// 使用您定义的颜色变量
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

/* 暂无课程信息样式 */
.no-courses-info {
  text-align: center;
  padding: 60px 20px;
  color: @text-secondary;
  font-size: 1.2rem;
  background-color: @bg-white;
  border-radius: @radius-md;
  box-shadow: @shadow-sm;
  margin: 20px 0;
}

.no-courses-info p {
  margin: 0;
  font-weight: 500;
}

.disabled {
  opacity: 0.6;
  pointer-events: none;
  cursor: not-allowed;
}

.course-selection-container {
  background-color: @bg-light;
  padding: 24px;
  border-radius: @radius-md;
}

.course-header {
  text-align: center;
  margin-top: 10px;
  color: @primary-dark;
  font-size: 1.3rem;
  font-weight: 600;
  padding: 16px;
  background: linear-gradient(135deg, @primary-bg, @bg-white);
  border-radius: @radius-md;
  box-shadow: @shadow-sm;
  margin-bottom: 24px;
}

.course-page {
  cursor: pointer;
  margin-bottom: 5px;
}

.course {
  border: 1px solid @primary-border;
  border-radius: @radius-lg;
  box-shadow: @shadow-md;
  background-color: @bg-white;
  padding: 20px;
  margin-bottom: 24px;
}

.course-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  margin-bottom: 10px;
  padding-top: 2rem;
  justify-items: center;
}

.course-card-wrapper {
  position: relative;
  width: 280px;
  height: 340px;
  transform-style: preserve-3d;
  transform: perspective(100rem);
  cursor: pointer;
}

/* 视差卡片样式 */
.course-card {
  --rX: 0;
  --rY: 0;
  --bX: 50%;
  --bY: 80%;

  width: 100%;
  height: 100%;
  border: 1px solid rgba(66, 165, 245, 0.2);
  border-radius: @radius-lg;
  padding: 24px;
  position: relative;

  transform: rotateX(calc(var(--rX) * 1deg)) rotateY(calc(var(--rY) * 1deg));
  background: linear-gradient(135deg, @bg-white, @primary-bg);
  background-position: var(--bX) var(--bY);
  background-size: 40rem auto;
  box-shadow: @shadow-sm;
  transition: transform 0.6s 1s;
}

.course-card::before,
.course-card::after {
  content: "";
  width: 2rem;
  height: 2rem;
  border: 2px solid @primary;
  position: absolute;
  z-index: 2;
  opacity: 0.3;
  transition: @transition;
}

.course-card::before {
  top: 1.5rem;
  right: 1.5rem;
  border-bottom-width: 0;
  border-left-width: 0;
}

.course-card::after {
  bottom: 1.5rem;
  left: 1.5rem;
  border-top-width: 0;
  border-right-width: 0;
}

.container--reverse {
  background: linear-gradient(135deg, @primary-bg, rgba(59, 130, 246, 0.1));
}

.container--special {
  background: linear-gradient(
    135deg,
    rgba(59, 130, 246, 0.1),
    rgba(37, 99, 235, 0.15)
  );
}

.container--active {
  transition: none;
}

.course-card-wrapper:hover .course-card::before,
.course-card-wrapper:hover .course-card::after {
  width: calc(100% - 3rem);
  height: calc(100% - 3rem);
}

/* 课程内容样式 */
.card-content {
  position: relative;
  z-index: 3;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.card-content h3 {
  margin-top: 0.5rem;
  color: @primary-dark;
  font-size: 1.5rem;
  font-weight: 600;
}

.card-content p {
  color: @text-secondary;
  font-size: 1rem;
  margin-bottom: 1rem;
}

/* 已选课程样式 */
.selected-courses {
  background-color: @bg-white;
  border-radius: @radius-md;
  padding: 25px;
  box-shadow: @shadow-sm;
  margin-top: 40px;
  border: 1px solid @primary-border;
}

.selected-courses h2 {
  color: @primary-dark;
  margin-bottom: 20px;
  font-size: 1.5rem;
  font-weight: 600;
  text-align: center;
}

.selected-courses ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.selected-courses li {
  background-color: @primary-bg;
  border: 1px solid @primary-border;
  padding: 12px 18px;
  margin-bottom: 10px;
  border-radius: @radius-sm;
  color: @primary-dark;
  font-size: 1.1rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: @transition;
}

.selected-courses li:hover {
  transform: translateX(8px);
  box-shadow: @shadow-sm;
  background-color: @primary-light;
  color: white;
}

.selected-course-item {
  background-color: @primary-bg;
  border: 1px solid @primary-border;
  padding: 12px 18px;
  margin-bottom: 10px;
  border-radius: @radius-sm;
  color: @primary-dark;
  font-size: 1.1rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: @transition;
}

.selected-course-item:hover {
  transform: translateX(8px);
  box-shadow: @shadow-sm;
  background-color: @primary-light;
  color: white;
}

.course-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-name {
  font-weight: 600;
  color: @primary-dark;
}

.course-teacher {
  color: @text-secondary;
  font-size: 0.9rem;
}

.unselect-btn {
  background-color: #ff69b4;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: @radius-sm;
  font-size: 0.9rem;
  cursor: pointer;
  transition: @transition;
}

.unselect-btn:hover {
  background-color: #ff1493;
  transform: scale(1.05);
}

.no-courses {
  text-align: center;
  padding: 40px 20px;
  color: @text-secondary;
  font-size: 1.1rem;
}

/* 分页容器样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin: 30px 0;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .course-list {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .course-selection-container {
    padding: 16px;
  }

  .course-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .course-card-wrapper {
    width: 100%;
    max-width: 320px;
  }
}
</style>
