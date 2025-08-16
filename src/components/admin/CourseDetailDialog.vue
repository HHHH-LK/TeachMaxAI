<template>
  <el-dialog
      v-model="dialogVisible"
      :title="currentCourse ? currentCourse.title : '课程详情'"
      :width="1000"
      append-to-body
      center
      lock-scroll
      modal
      :close-on-press-escape="false"
      :before-close="handleClose"
      custom-class="course-detail-dialog"
  >
    <div class="course-detail-content">
      <!-- 课程基本信息 -->
      <div class="course-info-section" v-if="isCourseValid">
        <div class="course-info-header">
          <div class="course-icon-lg" :style="currentCourse?.iconStyle">
            <el-icon :size="36"
            ><component :is="currentCourse?.icon"></component
            ></el-icon>
          </div>
          <div class="course-basic-info">
            <h2 class="course-detail-title">{{ currentCourse?.title }}</h2>
            <div class="course-detail-meta">
              <span
              ><el-icon :size="16"><Clock /></el-icon>
                {{ currentCourse?.time }}</span
              >
              <span
              ><el-icon :size="16"><Ship /></el-icon>
                {{ currentCourse?.date }}</span
              >
            </div>
          </div>

          <!--智能分配教师待实现-->
          <div class="teacher-assignment">
            <el-button
                type="primary"
                @click="handleAutoAssign"
                :loading="autoAssignLoading"
                :disabled="autoAssignDisabled"
            >
              智能分配教师
            </el-button>
          </div>
        </div>

        <div class="course-description">
          <h3>课程描述</h3>
          <p>{{ currentCourse?.description }}</p>
        </div>
      </div>

      <!-- 数据不完整提示 -->
      <div v-else class="course-error-section">
        <el-alert
            title="课程数据不完整"
            description="无法显示课程详情，请检查数据完整性"
            type="warning"
            show-icon
            :closable="false"
        />
      </div>

      <!-- 教师分配区域 -->
      <div class="teacher-assignment-section">
        <div class="section-header">
          <h3>教师分配</h3>
          <p>
            当前负责教师:
            <span class="current-teacher">
              {{
                currentCourse?.teacherList?.map((t) => t.name).join("、") ||
                "未分配"
              }}
            </span>
          </p>
        </div>

        <TeacherSelection
            :teachers="teachers"
            v-model:selected-teacher-ids="selectedTeacherIds"
        />

        <button
            class="assign-btn"
            @click="confirmAssignTeacher"
            :disabled="selectedTeacherIds.length === 0"
        >
          确认分配
        </button>
      </div>

      <!-- 章节管理区域 -->
      <div class="chapter-management-section">
        <ChapterManagement
            v-if="validCourseId"
            :course-id="validCourseId"
            @refresh="handleRefresh"
        />
        <div v-else class="no-course-selected">
          <el-empty description="课程数据不完整，无法显示章节管理" />
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref, watch, computed} from "vue";
import {Clock, Ship} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import TeacherSelection from "./TeacherSelection.vue";
import ChapterManagement from "./ChapterManagement.vue";
import {adminService, teacherService} from "@/services/api";

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  currentCourse: {
    type: Object,
    default: null,
  },
  teachers: {
    type: Array,
    default: () => [],
  },
});

// 在这里添加自定义事件 auto-assign
const emit = defineEmits(["close", "assign-teachers", "auto-assign"]);

const dialogVisible = ref(false);
const selectedTeacherIds = ref([]);

// 添加智能分配教师的状态变量
const autoAssignLoading = ref(false); // 加载状态
const autoAssignDisabled = computed(() => {
  // 禁用条件：数据无效或正在加载
  return !isCourseValid.value || autoAssignLoading.value;
});

// 计算属性：验证课程数据是否完整
const isCourseValid = computed(() => {
  return (
      props.currentCourse &&
      props.currentCourse.id &&
      props.currentCourse.title
  );
});

// 计算属性：获取有效的课程ID
const validCourseId = computed(() => {
  return isCourseValid.value ? props.currentCourse.id : null;
});

// 监听visible属性变化
watch(
    () => props.visible,
    (newVal) => {
      dialogVisible.value = newVal;
      if (newVal && props.currentCourse) {
        // 验证课程数据完整性
        if (!props.currentCourse.id) {
          console.error("课程缺少ID:", props.currentCourse);
          ElMessage.error("课程数据不完整，无法显示详情");
          return;
        }

        // 初始化选中的教师ID
        if (
            props.currentCourse.teacherList &&
            props.currentCourse.teacherList.length > 0
        ) {
          selectedTeacherIds.value = props.currentCourse.teacherList.map(
              (teacher) => teacher.id
          );
        } else {
          selectedTeacherIds.value = [];
        }

        console.log("课程详情对话框已打开，课程数据:", props.currentCourse);
      }
    }
);

// 监听对话框状态变化
watch(dialogVisible, (newVal) => {
  if (!newVal) {
    emit("close");
  }
});

// 处理智能分配教师
const handleAutoAssign = async () => {
  if (!isCourseValid.value) {
    ElMessage.error('课程数据不完整，无法分配教师');
    return;
  }

  autoAssignLoading.value = true;

  try {
    // 触发自定义事件，传递课程ID给父组件
    emit("auto-assign", props.currentCourse.id);

    ElMessage.success('正在智能分配教师，请稍候...');
  } catch (error) {
    console.error('智能分配失败:', error);
    ElMessage.error('智能分配教师失败，请重试');
  } finally {
    autoAssignLoading.value = false;
  }
};

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false;
};

// 确认分配教师
const confirmAssignTeacher = async () => {
  if (selectedTeacherIds.value.length === 0) {
    ElMessage.error('请选择至少一名教师');
    return;
  }

  // 获取选中的教师对象列表
  const selectedTeachers = props.teachers.filter((t) =>
      selectedTeacherIds.value.includes(t.id)
  );

  if (selectedTeachers.length > 0) {
    try {
      // 调用API更新课程的教师信息
      for (const teacher of selectedTeachers) {
        await adminService.changeTeacher(props.currentCourse.id, teacher.id);
      }

      ElMessage.success(`已成功将${props.currentCourse.title}分配给${selectedTeachers
          .map((t) => t.name)
          .join("、")}`);

      // 通知父组件教师分配完成
      emit("assign-teachers", selectedTeachers);

      // 清空选中的教师ID
      selectedTeacherIds.value = [];
    } catch (error) {
      console.error('教师分配失败:', error);
      ElMessage.error('教师分配失败，请重试');
    }
  }
};

// 处理刷新
const handleRefresh = () => {
  // 可以在这里添加刷新逻辑
  console.log("章节或知识点数据已更新");
};
</script>

<style lang="less" scoped>
@dialog-bg-color: rgba(255, 255, 255, 0.9);
@dialog-border-color: rgba(230, 230, 230, 0.8);
@text-primary: #1e293b;
@text-secondary: #64748b;
@accent-gradient: linear-gradient(90deg, #66b5fa 0%, #5c8df6 100%);
@primary-color: #bde3ff;

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
      max-height: 80vh;
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

  .course-error-section {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid @dialog-border-color;

    .el-alert {
      border-radius: 12px;
      background-color: #fffbe6;
      border: 1px solid #ffe58f;
      color: #faad14;
      font-size: 15px;
      font-weight: 500;
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 8px;

      .el-alert__icon {
        font-size: 20px;
      }
    }
  }

  .teacher-assignment-section {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid @dialog-border-color;

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

  .chapter-management-section {
    h3 {
      font-size: 18px;
      font-weight: 600;
      margin-bottom: 20px;
      color: @text-primary;
    }
  }
}
</style>