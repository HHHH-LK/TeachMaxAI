<template>
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
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="submitCourseForm">创建课程</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['close', 'submit']);

const dialogVisible = ref(false);
const courseFormRef = ref(null);

// 课程表单数据a
const courseForm = reactive({
  title: '',
  time: ''
});

// 表单验证规则
const rules = reactive({
  title: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 2, max: 50, message: '课程名称长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  time: [
    { required: true, message: '请输入课程时间', trigger: 'blur' },
    { pattern: /^\d{4}[春夏秋冬]$/, message: '请输入正确的时间格式，例如：2025春', trigger: 'blur' }
  ]
});

// 监听visible属性变化
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal;
  if (newVal && courseFormRef.value) {
    courseFormRef.value.resetFields();
  }
});

// 监听对话框状态变化
watch(dialogVisible, (newVal) => {
  if (!newVal) {
    emit('close');
  }
});

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false;
};

// 提交表单创建课程
const submitCourseForm = async () => {
  try {
    await courseFormRef.value.validate();
    emit('submit', { ...courseForm });
    dialogVisible.value = false;
    ElMessage.success(`课程《${courseForm.title}》创建成功！`);
  } catch (error) {
    console.log('表单验证失败', error);
  }
};
</script>

<style lang="less" scoped>
@dialog-bg-color: rgba(255, 255, 255, 0.9);
@dialog-border-color: rgba(230, 230, 230, 0.8);
@text-primary: #1e293b;
@text-secondary: #64748b;
@accent-gradient: linear-gradient(90deg, #66b5fa 0%, #5c8df6 100%);
@primary-color: #bde3ff;

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
</style>
