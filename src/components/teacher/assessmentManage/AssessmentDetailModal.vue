<template>
  <el-dialog v-model="visible" title="考核详情" width="800px">
    <div v-if="assessment" class="assessment-detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="考核标题">
          {{ assessment.title }}
        </el-descriptions-item>
        <el-descriptions-item label="考核类型">
          {{ assessment.type }}
        </el-descriptions-item>
        <el-descriptions-item label="题目数量">
          {{ assessment.totalQuestions }}
        </el-descriptions-item>
        <el-descriptions-item label="总分">
          {{ assessment.totalScore }}
        </el-descriptions-item>
        <el-descriptions-item label="已提交">
          {{ assessment.totalStudents }}/{{ assessment.totalStudents }}
        </el-descriptions-item>
        <el-descriptions-item label="已阅卷">
          {{ assessment.gradedCount }}/{{ assessment.totalStudents }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ assessment.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(assessment.status)">
            {{ getStatusText(assessment.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          label="涉及章节"
          v-if="assessment.chapters"
        >
          <div class="chapter-tags">
            <el-tag
              v-for="chapterId in assessment.chapters"
              :key="chapterId"
              type="info"
              size="small"
              style="margin-right: 8px; margin-bottom: 4px"
            >
              {{ getChapterName(chapterId) }}
            </el-tag>
          </div>
        </el-descriptions-item>
        <el-descriptions-item
          label="知识点"
          v-if="assessment.knowledgePoints"
        >
          <div class="knowledge-point-tags">
            <el-tag
              v-for="point in assessment.knowledgePoints"
              :key="point"
              type="success"
              size="small"
              style="margin-right: 8px; margin-bottom: 4px"
            >
              {{ point }}
            </el-tag>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  assessment: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update:modelValue']);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

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
    scheduled: "进行中",
    completed: "已完成",
  };
  return statusMap[status] || status;
};

// 获取课程章节
const getChapterName = (chapterId) => {
  const chapterNames = {
    1: "第一章：课程介绍",
    2: "第二章：基础知识",
    3: "第三章：核心概念",
    4: "第四章：实践应用",
    5: "第五章：项目实战",
  };
  return chapterNames[chapterId] || `第${chapterId}章`;
};
</script>

<style scoped>
.assessment-detail {
  padding: 20px 0;
}

.chapter-tags,
.knowledge-point-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style> 