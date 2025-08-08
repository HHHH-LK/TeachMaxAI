<template>
  <div class="homework-stats-container">
    <!-- 统计概览 -->
    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <h3>{{ totalHomework }}</h3>
          <p>总作业数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <h3>{{ totalStudents }}</h3>
          <p>学生总数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon><Check /></el-icon>
        </div>
        <div class="stat-content">
          <h3>{{ averageScore }}</h3>
          <p>平均分</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <h3>{{ submissionRate }}%</h3>
          <p>提交率</p>
        </div>
      </div>
    </div>

    <!-- 作业完成情况 -->
    <div class="stats-section">
      <h3>作业完成情况</h3>
      <div class="homework-progress">
        <div v-for="homework in homeworkStats" :key="homework.id" class="progress-item">
          <div class="progress-header">
            <span class="homework-name">{{ homework.title }}</span>
            <span class="progress-percentage">{{ homework.submissionRate }}%</span>
          </div>
          <el-progress
              :percentage="homework.submissionRate"
              :color="getProgressColor(homework.submissionRate)"
              :stroke-width="8"
          ></el-progress>
          <div class="progress-details">
            <span>已提交: {{ homework.submittedCount }}/{{ homework.totalStudents }}</span>
            <span>平均分: {{ homework.averageScore }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 成绩分布 -->
    <div class="stats-section">
      <h3>成绩分布</h3>
      <div class="grade-distribution">
        <div class="grade-chart">
          <div class="grade-bar">
            <span class="grade-label">90-100</span>
            <div class="bar-container">
              <div class="bar" :style="{ width: gradeDistribution.excellent + '%' }"></div>
            </div>
            <span class="grade-count">{{ gradeDistribution.excellent }}%</span>
          </div>
          <div class="grade-bar">
            <span class="grade-label">80-89</span>
            <div class="bar-container">
              <div class="bar" :style="{ width: gradeDistribution.good + '%' }"></div>
            </div>
            <span class="grade-count">{{ gradeDistribution.good }}%</span>
          </div>
          <div class="grade-bar">
            <span class="grade-label">70-79</span>
            <div class="bar-container">
              <div class="bar" :style="{ width: gradeDistribution.average + '%' }"></div>
            </div>
            <span class="grade-count">{{ gradeDistribution.average }}%</span>
          </div>
          <div class="grade-bar">
            <span class="grade-label">60-69</span>
            <div class="bar-container">
              <div class="bar" :style="{ width: gradeDistribution.pass + '%' }"></div>
            </div>
            <span class="grade-count">{{ gradeDistribution.pass }}%</span>
          </div>
          <div class="grade-bar">
            <span class="grade-label">0-59</span>
            <div class="bar-container">
              <div class="bar" :style="{ width: gradeDistribution.fail + '%' }"></div>
            </div>
            <span class="grade-count">{{ gradeDistribution.fail }}%</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { Document, User, Check, Clock } from '@element-plus/icons-vue';

// 响应式数据
const totalHomework = ref(3);
const totalStudents = ref(7);
const averageScore = ref(86);
const submissionRate = ref(76.1);

// 作业统计数据
const homeworkStats = ref([
  {
    id: 1,
    title: '第一次作业',
    submissionRate: 71.5,
    submittedCount: 5,
    totalStudents: 7,
    averageScore: 85.6
  },
  {
    id: 2,
    title: '第二次作业',
    submissionRate: 57.1,
    submittedCount: 4,
    totalStudents: 7,
    averageScore: 100
  },
  {
    id: 3,
    title: '第三次作业',
    submissionRate: 85.7,
    submittedCount: 6,
    totalStudents: 7,
    averageScore: 71.4
  },
]);

// 成绩分布数据
const gradeDistribution = ref({
  excellent: 22,  // 90-100
  good: 38,      // 80-89
  average: 35,    // 70-79
  pass: 16,       // 60-69
  fail: 9        // 0-59
});

// 方法
const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#67C23A';
  if (percentage >= 80) return '#E6A23C';
  if (percentage >= 70) return '#F56C6C';
  return '#909399';
};
</script>

<style scoped lang="less">
@primary: #2563eb; /* 主蓝色 */
@primary-light: #3b82f6; /* 浅一点的蓝色 */
@primary-dark: #1d4ed8; /* 深一点的蓝色 */

.homework-stats-container {
  padding: 20px;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, @primary-dark, @primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.stat-content h3 {
  margin: 0 0 4px 0;
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
}

.stat-content p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.stats-section {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stats-section h3 {
  margin: 0 0 20px 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
}

.homework-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-item {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.homework-name {
  font-weight: 600;
  color: #2c3e50;
}

.progress-percentage {
  font-weight: 600;
  color: #409EFF;
}

.progress-details {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.grade-distribution {
  padding: 20px 0;
}

.grade-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.grade-bar {
  display: flex;
  align-items: center;
  gap: 16px;
}

.grade-label {
  width: 60px;
  font-weight: 600;
  color: #2c3e50;
}

.bar-container {
  flex: 1;
  height: 20px;
  background: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.bar {
  height: 100%;
  background: linear-gradient(90deg, @primary-light, @primary);
  border-radius: 10px;
  transition: width 0.3s ease;
}

.grade-count {
  width: 60px;
  text-align: right;
  font-weight: 600;
  color: #666;
}
</style> 