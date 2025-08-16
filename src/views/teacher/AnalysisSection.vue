<template>
  <div class="analysis-section">
    <div class="section-header">
      <h2>学情数据分析</h2>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <label>课程:</label>
        <select v-model="selectedCourse" @change="onCourseChange" :disabled="loading">
          <option value="">{{ loading ? '加载中...' : '请选择课程' }}</option>
          <option v-for="course in courses" :key="course.id" :value="course.id">{{ course.name }}</option>
        </select>
      </div>
      <button class="primary-btn" @click="emit('fetchAnalysisData')" :disabled="loading">查询</button>
      <div style="flex:1"></div>
      <button class="primary-btn ai-btn" @click="onAIClick" :disabled="loading">AI学情分析</button>
    </div>

    <!-- 知识点掌握情况独占一行 -->
    <div class="analysis-card knowledge-card">
      <h3>知识点掌握情况</h3>
      <div class="knowledge-top">
        <div class="chart-container">
          <v-chart :option="masteryChartOption" autoresize style="height:300px" />
        </div>
        <div class="knowledge-points-list">
          <div v-if="knowledgePoints.length === 0" class="no-knowledge-points">
            <p>暂无知识点掌握情况数据</p>
          </div>
          <div v-for="(point, index) in knowledgePoints" :key="index" class="knowledge-point-item">
            <div class="point-name">{{ point.name }}</div>
            <div class="point-progress">
              <div class="progress-bar" :style="{ width: point.masteryRate + '%', backgroundColor: getColorByRate(point.masteryRate) }"></div>
              <span class="progress-text">{{ point.masteryRate }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 班级整体情况和高频错误分析并排 -->
    <div class="analysis-row">
      <div class="analysis-card class-card">
        <div class="card-header">
          <h3>整体情况</h3>
          <div class="exam-filter">
            <label>考试:</label>
            <select
                v-model="selectedExamId"
                @change="onExamChange"
                :disabled="!selectedCourse || examLoading || examList.length === 0"
            >
<!--              <option value="">{{ examLoading ? '加载中...' : '全部考试' }}</option>-->
              <option v-for="exam in examList" :key="exam.id" :value="exam.id">
                {{ exam.date ? (exam.name + '（' + exam.date + '）') : exam.name }}
              </option>
            </select>
          </div>
        </div>

        <div class="stats">
          <div class="stat-item">
            <div class="stat-value">{{ analysisData.averageScore || '0' }}</div>
            <div class="stat-label">平均分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ analysisData.passRate || '0%' }}</div>
            <div class="stat-label">通过率</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ analysisData.excellentRate || '0%' }}</div>
            <div class="stat-label">优秀率</div>
          </div>
        </div>

        <div class="chart-container">
          <div ref="scoreChartRef" style="height:300px;width:100%"></div>
        </div>
      </div>
      <div class="analysis-card error-card">
        <h3>高频错误分析</h3>
        <div class="error-list">
          <div v-if="commonErrors.length === 0" class="no-errors">
            <p>暂无高频错误数据</p>
          </div>
          <div v-for="(error, index) in commonErrors" :key="index" class="error-item">
            <div class="error-info">
              <div class="error-title">{{ error.description }}</div>
              <div class="error-stats">出现频率: {{ error.frequency }}次</div>
            </div>
            <div class="error-suggestion">
              <h4>修正建议:</h4>
              <p>{{ error.suggestion }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI学情分析弹窗 -->
    <el-dialog
        v-model="showAIAnalysis"
        title="AI学情分析"
        width="75%"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        class="ai-analysis-dialog"
    >
      <AILearningAnalysis
          :student-data="analysisData"
          @close="showAIAnalysis = false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue';
import { ref, computed, onMounted } from 'vue';
import VChart from 'vue-echarts';
import * as echarts from 'echarts';
import { getCurrentUserId } from "@/utils/userUtils";
import AILearningAnalysis from '@/components/teacher/AILearningAnalysis.vue';
import {teacherService, adminService} from "@/services/api.js";
import { ElMessage } from 'element-plus';

// 历史考试下拉相关
const examList = ref([]);
const selectedExamId = ref('');
const examLoading = ref(false);
const props = defineProps({
  // selectedClass: String, // 删除
  selectedCourse: String,
  courses: {
    type: Array,
    default: () => [],
  },
  analysisData: {
    type: Object,
    default: () => ({
      averageScore: 0,
      passRate: '0%',
      excellentRate: '0%',
    }),
  },
  knowledgePoints: {
    type: Array,
    default: () => [],
  },
  commonErrors: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['update:selectedClass', 'update:selectedCourse', 'fetchAnalysisData']);

// 获取课程对应的历史考试列表
const fetchExamList = async (courseId) => {
  examLoading.value = true;
  try {
    const res = await adminService.getAllPaper(courseId);
    console.log("获取课程对应的历史考试列表", res)
    if (res?.data?.success) {
      examList.value = (res.data.data || []).map(exam => ({
        id: exam.examId,
        name: exam.title,
        date: exam.examDate || exam.createTime || ''
      }));
    } else {
      examList.value = [];
    }
  } catch (err) {
    console.error('获取考试列表失败:', err);
    ElMessage.error('获取考试列表失败');
    examList.value = [];
  } finally {
    examLoading.value = false;
  }
};

// 选择考试时，仅更新“整体情况”数据与柱状图
const onExamChange = async () => {
  const courseId = selectedCourse.value;
  await fetchAnalysisData(courseId, selectedExamId.value);
  console.log('[onExamChange] examId =', selectedExamId.value);
};
const currentUserId = getCurrentUserId();

// 获取教师信息和课程列表
const teacherInfo = ref(null);
const courses = ref([]);
const selectedCourse = ref('');
const loading = ref(false);
const commonErrors = ref([]); // 高频错误数据
const knowledgePoints = ref([]); // 知识点掌握情况数据
const analysisData = ref({
  averageScore: 0,
  passRate: '0%',
  excellentRate: '0%',
  failNumber: 0,
  goodNumber: 0,
  normalNumber: 0,
  passNumber: 0,
  excellentNumber: 0
});

// 获取教师信息并加载课程列表
const loadTeacherInfoAndCourses = async () => {
  loading.value = true;
  try {
    // 1. 获取教师信息
    const teacherResponse = await teacherService.getTeacherInfo(currentUserId);
    // console.log('教师信息响应:', teacherResponse);
      if (teacherResponse.data && teacherResponse.data.success) {
        teacherInfo.value = teacherResponse.data.data;
        const teacherId = teacherResponse.data.data.teacherId;
        // console.log('获取到的教师ID:', teacherId);
        
        // 2. 用teacherId获取课程列表
        const coursesResponse = await teacherService.getAllCourse(teacherId);
        // console.log('课程列表响应:', coursesResponse);
      
      if (coursesResponse.data && coursesResponse.data.success) {
        courses.value = coursesResponse.data.data.map(course => ({
          id: course.courseId,
          name: course.courseName
        }));
        
        console.log('加载的课程列表:', courses.value);
        
        // 如果有课程，默认选择第一个
        if (courses.value.length > 0) {
          selectedCourse.value = courses.value[0].id;
          console.log('默认选择课程ID:', selectedCourse.value);
          // 自动触发数据获取
          onCourseChange();
        }
      }
    }
  } catch (error) {
    console.error('获取教师信息或课程列表失败:', error);
    ElMessage.error('获取课程信息失败，请刷新页面重试');
  } finally {
    loading.value = false;
  }
};

const getColorByRate = (rate) => {
  if (rate < 60) return '#e74c3c'; // Red
  if (rate < 80) return '#f39c12'; // Orange
  return '#2ecc71'; // Green
};

// 成绩分布柱状图（自定义配色，原生echarts渲染）
const scoreChartRef = ref(null);
const scoreChart = ref(null); // 保存图表实例
const barColors = [
  { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [ { offset: 0, color: '#ffb347' }, { offset: 1, color: '#ffcc33' } ] },
  { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [ { offset: 0, color: '#4fc3f7' }, { offset: 1, color: '#1976d2' } ] },
  { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [ { offset: 0, color: '#81e8ae' }, { offset: 1, color: '#00b894' } ] },
  { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [ { offset: 0, color: '#f7a7b7' }, { offset: 1, color: '#e17055' } ] },
  { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [ { offset: 0, color: '#b388ff' }, { offset: 1, color: '#7c4dff' } ] }
];

// 创建柱状图配置函数
const createScoreChartOption = (data) => {
  return {
    title: { text: '成绩分布', left: 'center', top: 10, textStyle: { fontSize: 18, color: '#333', fontWeight: 'bold' } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(50,50,50,0.9)', borderRadius: 8, textStyle: { color: '#fff', fontSize: 14 } },
    grid: { left: 40, right: 20, bottom: 40, top: 60 },
    xAxis: { type: 'category', data: ['90-100', '80-89', '70-79', '60-69', '0-59'], axisTick: { alignWithLabel: true }, axisLine: { lineStyle: { color: '#bbb' } }, axisLabel: { fontSize: 14, color: '#666' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }, axisLabel: { fontSize: 14, color: '#666' } },
    series: [{
      name: '人数',
      type: 'bar',
      data: [
        data.excellentNumber || 0,
        data.goodNumber || 0,
        data.normalNumber || 0,
        data.passNumber || 0,
        data.failNumber || 0
      ],
      barWidth: 38,
      itemStyle: {
        borderRadius: [10, 10, 0, 0],
        shadowColor: 'rgba(52,152,219,0.15)',
        shadowBlur: 10,
        color: function(params) { return barColors[params.dataIndex]; }
      },
      label: { show: true, position: 'top', color: '#333', fontWeight: 'bold', fontSize: 14 }
    }]
  };
};

// 知识点掌握率环形图
const masteryChartOption = computed(() => {
  return {
    title: { text: '知识点掌握率', left: 'center', top: 10, textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { orient: 'vertical', left: 'left', data: props.knowledgePoints.map(kp => kp.name) },
    series: [
      {
        name: '掌握率',
        type: 'pie',
        radius: ['60%', '80%'],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
        labelLine: { show: false },
        data: props.knowledgePoints.map(kp => ({ value: kp.masteryRate, name: kp.name }))
      }
    ]
  };
});

const showAIAnalysis = ref(false);

const onAIClick = () => {
  showAIAnalysis.value = true;
};

// 获取知识点掌握情况数据
const fetchKnowledgePoints = async (courseId) => {
  try {
    const response = await teacherService.getAllClassNotCorrectInfo(courseId);
    console.log('知识点掌握情况响应:', response);
    
    if (response.data && response.data.success) {
      const knowledgeData = response.data.data;
      const knowledgePointsData = [];
      
      // 遍历知识点ID和掌握情况
      for (const [pointId, masteryRate] of Object.entries(knowledgeData)) {
        try {
          // 获取知识点名称
          const nameResponse = await teacherService.getKnowledgeNameById(pointId);
          // console.log(`知识点${pointId}名称响应:`, nameResponse);
          
          if (nameResponse.data && nameResponse.data.success) {
            knowledgePointsData.push({
              id: pointId,
              name: nameResponse.data.data,
              masteryRate: masteryRate
            });
          }
        } catch (error) {
          console.error(`获取知识点${pointId}名称失败:`, error);
        }
      }
      
      knowledgePoints.value = knowledgePointsData;
      console.log('处理后的知识点数据:', knowledgePoints.value);
    }
  } catch (error) {
    console.error('获取知识点掌握情况失败:', error);
    ElMessage.error('获取知识点掌握情况失败');
  }
};

// 获取整体情况数据
const fetchAnalysisData = async (courseId, examId) => {
  // 定义默认空数据结构（全零）
  const defaultEmptyData = {
    averageScore: 0,
    passRate: 0,
    excellentRate: 0,
    failNumber: 0,
    goodNumber: 0,
    normalNumber: 0,
    passNumber: 0,
    excellentNumber: 0
  };

  try {
    const res = await teacherService.getAllSituation(courseId, examId);
    if (res.data && res.data.success) {
      // 若后端返回数据为空，使用默认空数据
      const data = res.data.data || defaultEmptyData;
      analysisData.value = {
        averageScore: data.averageScore || 0,
        passRate: `${data.passRate || 0}%`,
        excellentRate: `${data.excellentRate || 0}%`,
        failNumber: data.failNumber || 0,
        goodNumber: data.goodNumber || 0,
        normalNumber: data.normalNumber || 0,
        passNumber: data.passNumber || 0,
        excellentNumber: data.excellentNumber || 0
      };
      // 强制图表使用当前数据（即使为空）
      scoreChart.value?.setOption(createScoreChartOption(data));
    } else {
      // 接口成功但返回失败标识，重置为默认空数据
      analysisData.value = {
        averageScore: 0,
        passRate: '0%',
        excellentRate: '0%',
        ...defaultEmptyData
      };
      scoreChart.value?.setOption(createScoreChartOption(defaultEmptyData));
    }
  } catch (e) {
    console.error('获取整体情况数据失败:', e);
    ElMessage.error('获取整体情况数据失败');
    // 接口失败时，强制重置为默认空数据
    analysisData.value = {
      averageScore: 0,
      passRate: '0%',
      excellentRate: '0%',
      ...defaultEmptyData
    };
    scoreChart.value?.setOption(createScoreChartOption(defaultEmptyData));
  }
};

// 获取高频错误数据
const fetchCommonErrors = async (courseId) => {
  try {
    const response = await teacherService.getTheMaxUncorrectPoint(courseId);
    console.log('高频错误数据响应:', response);
    
    if (response.data && response.data.success) {
      // 将后端数据转换为组件需要的格式
      commonErrors.value = response.data.data.map(item => ({
        description: item.pointName,
        frequency: item.wrongAnswerCount,
        suggestion: `知识点: ${item.description}，难度: ${item.difficultyLevel}，关键词: ${item.keywords || '无'}`
      }));
    }
  } catch (error) {
    console.error('获取高频错误数据失败:', error);
    ElMessage.error('获取高频错误数据失败');
  }
};

// 新增课程切换时自动查询逻辑
const onCourseChange = async () => {
  const selectedCourseId = selectedCourse.value;
  console.log('下拉框选择变化，当前值:', selectedCourseId);
  emit('update:selectedCourse', selectedCourseId);

  if (selectedCourseId) {
    const selectedCourseObj = courses.value.find(course => course.id == selectedCourseId);
    if (selectedCourseObj) {
      console.log('选中的课程ID:', selectedCourseObj.id);
      console.log('选中的课程名称:', selectedCourseObj.name);

      // 切换课程时重置考试筛选
      selectedExamId.value = '';

      // 获取课程对应的历史考试列表
      await fetchExamList(selectedCourseObj.id);

      // 如果有历史考试，默认选择第一个考试
      if (examList.value.length > 0) {
        selectedExamId.value = examList.value[0].id;
        console.log('默认选择考试ID:', selectedExamId.value);
      }

      // 调用 fetchAnalysisData 时确保 examId 有值
      await fetchAnalysisData(selectedCourseObj.id, selectedExamId.value);

      // 获取高频错误数据和知识点掌握情况数据
      await Promise.all([
        fetchCommonErrors(selectedCourseObj.id),
        fetchKnowledgePoints(selectedCourseObj.id)
      ]);
    }
  } else {
    console.log('未选择课程');
    selectedExamId.value = '';
    examList.value = [];
    commonErrors.value = [];
    knowledgePoints.value = [];
    analysisData.value = {
      averageScore: 0,
      passRate: '0%',
      excellentRate: '0%',
      failNumber: 0,
      goodNumber: 0,
      normalNumber: 0,
      passNumber: 0,
      excellentNumber: 0
    };
    if (scoreChart.value) {
      scoreChart.value.setOption(createScoreChartOption({
        excellentNumber: 0,
        goodNumber: 0,
        normalNumber: 0,
        passNumber: 0,
        failNumber: 0
      }));
    }
  }

  emit('fetchAnalysisData');
};

onMounted(() => {
  loadTeacherInfoAndCourses();
  if (scoreChartRef.value) {
    scoreChart.value = echarts.init(scoreChartRef.value);
    // 初始化时使用默认数据
    scoreChart.value.setOption(createScoreChartOption({
      excellentNumber: 0,
      goodNumber: 0,
      normalNumber: 0,
      passNumber: 0,
      failNumber: 0
    }));
    window.addEventListener('resize', () => scoreChart.value.resize());
  }
});
</script>

<style scoped>
.analysis-section {
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 24px;
  color: #333;
}

.filter-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  align-items: center;
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.filter-item label {
  margin-right: 10px;
  font-weight: bold;
  color: #555;
}

.filter-item select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 15px;
  min-width: 120px;
  cursor: pointer;
  background-color: white;
  outline: none;
}

.filter-item select:focus {
  border-color: #0096ff;
  box-shadow: 0 0 0 2px rgba(0, 150, 255, 0.2);
}

.filter-item select:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.6;
}

.primary-btn {
  background-color: #0096ff;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s ease;
}

.primary-btn:hover {
  background-color: #2980b9;
}

.ai-btn {
  background: linear-gradient(90deg, #3680e0, #42a7ff);
  margin-left: 10px;
  font-weight: bold;
  letter-spacing: 1px;
  box-shadow: 0 2px 8px rgba(33,147,176,0.08);
}

.analysis-dashboard {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.analysis-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  padding: 20px;
  margin-bottom: 20px;
}

.stats {
  display: flex;
  gap: 30px;
  margin-bottom: 18px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 22px;
  font-weight: bold;
  color: #3498db;
}

.stat-label {
  font-size: 14px;
  color: #888;
}



.chart-container {
  margin-top: 10px;
  margin-bottom: 10px;
}

.knowledge-points-list {
  margin-top: 18px;
}
.knowledge-point-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.point-name {
  width: 100px;
  color: #555;
}
.point-progress {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 6px;
  overflow: hidden;
  margin-left: 10px;
  height: 22px;
  position: relative;
}
.progress-bar {
  height: 100%;
  border-radius: 6px 0 0 6px;
  transition: width 0.4s;
}
.progress-text {
  position: absolute;
  right: 10px;
  color: #333;
  font-size: 13px;
}
.error-list {
  margin-top: 10px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 6px;
}
.error-item {
  background: #f8f8f8;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 10px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
.error-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.error-title {
  font-weight: bold;
  color: #e67e22;
}
.error-stats {
  color: #888;
  font-size: 13px;
}
.error-suggestion {
  margin-top: 6px;
  color: #666;
  font-size: 14px;
}

.no-errors {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.no-knowledge-points {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

/* 新增布局样式 */
.knowledge-card {
  margin-bottom: 30px;
}
.knowledge-top {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}
.knowledge-points-list {
  flex: 1;
  min-width: 200px;
}

.analysis-row {
  display: flex;
  gap: 30px;
}
.class-card, .error-card {
  flex: 1;
}

/* AI分析弹窗样式 */
:deep(.ai-analysis-dialog) {
  .el-dialog__body {
    padding: 0;
    max-height: 80vh;
    overflow-y: auto;
  }

  .el-dialog__header {
    background: linear-gradient(135deg, #3458e7 0%, #4b67a2 100%);
    color: white;
    border-radius: 8px 8px 0 0;
  }

  .el-dialog__title {
    color: white;
    font-weight: 600;
  }

  .el-dialog__headerbtn .el-dialog__close {
    color: white;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.exam-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.exam-filter label {
  font-weight: bold;
  color: #555;
}

.exam-filter select {
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  min-width: 160px;
  background-color: #fff;
  outline: none;
}

.exam-filter select:focus {
  border-color: #0096ff;
  box-shadow: 0 0 0 2px rgba(0, 150, 255, 0.15);
}
</style>