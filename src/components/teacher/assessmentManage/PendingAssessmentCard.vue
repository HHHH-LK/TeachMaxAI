<template>
  <el-card class="assessment-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <h3>{{ assessment.title }}</h3>
        <el-tag type="warning" size="small">待阅卷</el-tag>
      </div>
    </template>

    <div class="card-content">
      <div class="info-row">
        <span class="label">待阅卷数量:</span>
        <span class="value">{{ assessment.totalStudents - assessment.gradedCount }}</span>
      </div>
      <div class="info-row">
        <span class="label">提交时间:</span>
        <span class="value">{{ assessment.lastSubmissionTime }}</span>
      </div>
    </div>

    <template #footer>
      <div class="card-actions">
        <el-button
          size="small"
          type="success"
          @click="$emit('grade', assessment)"
        >
          开始智能阅卷
        </el-button>
        <el-button size="small" @click="$emit('view-submissions', assessment)">
          查看提交
        </el-button>
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

const emit = defineEmits(['grade', 'view-submissions']);
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