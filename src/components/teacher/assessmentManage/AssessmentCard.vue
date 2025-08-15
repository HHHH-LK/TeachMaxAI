<template>
  <el-card class="assessment-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <h3>{{ assessment.title }}</h3>
        <div class="tags">
          <el-tag
            :type="getTypeTagType(assessment.type)"
            size="small"
          >
            {{ assessment.type }}
          </el-tag>
          <el-tag
            :type="getStatusTagType(assessment.status)"
            size="small"
          >
            {{ getStatusText(assessment.status) }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="card-content">
      <div class="info-row">
        <span class="label">题目数量:</span>
        <span class="value">{{ assessment.totalQuestions }}</span>
      </div>
      <div class="info-row">
        <span class="label">总分:</span>
        <span class="value">{{ assessment.totalScore }}</span>
      </div>
      <div class="info-row">
        <span class="label">已提交:</span>
        <span class="value">{{ assessment.submittedCount }}/{{ assessment.totalStudents }}</span>
      </div>
      <div class="info-row">
        <span class="label">已阅卷:</span>
        <span class="value">{{ assessment.gradedCount }}/{{ assessment.submittedCount }}</span>
      </div>
    </div>

    <template #footer>
      <div class="card-actions">
        <el-button size="small" @click="$emit('view', assessment)">查看</el-button>
        <el-button
          size="small"
          type="info"
          @click="$emit('view-exam', assessment)"
        >考试内容</el-button>
        <el-button
          size="small"
          type="success"
          @click="$emit('grade', assessment)"
          v-if="assessment.submittedCount > 0"
        >
          智能阅卷
        </el-button>
        <el-button
          size="small"
          type="primary"
          v-if="assessment.status === 'draft'"
          @click="$emit('publish', assessment)"
        >发布</el-button>
        <el-button
          size="small"
          type="danger"
          @click="$emit('delete', assessment.id)"
        >删除</el-button>
      </div>
    </template>
  </el-card>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  assessment: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['view', 'view-exam', 'grade', 'delete', 'publish']);

// 考试类型标签颜色
const getTypeTagType = (type) => {
  const typeMap = {
    期中考试: "primary",
    期末考试: "danger",
    单元测验: "warning",
    实验报告: "success",
    随堂测试: "info",
  };
  return typeMap[type] || "info";
};

// 考试状态标签颜色
const getStatusTagType = (status) => {
  const statusMap = {
    draft: "info",
    scheduled: "success",
    completed: "warning",
  };
  return statusMap[status] || "info";
};

// 考试状态转中文
const getStatusText = (status) => {
  const statusMap = {
    draft: "草稿",
    active: "进行中",
    completed: "已完成",
  };
  return statusMap[status] || status;
};
</script>

<style scoped>
.assessment-card {
  transition: all 0.3s ease;
}

.assessment-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.tags {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.card-content {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.label {
  color: #909399;
  font-size: 14px;
}

.value {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .card-actions {
    justify-content: center;
  }
}
</style> 