<template>
  <div class="grade-statistics">
    <!-- 统计概览卡片 -->
    <div class="stats-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ totalStudents }}</div>
                <div class="stat-label">总学生数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ totalExams }}</div>
                <div class="stat-label">考试次数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ averageScore }}</div>
                <div class="stat-label">平均分</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><Trophy /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ passRate }}%</div>
                <div class="stat-label">及格率</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 成绩分布图表 -->
    <div class="charts-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>成绩分布</span>
              </div>
            </template>
            <div class="chart-container">
              <div ref="scoreDistributionChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>成绩趋势</span>
              </div>
            </template>
            <div class="chart-container">
              <div ref="scoreTrendChart" class="chart"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 成绩列表 -->
    <div class="grade-list-section">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>成绩详情</span>
          </div>
        </template>

        <el-table :data="gradeList" style="width: 100%" v-loading="loading">
          <el-table-column prop="studentName" label="学生姓名" width="120" />
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="examName" label="考试名称" width="150" />
          <el-table-column prop="score" label="成绩" width="100">
            <template #default="scope">
              <span :class="getScoreClass(scope.row.score)">{{ scope.row.score }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="rank" label="排名" width="80" />
          <el-table-column prop="examDate" label="考试日期" width="120" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { User, Document, TrendCharts, Trophy, Download } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {teacherService} from "@/services/api.js";

// const res = teacherService.getAllSituation(1)
// console.log(res)

// 响应式数据
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 统计数据
const totalStudents = ref(7);
const totalExams = ref(5);//todu
const averageScore = ref(79.8);
const passRate = ref(85.7);

// 图表引用
const scoreDistributionChart = ref(null);
const scoreTrendChart = ref(null);

// 成绩列表数据
const gradeList = ref([
  {
    studentName: '陈小明',
    studentId: '2023001',
    examName: '期中考试',
    score: 85,
    rank: 3,
    examDate: '2024-01-15',
    status: '已批改'
  },
  {
    studentName: '刘小红',
    studentId: '2023002',
    examName: '期中考试',
    score: 92,
    rank: 2,
    examDate: '2024-01-15',
    status: '已批改'
  },
  {
    studentName: '张小华',
    studentId: '2023003',
    examName: '期中考试',
    score: 78,
    rank: 5,
    examDate: '2024-01-15',
    status: '已批改'
  },
  {
    studentName: '孙小亮',
    studentId: '2023004',
    examName: '期中考试',
    score: 95,
    rank: 1,
    examDate: '2024-01-15',
    status: '已批改'
  },
  {
    studentName: '周小雯',
    studentId: '2023005',
    examName: '期中考试',
    score: 68,
    rank: 6,
    examDate: '2024-01-15',
    status: '已批改'
  },
  {
    studentName: '冯小琴',
    studentId: '2023005',
    examName: '期中考试',
    score: 82,
    rank: 4,
    examDate: '2024-01-15',
    status: '已批改'
  },
  {
    studentName: '韩小慧',
    studentId: '2023005',
    examName: '期中考试',
    score: 59,
    rank: 7,
    examDate: '2024-01-15',
    status: '已批改'
  },
]);

// 获取成绩样式类
const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent';
  if (score >= 80) return 'score-good';
  if (score >= 70) return 'score-average';
  if (score >= 60) return 'score-pass';
  return 'score-fail';
};

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case '已批改': return 'success';
    case '批改中': return 'warning';
    case '未批改': return 'info';
    default: return 'info';
  }
};

// 处理分页
const handleSizeChange = (val) => {
  pageSize.value = val;
  loadGradeData();
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
  loadGradeData();
};

// 加载成绩数据
const loadGradeData = () => {
  loading.value = true;
  // 模拟API调用
  setTimeout(() => {
    total.value = 7;
    loading.value = false;
  }, 1000);
};

// 初始化成绩分布图表
const initScoreDistributionChart = () => {
  const chart = echarts.init(scoreDistributionChart.value);
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '成绩分布',
        type: 'pie',
        radius: '50%',
        data: [
          { value: 2, name: '优秀(90-100)' },
          { value: 2, name: '良好(80-89)' },
          { value: 1, name: '中等(70-79)' },
          { value: 1, name: '及格(60-69)' },
          { value: 1, name: '不及格(<60)' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };
  chart.setOption(option);
};

// 初始化成绩趋势图表
const initScoreTrendChart = () => {
  const chart = echarts.init(scoreTrendChart.value);
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: ['第一次月考', '期中考试', '第二次月考', '期末考试']
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100
    },
    series: [
      {
        name: '平均分',
        type: 'line',
        data: [75, 78.5, 82, 85],
        smooth: true,
        lineStyle: {
          color: '#3b82f6'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0.1)' }
            ]
          }
        }
      }
    ]
  };
  chart.setOption(option);
};

onMounted(() => {
  loadGradeData();
  nextTick(() => {
    initScoreDistributionChart();
    initScoreTrendChart();
  });
});
</script>

<style scoped lang="less">
@primary: #2563eb; /* 主蓝色 */
@primary-light: #3b82f6; /* 浅一点的蓝色 */
@primary-dark: #1d4ed8; /* 深一点的蓝色 */

.grade-statistics {
  padding: 20px 0;
}

.stats-overview {
  margin-bottom: 30px;
}

.stat-card {
  height: 120px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 24px;
  color: white;
}

.stat-card:nth-child(1) .stat-icon {
  background: linear-gradient(135deg, @primary-dark, @primary);
}

.stat-card:nth-child(2) .stat-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card:nth-child(3) .stat-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card:nth-child(4) .stat-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #7f8c8d;
}

.charts-section {
  margin-bottom: 30px;
}

.chart-card {
  height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 320px;
}

.chart {
  width: 100%;
  height: 100%;
}

.grade-list-section {
  margin-bottom: 30px;
}

.score-excellent {
  color: #10b981;
  font-weight: bold;
}

.score-good {
  color: #3b82f6;
  font-weight: bold;
}

.score-average {
  color: #f59e0b;
  font-weight: bold;
}

.score-pass {
  color: #8b5cf6;
  font-weight: bold;
}

.score-fail {
  color: #ef4444;
  font-weight: bold;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style> 