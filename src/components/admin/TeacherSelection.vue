<template>
  <div class="teacher-selection">
    <h4>可选教师列表（可多选）</h4>
    <div class="teacher-grid">
      <div
        class="teacher-card"
        v-for="(teacher, tIndex) in paginatedTeachers"
        :key="teacher.id"
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
    
    <!-- 教师列表分页 -->
    <div class="teacher-pagination" v-if="teachers.length > 0">
      <el-pagination
        v-model:current-page="currentTeacherPage"
        v-model:page-size="pageSize"
        :page-sizes="[6, 12, 18]"
        :total="teachers.length"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleTeacherPageSizeChange"
        @current-change="handleTeacherPageChange"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { Check } from '@element-plus/icons-vue';

const props = defineProps({
  teachers: {
    type: Array,
    default: () => []
  },
  selectedTeacherIds: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:selectedTeacherIds']);

// 教师分页相关数据
const currentTeacherPage = ref(1);
const pageSize = ref(6);

// 分页后的教师列表
const paginatedTeachers = computed(() => {
  const start = (currentTeacherPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return props.teachers.slice(start, end);
});

// 检查教师是否被选中
const isTeacherSelected = (teacherId) => {
  return props.selectedTeacherIds.includes(teacherId);
};

// 切换教师选择状态
const toggleTeacherSelection = (teacher) => {
  const newSelectedIds = [...props.selectedTeacherIds];
  const index = newSelectedIds.indexOf(teacher.id);
  
  if (index > -1) {
    newSelectedIds.splice(index, 1);
  } else {
    newSelectedIds.push(teacher.id);
  }
  
  emit('update:selectedTeacherIds', newSelectedIds);
};

// 处理教师分页大小改变
const handleTeacherPageSizeChange = (newSize) => {
  pageSize.value = newSize;
  currentTeacherPage.value = 1; // 重置到第一页
};

// 处理教师分页改变
const handleTeacherPageChange = (newPage) => {
  currentTeacherPage.value = newPage;
};
</script>

<style lang="less" scoped>
@text-primary: #1e293b;
@text-secondary: #64748b;
@primary-color: #bde3ff;

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

  .teacher-pagination {
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
