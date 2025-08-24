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
            <div class="header-actions">
              <!-- 考试选择下拉框 -->
              <el-select
                  v-model="selectedExamId"
                  placeholder="请选择考试"
                  style="width: 220px; margin-right: 15px"
                  clearable
                  :disabled="examList.length === 0"
              >
                <el-option
                    v-for="exam in examList"
                    :key="exam.id"
                    :label="exam.label"
                    :value="exam.value"
                ></el-option>
              </el-select>
              <!-- 导出按钮 -->
              <el-button
                  type="primary"
                  :icon="Download"
                  @click="handleExport"
                  :disabled="!selectedExamId || examList.length === 0"
              >
                导出成绩
              </el-button>
            </div>
          </div>
        </template>
        <el-table :data="paginatedGradeList" style="width: 100%" v-loading="loading">
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
import { ref, onMounted, nextTick, watch, onBeforeUnmount, computed } from 'vue';
import { useRoute } from 'vue-router';
import { User, Document, TrendCharts, Trophy, Download } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {teacherService} from "@/services/api.js";
import * as XLSX from "xlsx";

const route = useRoute();
const courseId = ref(route.params.courseId);

// 响应式数据
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const examList = ref([]); // 考试列表（格式：[{id, label, value}]）
const selectedExamId = ref(''); // 选中的考试ID

// 统计数据
const totalStudents = ref(0);
const totalExams = ref(0);
const averageScore = ref(0);
const passRate = ref(0);

// 成绩分布数据
const scoreDistribution = ref({
  scoreA: 0, // 优秀
  scoreB: 0, // 良好
  scoreC: 0, // 中等
  scoreD: 0, // 及格
  scoreF: 0  // 不及格
});

// 成绩趋势数据
const scoreTrend = ref({
  examTitles: [],
  averageScores: []
});

// 图表引用
const scoreDistributionChart = ref(null);
const scoreTrendChart = ref(null);
let scoreDistributionChartInstance = null;
let scoreTrendChartInstance = null;

// 成绩列表数据
const gradeList = ref([]);

// 分页后的成绩列表
const paginatedGradeList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  const result = filteredGradeList.value.slice(start, end);
  console.log('分页信息:', {
    currentPage: currentPage.value,
    pageSize: pageSize.value,
    total: filteredGradeList.value.length,
    start,
    end,
    resultLength: result.length
  });
  return result;
});

const handleExport = () => {
  if (!selectedExamId.value) {
    ElMessage.warning('请先选择需要导出的考试');
    return;
  }

  // 获取选中考试信息
  const selectedExam = examList.value.find(item => item.value === selectedExamId.value);
  const examName = selectedExam ? selectedExam.label : '未知考试';

  console.log('导出信息:', examName)

  // 整理导出数据（匹配Excel表头）
  const exportData = filteredGradeList.value.map(item => ({
    '学生姓名': item.studentName,
    '学号': item.studentId,
    '考试名称': item.examName,
    '成绩': item.score,
    '排名': item.rank,
    '考试日期': item.examDate,
    '状态': item.status
  }));

  // 生成Excel工作簿
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet(exportData); // JSON转工作表
  XLSX.utils.book_append_sheet(workbook, worksheet, '成绩列表'); // 添加工作表

  // 下载Excel文件
  XLSX.writeFile(workbook, `${examName}_成绩导出.xlsx`);
  ElMessage.success(`《${examName}》成绩导出成功`);
};

// 获取成绩样式类
const getScoreClass = (score) => {
  const numScore = Number(score) || 0;
  if (numScore >= 90) return 'score-excellent';
  if (numScore >= 80) return 'score-good';
  if (numScore >= 70) return 'score-average';
  if (numScore >= 60) return 'score-pass';
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
  currentPage.value = 1; // 重置到第一页
};

const handleCurrentChange = (val) => {
  currentPage.value = val;
};

// 加载成绩数据
const loadGradeData = async () => {
  try {
    loading.value = true;
    const response = await teacherService.getStudentExam(courseId.value);
    console.log("loadGradeData:", response);

    if (response.data && response.data.success) {
      const data = response.data.data;
      console.log("获取到成绩信息", data)

      // 原有逻辑：更新统计数据、成绩分布、成绩趋势...
      totalStudents.value = data.studentCount || 0;
      totalExams.value = data.examCount || 0;
      averageScore.value = data.overallAverageScore || 0;
      passRate.value = data.passRate || 0;
      scoreDistribution.value = {
        scoreA: data.scoreA || 0,
        scoreB: data.scoreB || 0,
        scoreC: data.scoreC || 0,
        scoreD: data.scoreD || 0,
        scoreF: data.scoreF || 0
      };
      scoreTrend.value.examTitles = data.examDetails.map(exam => exam.title);
      scoreTrend.value.averageScores = data.examDetails.map(exam => exam.averageScore || 0);

      examList.value = data.examDetails.map(exam => ({
        id: exam.examId, // 唯一标识
        label: exam.title, // 下拉框显示文本
        value: exam.examId // 下拉框选中值（与selectedExamId绑定）
      }));

      // 修改：构建成绩列表时，新增examId字段（用于过滤）
      gradeList.value = [];
      data.examDetails.forEach(exam => {
        if (exam.studentScores && exam.studentScores.length > 0) {
          exam.studentScores.forEach((student, index) => {
            gradeList.value.push({
              studentName: student.studentName || `学生${index + 1}`,
              studentId: student.studentId || `ID${index + 1}`,
              examName: exam.title,
              examId: exam.examId,
              score: student.score || 0,
              rank: student.rank || index + 1,
              examDate: new Date(exam.examDate).toLocaleDateString(),
              status: '已批改'
            });
          });
        }
      });

      // 修改：分页总数基于过滤后的数据
      total.value = filteredGradeList.value.length;

      // 重新初始化图表
      nextTick(() => {
        initScoreDistributionChart();
        initScoreTrendChart();
      });
    }
  } catch (error) {
    console.error('获取成绩数据失败:', error);
    ElMessage.error('获取成绩数据失败');
  } finally {
    loading.value = false;
  }
};

const filteredGradeList = computed(() => {
  if (!selectedExamId.value) return gradeList.value; // 未选考试时显示全部
  // 按考试ID过滤（需确保gradeList中包含examId字段）
  return gradeList.value.filter(item => item.examId === selectedExamId.value);
});

// 初始化成绩分布图表
const initScoreDistributionChart = () => {
  if (!scoreDistributionChart.value) return;

  // 销毁旧实例
  if (scoreDistributionChartInstance) {
    scoreDistributionChartInstance.dispose();
  }

  scoreDistributionChartInstance = echarts.init(scoreDistributionChart.value);
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
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
          { value: scoreDistribution.value.scoreA, name: '优秀(90-100)', itemStyle: { color: '#10b981' } },
          { value: scoreDistribution.value.scoreB, name: '良好(80-89)', itemStyle: { color: '#3b82f6' } },
          { value: scoreDistribution.value.scoreC, name: '中等(70-79)', itemStyle: { color: '#f59e0b' } },
          { value: scoreDistribution.value.scoreD, name: '及格(60-69)', itemStyle: { color: '#8b5cf6' } },
          { value: scoreDistribution.value.scoreF, name: '不及格(<60)', itemStyle: { color: '#ef4444' } }
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
  scoreDistributionChartInstance.setOption(option);
};

// 初始化成绩趋势图表
const initScoreTrendChart = () => {
  if (!scoreTrendChart.value) return;

  // 销毁旧实例
  if (scoreTrendChartInstance) {
    scoreTrendChartInstance.dispose();
  }

  scoreTrendChartInstance = echarts.init(scoreTrendChart.value);
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: function(params) {
        return `${params[0].name}<br/>平均分: ${params[0].value}`;
      }
    },
    xAxis: {
      type: 'category',
      data: scoreTrend.value.examTitles,
      axisLabel: {
        rotate: 45,
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      name: '分数'
    },
    series: [
      {
        name: '平均分',
        type: 'line',
        data: scoreTrend.value.averageScores,
        smooth: true,
        lineStyle: {
          color: '#3b82f6',
          width: 3
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
        },
        itemStyle: {
          color: '#3b82f6'
        }
      }
    ]
  };
  scoreTrendChartInstance.setOption(option);
};

// 监听路由参数变化
watch(
  () => route.params.courseId,
  (newCourseId) => {
    if (newCourseId && newCourseId !== courseId.value) {
      courseId.value = newCourseId;
      loadGradeData();
    }
  }
);

// 监听数据变化时重置分页
watch(
  () => gradeList.value.length,
  () => {
    currentPage.value = 1;
  }
);

watch(selectedExamId, () => {
  currentPage.value = 1;
  total.value = filteredGradeList.value.length;
});

onMounted(() => {
  loadGradeData();
});

// 组件卸载时清理图表实例
onBeforeUnmount(() => {
  if (scoreDistributionChartInstance) {
    scoreDistributionChartInstance.dispose();
  }
  if (scoreTrendChartInstance) {
    scoreTrendChartInstance.dispose();
  }
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

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
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