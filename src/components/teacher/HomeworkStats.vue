<template>
  <div class="homework-stats-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-loading />
      <p>正在加载作业统计数据...</p>
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-icon size="48" color="#F56C6C"><Warning /></el-icon>
      <p>{{ error }}</p>
      <el-button @click="retryFetch" type="primary" size="small">重试</el-button>
    </div>
    
    <!-- 主要内容 -->
    <div v-else>
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
        <div v-if="homeworkStats.length === 0" class="empty-data">
          <p>暂无作业数据</p>
        </div>
        <div v-else class="homework-progress">
          <div v-for="(homework, index) in homeworkStats" :key="homework.assignmentId" class="progress-item">
            <div class="progress-header">
              <div class="homework-title-section">
                <span class="homework-number">第{{ getChineseNumber(index + 1) }}次作业</span>
              </div>
              <span class="progress-percentage">{{ homework.submissionRate }}%</span>
            </div>
            <el-progress
                :percentage="homework.submissionRate"
                :color="getProgressColor(homework.submissionRate)"
                :stroke-width="8"
            ></el-progress>
            <div class="progress-details">
              <span>已提交: {{ homework.submissionCount }}/{{ totalStudents }}</span>
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
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { Document, User, Check, Clock, Warning } from '@element-plus/icons-vue';
import { teacherService } from "@/services/api.js";

// 定义 props
const props = defineProps({
  courseId: {
    type: [String, Number],
    required: true
  }
});

// 响应式数据
const loading = ref(false);
const error = ref('');
const totalHomework = ref(0);
const totalStudents = ref(0);
const averageScore = ref(0);
const submissionRate = ref(0);

// 作业统计数据
const homeworkStats = ref([]);

// 成绩分布数据
const gradeDistribution = ref({
  excellent: 0,  // 90-100
  good: 0,      // 80-89
  average: 0,    // 70-79
  pass: 0,       // 60-69
  fail: 0        // 0-59
});

// 获取作业统计数据
const fetchHomeworkStats = async (courseId) => {
  loading.value = true;
  error.value = '';
  
  try {
    const response = await teacherService.getStudentHomework(courseId);
    console.log('API响应:', response);
    
    if (response.data && response.data.success) {
      const data = response.data.data;
      
      // 更新概览数据
      totalHomework.value = data.totalAssignments || 0;
      totalStudents.value = data.totalStudents || 0;
      averageScore.value = data.overallAverageScore ? Math.round(data.overallAverageScore) : 0;
      submissionRate.value = data.overallSubmissionRate ? Math.round(data.overallSubmissionRate) : 0;
      
      // 更新作业列表数据
      homeworkStats.value = data.homeworkList || [];
      
      // 计算成绩分布
      calculateGradeDistribution();
    } else {
      error.value = response.data?.message || '获取数据失败';
    }
  } catch (err) {
    console.error('获取作业统计数据失败:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    loading.value = false;
  }
};

// 重试获取数据
const retryFetch = () => {
  if (props.courseId) {
    fetchHomeworkStats(props.courseId);
  }
};

// 计算成绩分布 - 基于每个作业的分数段数据
const calculateGradeDistribution = () => {
  let totalExcellent = 0, totalGood = 0, totalAverage = 0, totalPass = 0, totalFail = 0;
  let totalStudents = 0;
  
  homeworkStats.value.forEach(homework => {
    // 使用后端返回的分数段数据
    const scoreA = homework.scoreA || 0; // 90-100分
    const scoreB = homework.scoreB || 0; // 80-89分  
    const scoreC = homework.scoreC || 0; // 70-79分
    const scoreD = homework.scoreD || 0; // 60-69分
    const scoreF = homework.scoreF || 0; // 0-59分
    
    totalExcellent += scoreA;
    totalGood += scoreB;
    totalAverage += scoreC;
    totalPass += scoreD;
    totalFail += scoreF;
    
    // 累加总学生数（使用提交数量作为参考）
    totalStudents += homework.submissionCount || 0;
  });
  
  // 如果没有学生数据，使用作业数量来计算分布
  if (totalStudents === 0) {
    const total = homeworkStats.value.length || 1;
    gradeDistribution.value = {
      excellent: Math.round((totalExcellent / total) * 100),
      good: Math.round((totalGood / total) * 100),
      average: Math.round((totalAverage / total) * 100),
      pass: Math.round((totalPass / total) * 100),
      fail: Math.round((totalFail / total) * 100)
    };
  } else {
    // 使用实际学生数量计算百分比
    gradeDistribution.value = {
      excellent: Math.round((totalExcellent / totalStudents) * 100),
      good: Math.round((totalGood / totalStudents) * 100),
      average: Math.round((totalAverage / totalStudents) * 100),
      pass: Math.round((totalPass / totalStudents) * 100),
      fail: Math.round((totalFail / totalStudents) * 100)
    };
  }
};

// 方法
const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#67C23A';
  if (percentage >= 80) return '#E6A23C';
  if (percentage >= 70) return '#F56C6C';
  return '#909399';
};

// 获取中文数字
const getChineseNumber = (num) => {
  const chineseNumbers = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
  
  if (num <= 10) {
    return chineseNumbers[num - 1];
  } else if (num <= 19) {
    return '十' + (num > 10 ? chineseNumbers[num - 11] : '');
  } else if (num <= 99) {
    const tens = Math.floor(num / 10);
    const ones = num % 10;
    let result = chineseNumbers[tens - 1] + '十';
    if (ones > 0) {
      result += chineseNumbers[ones - 1];
    }
    return result;
  } else {
    return num.toString();
  }
};

// 监听 courseId 变化
watch(() => props.courseId, (newCourseId) => {
  if (newCourseId) {
    fetchHomeworkStats(newCourseId);
  }
}, { immediate: true });

// 组件挂载时获取数据
onMounted(() => {
  if (props.courseId) {
    fetchHomeworkStats(props.courseId);
  }
});
</script>

<style scoped lang="less">
@primary: #2563eb; /* 主蓝色 */
@primary-light: #3b82f6; /* 浅一点的蓝色 */
@primary-dark: #1d4ed8; /* 深一点的蓝色 */

.homework-stats-container {
  padding: 20px;
}

.loading-container, .error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.loading-container p, .error-container p {
  margin-top: 16px;
  color: #666;
  font-size: 16px;
  text-align: center;
}

.error-container .el-icon {
  margin-bottom: 16px;
}

.error-container .el-button {
  margin-top: 16px;
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
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e9ecef;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.progress-item:hover {
  background: #ffffff;
  border-color: @primary-light;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
}

.progress-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(135deg, @primary-dark, @primary);
  border-radius: 0 2px 2px 0;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.homework-title-section {
  display: flex;
  align-items: center;
}

.homework-number {
  font-weight: 700;
  color: @primary;
  font-size: 18px;
  background: linear-gradient(135deg, @primary-light, @primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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

.empty-data {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 16px;
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