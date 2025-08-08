<template>
  <div class="dashboard">
    <div class="top-bar"></div>

    <div class="page-title">
      <h2>大屏一览</h2>
      <p>全面掌握情况，精准提升质量</p>
    </div>

    <div class="filter-container">
      <el-select
          v-model="selectedClass"
          placeholder="请选择班级"
          @change="fetchData"
          popper-class="custom-select-dropdown"
      >
        <el-option
            v-for="item in classes"
            :key="item.value"
            :label="item.label"
            :value="item.value">
        </el-option>
      </el-select>
    </div>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-header">
            <h3>使用次数统计</h3>
            <span class="chart-desc">教师与学生平台使用频率分析</span>
          </div>
          <v-chart class="chart" :option="usageOption" autoresize />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-header">
            <h3>教学效率指数</h3>
            <span class="chart-desc">多维度教学效率评估</span>
          </div>
          <v-chart class="chart" :option="efficiencyOption" autoresize />
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-header">
            <h3>学生学习效果</h3>
            <span class="chart-desc">各知识点掌握程度分布</span>
          </div>
          <v-chart class="chart" :option="learningEffectOption" autoresize />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-container">
          <div class="chart-header">
            <h3>高频错误知识点</h3>
            <span class="chart-desc">错误率最高的知识点统计</span>
          </div>
          <el-table
              :data="commonErrors"
              stripe
              height="300"
              :row-class-name="tableRowClassName"
              :cell-style="{ transition: 'all 0.3s' }"
          >
            <el-table-column prop="point" label="知识点" />
            <el-table-column prop="count" label="错误次数" />
          </el-table>
        </div>
      </el-col>
    </el-row>

    <div class="page-footer">
      <div class="footer-content">
        <p>© 2025 教学数据分析系统 | 数据更新时间: {{ updateTime }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts';
import { ref, onMounted, computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart, PieChart } from 'echarts/charts'; // Removed RadarChart
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components';
import VChart from 'vue-echarts';

use([
  CanvasRenderer,
  BarChart, // Changed to BarChart for efficiency and learning effect
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
]);

// 格式化当前时间作为更新时间
const updateTime = computed(() => {
  const now = new Date();
  return now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
});

// 使用次数统计 - 优化了配色和动画
const usageOption = ref({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderColor: '#eee',
    borderWidth: 1,
    textStyle: { color: '#333' },
    padding: 12,
    boxShadow: '0 3px 10px rgba(0, 0, 0, 0.1)',
    transition: 'all 0.3s'
  },
  legend: {
    data: ['教师', '学生'],
    top: 0,
    textStyle: { color: '#666' }
  },
  xAxis: {
    type: 'category',
    data: ['当日', '本周'],
    axisLine: { lineStyle: { color: '#eee' } },
    axisLabel: { color: '#666' }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#f5f5f5' } },
    axisLabel: { color: '#666' }
  },
  series: [
    {
      name: '教师',
      type: 'bar',
      data: [120, 820],
      itemStyle: {
        color: '#165DFF',
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: { color: '#0E42D2' }
      },
      animationDuration: 1500
    },
    {
      name: '学生',
      type: 'bar',
      data: [250, 1500],
      itemStyle: {
        color: '#36D399',
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: { color: '#2AA97A' }
      },
      animationDuration: 1500,
      animationDelay: 300
    },
  ],
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
});

// 教学效率指数 - 更改为柱状图
const efficiencyOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    },
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderColor: '#eee',
    borderWidth: 1,
    textStyle: { color: '#333' },
    padding: 12,
    boxShadow: '0 3px 10px rgba(0, 0, 0, 0.1)',
  },
  xAxis: {
    type: 'category',
    data: ['备课耗时', '练习设计耗时', '课程优化', '修正耗时'],
    axisLine: { lineStyle: { color: '#eee' } },
    axisLabel: { color: '#666' }
  },
  yAxis: {
    type: 'value',
    name: '效率得分',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#f5f5f5' } },
    axisLabel: { color: '#666' }
  },
  series: [
    {
      name: '效率得分',
      type: 'bar',
      data: [85, 90, 75, 80],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
          offset: 0,
          color: '#165DFF'
        }, {
          offset: 1,
          color: '#6987ff'
        }]),
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: { color: '#0E42D2' }
      },
      animationDuration: 1500
    }
  ],
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
});

// 学生学习效果 - 更改为水平柱状图
const learningEffectOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    },
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderColor: '#eee',
    borderWidth: 1,
    textStyle: { color: '#333' },
    padding: 12,
    boxShadow: '0 3px 10px rgba(0, 0, 0, 0.1)',
  },
  xAxis: {
    type: 'value',
    name: '掌握程度 (%)',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#f5f5f5' } },
    axisLabel: { color: '#666' }
  },
  yAxis: {
    type: 'category',
    data: ['函数', '几何', '代数', '概率'],
    axisLine: { lineStyle: { color: '#eee' } },
    axisLabel: { color: '#666' }
  },
  series: [
    {
      name: '掌握程度',
      type: 'bar',
      data: [
        { value: 90, itemStyle: { color: '#165DFF' } },
        { value: 75, itemStyle: { color: '#36D399' } },
        { value: 60, itemStyle: { color: '#FF9F43' } },
        { value: 82, itemStyle: { color: '#722ED1' } },
      ],
      itemStyle: {
        borderRadius: [0, 4, 4, 0] // Rounded corners on the right
      },
      label: {
        show: true,
        position: 'right',
        valueAnimation: true,
        formatter: '{c}%',
        color: '#333',
        fontWeight: 'bold'
      },
      animationDuration: 1500,
      animationEasing: 'quinticInOut'
    }
  ],
  grid: { left: '3%', right: '7%', bottom: '3%', containLabel: true }
});

// 高频错误知识点
const commonErrors = ref([]);

const selectedClass = ref('all');
const classes = ref([
  { value: 'all', label: '全校' },
  { value: 'class1', label: '2306701班' },
  { value: 'class2', label: '2306801班' },
  { value: 'class3', label: '2306901班' },
]);

// 表格行样式
const tableRowClassName = ({ row, rowIndex }) => {
  return rowIndex === 0 ? 'highlight-row' : '';
};

// 数据获取与更新，添加了动画效果
const fetchData = () => {
  // 添加加载状态效果
  commonErrors.value = [];

  // Use setTimeout to simulate async loading
  setTimeout(() => {
    if (selectedClass.value === 'all') {
      // 全校数据
      usageOption.value.series[0].data = [120, 820];
      usageOption.value.series[1].data = [250, 1500];
      efficiencyOption.value.series[0].data = [85, 90, 75, 80];
      learningEffectOption.value.series[0].data = [
        { value: 90, itemStyle: { color: '#165DFF' } },
        { value: 75, itemStyle: { color: '#36D399' } },
        { value: 60, itemStyle: { color: '#FF9F43' } },
        { value: 82, itemStyle: { color: '#722ED1' } },
      ];
      commonErrors.value = [
        { point: '三角函数公式', count: 45 },
        { point: '虚拟语气', count: 32 },
        { point: '牛顿第二定律', count: 28 },
        { point: '化学方程式配平', count: 19 },
        { point: '逻辑运算符', count: 19 },
        { point: '等式解题', count: 18 },
        { point: '矩阵乘法', count: 17 },
        { point: '矩阵求逆', count: 16 },
        { point: '矩阵求秩', count: 15 },
        { point: '矩阵求迹', count: 13 },
        { point: '矩阵求行列式', count: 12 },
        { point: '矩阵求逆', count: 10 },
        { point: '矩阵求秩', count: 10 },
        { point: '矩阵求迹', count: 9 },
      ];
    } else {
      // 模拟班级数据
      const randomFactor = Math.random() + 0.5;
      usageOption.value.series[0].data = [20 * randomFactor, 150 * randomFactor];
      usageOption.value.series[1].data = [40 * randomFactor, 300 * randomFactor];
      efficiencyOption.value.series[0].data = [
        (70 + 10 * randomFactor).toFixed(0),
        (80 + 10 * randomFactor).toFixed(0),
        (65 + 10 * randomFactor).toFixed(0),
        (75 + 10 * randomFactor).toFixed(0),
      ];
      learningEffectOption.value.series[0].data = [
        { value: (80 + 10 * randomFactor).toFixed(0), itemStyle: { color: '#165DFF' } },
        { value: (70 + 10 * randomFactor).toFixed(0), itemStyle: { color: '#36D399' } },
        { value: (55 + 10 * randomFactor).toFixed(0), itemStyle: { color: '#FF9F43' } },
        { value: (78 + 10 * randomFactor).toFixed(0), itemStyle: { color: '#722ED1' } },
      ];
      commonErrors.value = [
        { point: '三角函数公式', count: (10 * randomFactor).toFixed(0) },
        { point: '虚拟语气', count: (8 * randomFactor).toFixed(0) },
        { point: '牛顿第二定律', count: (5 * randomFactor).toFixed(0) },
        { point: '化学方程式配平', count: (3 * randomFactor).toFixed(0) },
      ].sort((a, b) => b.count - a.count);
    }
  }, 300); // 模拟网络延迟
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
/* 引入现代无衬线字体 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

.dashboard {
  padding: 0 32px 32px;
  background-color: #f9fafb;
  min-height: 100vh;
  box-sizing: border-box;
  font-family: 'Inter', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  background-image:
      radial-gradient(at 40% 20%, rgba(22, 93, 255, 0.05) 0px, transparent 50%),
      radial-gradient(at 80% 0%, rgba(54, 211, 153, 0.05) 0px, transparent 50%),
      radial-gradient(at 0% 50%, rgba(114, 46, 209, 0.05) 0px, transparent 50%);
}

/* 顶部装饰条 */
.top-bar {
  height: 6px;
  background: linear-gradient(90deg, #165DFF, #36D399, #722ED1);
  width: 100%;
}

/* 页面标题 */
.page-title {
  padding: 40px 0 24px;
  text-align: center;
}

.page-title h2 {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 700;
  color: #1D2939;
  letter-spacing: -0.5px;
}

.page-title p {
  margin: 0;
  font-size: 16px;
  color: #667085;
}

.filter-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.1);
  margin-bottom: 32px;
  transition: all 0.3s ease;
}

.filter-container:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05), 0 2px 4px rgba(0, 0, 0, 0.1);
}

.el-select {
  width: 240px;
  font-size: 16px;
}

.el-select .el-input__inner {
  height: 44px;
  border-radius: 8px;
  border-color: #E5E7EB;
  padding: 0 16px;
  transition: all 0.2s;
}

.el-select .el-input__inner:focus {
  border-color: #165DFF;
  box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.1);
}

/* 自定义下拉菜单样式 */
:deep(.custom-select-dropdown) {
  border-radius: 8px;
  border-color: #E5E7EB;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  padding: 8px 0;
}

:deep(.custom-select-dropdown .el-select-dropdown__item) {
  padding: 12px 16px;
  transition: all 0.2s;
}

:deep(.custom-select-dropdown .el-select-dropdown__item:hover) {
  background-color: #F0F5FF;
  color: #165DFF;
}

:deep(.custom-select-dropdown .el-select-dropdown__item.selected) {
  background-color: #E6F7FF;
  color: #165DFF;
  font-weight: 500;
}

.chart-container {
  background: #ffffff;
  padding: 28px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.1);
  height: 440px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

/* 卡片顶部装饰线 */
.chart-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #165DFF, #36D399);
  opacity: 0.8;
}

.chart-container:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05), 0 6px 12px rgba(0, 0, 0, 0.08);
}

.chart-header {
  margin-bottom: 20px;
}

.chart-container h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1D2939;
  position: relative;
  padding-left: 12px;
}

.chart-container h3::before {
  content: '';
  position: absolute;
  width: 4px;
  height: 18px;
  background-color: #165DFF;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  border-radius: 2px;
}

.chart-desc {
  font-size: 13px;
  color: #667085;
  padding-left: 12px;
}

.chart {
  flex: 1;
  height: auto;
  transition: all 0.5s ease;
}

/* 表格样式优化 */
.el-table {
  border-radius: 8px;
  overflow: hidden;
  font-size: 14px;
  box-shadow: none;
  border: 1px solid #F2F4F7;
}

.el-table th {
  background-color: #F9FAFB;
  font-weight: 600;
  color: #344054;
  height: 48px;
  padding: 0 16px;
  border-bottom: 1px solid #F2F4F7;
}

.el-table td {
  color: #344054;
  height: 48px;
  padding: 0 16px;
  border-bottom: 1px solid #F2F4F7;
}

/* 高亮显示错误次数最多的行 */
:deep(.highlight-row td) {
  color: #D92D20;
  font-weight: 500;
  background-color: #FEF3F2 !important;
}

:deep(.el-table__body tr:hover > td) {
  background-color: #F9FAFB !important;
}

/* 页脚样式 */
.page-footer {
  margin-top: 48px;
  padding: 24px 0;
  border-top: 1px solid #F2F4F7;
}

.footer-content {
  text-align: center;
  font-size: 14px;
  color: #667085;
}

.footer-content p {
  margin: 0;
}

/* 响应式调整 */
@media (max-width: 1024px) {
  .dashboard {
    padding: 0 24px 24px;
  }

  .chart-container {
    height: 400px;
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .el-col {
    width: 100% !important;
    max-width: 100% !important;
    flex: 0 0 100% !important;
    margin-bottom: 24px;
  }

  .chart-container {
    height: auto;
    min-height: 320px;
  }

  .filter-container {
    flex-direction: column;
    align-items: stretch;
  }

  .el-select {
    width: 100%;
  }

  .page-title h2 {
    font-size: 24px;
  }
}
</style>