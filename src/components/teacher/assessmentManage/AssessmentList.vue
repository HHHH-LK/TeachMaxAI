<template>
  <div class="assessment-list">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="全部考核" name="all">
        <div class="assessment-grid">
          <AssessmentCard
            v-for="assessment in assessments"
            :key="assessment.id"
            :assessment="assessment"
            @view="$emit('view', $event)"
            @view-exam="$emit('view-exam', $event)"
            @grade="$emit('grade', $event)"
            @delete="$emit('delete', $event)"
            @publish="$emit('publish', $event)"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="待阅卷" name="pending">
        <div class="assessment-grid">
          <PendingAssessmentCard
            v-for="assessment in pendingAssessments"
            :key="assessment.id"
            :assessment="assessment"
            @grade="$emit('grade', $event)"
            @view-submissions="$emit('view-submissions', $event)"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import AssessmentCard from './AssessmentCard.vue';
import PendingAssessmentCard from './PendingAssessmentCard.vue';

const props = defineProps({
  assessments: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(['view', 'view-exam', 'grade', 'delete', 'view-submissions', 'publish']);

const activeTab = ref('all');

// 计算待阅卷的考核
const pendingAssessments = computed(() => {
  console.log("assessment",props.assessments)
  return props.assessments.filter(
    // (a) => a.submittedCount > 0 && a.gradedCount < a.submittedCount
    a => {
    return a.submittedCount > 0;
  }
  );
});
</script>

<style scoped>
.assessment-list {
  margin-top: 20px;
}

.assessment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .assessment-grid {
    grid-template-columns: 1fr;
  }
}
</style> 