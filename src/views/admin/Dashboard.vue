<template>
  <div class="dashboard">
    <div class="top-bar"></div>

    <div class="page-title">
      <h2>大屏一览</h2>
      <p>全面掌握情况，精准提升质量</p>
    </div>
    <!--
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
    </div> -->

    <el-row :gutter="24" style="margin-top: 24px">
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
    <el-row :gutter="24" style="margin-top: 24px">
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
import * as echarts from "echarts";
import { ref, onMounted, computed } from "vue";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { BarChart, PieChart } from "echarts/charts"; // Removed RadarChart
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from "echarts/components";
import VChart from "vue-echarts";
import { adminService } from "@/services/api";

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
  return now.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
});

// 使用次数统计 - 优化了配色和动画
const usageOption = ref({
  tooltip: {
    trigger: "axis",
    backgroundColor: "rgba(255, 255, 255, 0.9)",
    borderColor: "#eee",
    borderWidth: 1,
    textStyle: { color: "#333" },
    padding: 12,
    boxShadow: "0 3px 10px rgba(0, 0, 0, 0.1)",
    transition: "all 0.3s",
  },
  legend: {
    data: ["教师", "学生"],
    top: 0,
    textStyle: { color: "#666" },
  },
  xAxis: {
    type: "category",
    data: ["当日", "本周"],
    axisLine: { lineStyle: { color: "#eee" } },
    axisLabel: { color: "#666" },
  },
  yAxis: {
    type: "value",
    axisLine: { show: false },
    splitLine: { lineStyle: { color: "#f5f5f5" } },
    axisLabel: { color: "#666" },
  },
  series: [
    {
      name: "教师",
      type: "bar",
      data: [120, 820],
      itemStyle: {
        color: "#165DFF",
        borderRadius: [4, 4, 0, 0],
      },
      emphasis: {
        itemStyle: { color: "#0E42D2" },
      },
      animationDuration: 1500,
    },
    {
      name: "学生",
      type: "bar",
      data: [250, 1500],
      itemStyle: {
        color: "#36D399",
        borderRadius: [4, 4, 0, 0],
      },
      emphasis: {
        itemStyle: { color: "#2AA97A" },
      },
      animationDuration: 1500,
      animationDelay: 300,
    },
  ],
  grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
});

// 教学效率指数 - 更改为柱状图
const efficiencyOption = ref({
  tooltip: {
    trigger: "axis",
    axisPointer: {
      type: "shadow",
    },
    backgroundColor: "rgba(255, 255, 255, 0.9)",
    borderColor: "#eee",
    borderWidth: 1,
    textStyle: { color: "#333" },
    padding: 12,
    boxShadow: "0 3px 10px rgba(0, 0, 0, 0.1)",
  },
  xAxis: {
    type: "category",
    data: ["备课耗时", "练习设计耗时", "课程优化", "修正耗时"],
    axisLine: { lineStyle: { color: "#eee" } },
    axisLabel: { color: "#666" },
  },
  yAxis: {
    type: "value",
    name: "效率得分",
    axisLine: { show: false },
    splitLine: { lineStyle: { color: "#f5f5f5" } },
    axisLabel: { color: "#666" },
  },
  series: [
    {
      name: "效率得分",
      type: "bar",
      data: [85, 90, 75, 80],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {
            offset: 0,
            color: "#165DFF",
          },
          {
            offset: 1,
            color: "#6987ff",
          },
        ]),
        borderRadius: [4, 4, 0, 0],
      },
      emphasis: {
        itemStyle: { color: "#0E42D2" },
      },
      animationDuration: 1500,
    },
  ],
  grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
});

// 学生学习效果 - 水平柱状图（保持原样式）
const learningEffectOption = ref({
  tooltip: {
    trigger: "axis",
    axisPointer: {
      type: "shadow",
    },
    backgroundColor: "rgba(255, 255, 255, 0.9)",
    borderColor: "#eee",
    borderWidth: 1,
    textStyle: { color: "#333" },
    padding: 12,
    boxShadow: "0 3px 10px rgba(0, 0, 0, 0.1)",
  },
  xAxis: {
    type: "value",
    name: "掌握程度 (%)",
    axisLine: { show: false },
    splitLine: { lineStyle: { color: "#f5f5f5" } },
    axisLabel: { color: "#666" },
  },
  yAxis: {
    type: "category",
    data: ["函数", "几何", "代数", "概率"],
    axisLine: { lineStyle: { color: "#eee" } },
    axisLabel: { color: "#666" },
  },
  series: [
    {
      name: "掌握程度",
      type: "bar",
      data: [
        { value: 90, itemStyle: { color: "#165DFF" } },
        { value: 75, itemStyle: { color: "#36D399" } },
        { value: 60, itemStyle: { color: "#FF9F43" } },
        { value: 82, itemStyle: { color: "#722ED1" } },
      ],
      itemStyle: {
        borderRadius: [0, 4, 4, 0], // Rounded corners on the right
      },
      label: {
        show: true,
        position: "right",
        valueAnimation: true,
        formatter: "{c}%",
        color: "#333",
        fontWeight: "bold",
      },
      animationDuration: 1500,
      animationEasing: "quinticInOut",
    },
  ],
  grid: { left: "3%", right: "7%", bottom: "3%", containLabel: true },
});


// 高频错误知识点
const commonErrors = ref([]);

const selectedClass = ref("all");
const classes = ref([
  { value: "all", label: "全校" },
  { value: "class1", label: "2306701班" },
  { value: "class2", label: "2306801班" },
  { value: "class3", label: "2306901班" },
]);

// 表格行样式
const tableRowClassName = ({ row, rowIndex }) => {
  return rowIndex === 0 ? "highlight-row" : "";
};

// 数据获取与更新，添加了动画效果
const fetchData = async () => {
  try {
    commonErrors.value = []; // 清空错误列表

    // 1. 获取使用次数数据
    const usageResponse = await adminService.getUseTimes();
    if (usageResponse.data && usageResponse.data.code === 0) {
      const usageData = usageResponse.data.data;

      // 更新教师使用次数
      usageOption.value.series[0].data = [
        usageData.teacher.week || 0,
        usageData.teacher.today || 0,
      ];

      // 更新学生使用次数
      usageOption.value.series[1].data = [
        usageData.student.week || 0,
        usageData.student.today || 0,
      ];
    } else {
      throw new Error(usageResponse.data?.msg || "获取使用次数失败");
    }

    // 2. 获取高频错误知识点（不过滤错误次数为0）
    const errorsResponse = await adminService.getErrorKnow();
    if (errorsResponse.data && errorsResponse.data.code === 0) {
      // 处理所有知识点数据
      const allErrors = errorsResponse.data.data.map((error) => ({
        point: error.pointName,
        count: error.totalWrongCount || 0,
        rate: error.averageErrorRate || 0,
        mastery: 100 - (error.averageErrorRate || 0) // 掌握程度
      }));

      // 3. 更新学习效果图表（按掌握程度排序）
      const masterySorted = [...allErrors].sort((a, b) => b.mastery - a.mastery);

      // 取前4个知识点（按掌握程度排序）
      const topMastery = masterySorted.slice(0, 4);

      // 更新y轴数据（知识点名称）
      learningEffectOption.value.yAxis.data = topMastery.map(item => item.point);

      // 更新系列数据（掌握程度）
      learningEffectOption.value.series[0].data = topMastery.map((item, index) => {
        const colors = [
          { value: item.mastery, itemStyle: { color: "#165DFF" } },
          { value: item.mastery, itemStyle: { color: "#36D399" } },
          { value: item.mastery, itemStyle: { color: "#FF9F43" } },
          { value: item.mastery, itemStyle: { color: "#722ED1" } },
        ];
        return colors[index] || { value: item.mastery, itemStyle: { color: "#722ED1" } };
      });

      const errorsWithCount = allErrors.filter(error => error.count > 0);
      //过滤错误次数为0
      const countSorted = [...errorsWithCount].sort((a, b) => b.count - a.count);
      commonErrors.value = countSorted.slice(0, 10);
    } else {
      throw new Error(errorsResponse.data?.msg || "获取高频错误失败");
    }

    // 5. 效率数据（没有对应接口，保持模拟数据）
    efficiencyOption.value.series[0].data = [85, 90, 75, 80];
  } catch (error) {
    console.error("获取数据失败:", error);


    usageOption.value.series[0].data = [120, 820];
    usageOption.value.series[1].data = [250, 1500];
    efficiencyOption.value.series[0].data = [85, 90, 75, 80];

    // 学习效果图表后备数据（保持原样式）
    learningEffectOption.value.yAxis.data = ["函数", "几何", "代数", "概率"];
    learningEffectOption.value.series[0].data = [
      { value: 90, itemStyle: { color: "#165DFF" } },
      { value: 75, itemStyle: { color: "#36D399" } },
      { value: 60, itemStyle: { color: "#FF9F43" } },
      { value: 82, itemStyle: { color: "#722ED1" } },
    ];

    // 使用模拟错误数据（按错误次数排序）
    commonErrors.value = [
      { point: "三角函数公式", count: 45, mastery: 55 },
      { point: "虚拟语气", count: 32, mastery: 68 },
      { point: "牛顿第二定律", count: 28, mastery: 72 },
      { point: "化学方程式配平", count: 19, mastery: 81 },
      { point: "逻辑运算符", count: 19, mastery: 81 },
      { point: "等式解题", count: 18, mastery: 82 },
      { point: "矩阵乘法", count: 17, mastery: 83 },
      { point: "矩阵求逆", count: 16, mastery: 84 },
      { point: "矩阵求秩", count: 15, mastery: 85 },
      { point: "矩阵求迹", count: 13, mastery: 87 },
    ].sort((a, b) => b.count - a.count); // 按错误次数降序排序

    ElMessage.error(`获取数据失败: ${error.message || "服务器错误"}`);
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
/* 引入现代无衬线字体 */
@import url("https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap");

.dashboard {
  padding: 0 32px 32px;
  background-color: #f9fafb;
  min-height: 100vh;
  box-sizing: border-box;
  font-family: "Inter", "Helvetica Neue", Helvetica, Arial, sans-serif;
  background-image: radial-gradient(
      at 40% 20%,
      rgba(22, 93, 255, 0.05) 0px,
      transparent 50%
  ),
  radial-gradient(at 80% 0%, rgba(54, 211, 153, 0.05) 0px, transparent 50%),
  radial-gradient(at 0% 50%, rgba(114, 46, 209, 0.05) 0px, transparent 50%);
}

/* 顶部装饰条 */
.top-bar {
  height: 6px;
  background: linear-gradient(90deg, #165dff, #36d399, #722ed1);
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
  color: #1d2939;
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
  border-color: #e5e7eb;
  padding: 0 16px;
  transition: all 0.2s;
}

.el-select .el-input__inner:focus {
  border-color: #165dff;
  box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.1);
}

/* 自定义下拉菜单样式 */
:deep(.custom-select-dropdown) {
  border-radius: 8px;
  border-color: #e5e7eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  padding: 8px 0;
}

:deep(.custom-select-dropdown .el-select-dropdown__item) {
  padding: 12px 16px;
  transition: all 0.2s;
}

:deep(.custom-select-dropdown .el-select-dropdown__item:hover) {
  background-color: #f0f5ff;
  color: #165dff;
}

:deep(.custom-select-dropdown .el-select-dropdown__item.selected) {
  background-color: #e6f7ff;
  color: #165dff;
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
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #165dff, #36d399);
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
  color: #1d2939;
  position: relative;
  padding-left: 12px;
}

.chart-container h3::before {
  content: "";
  position: absolute;
  width: 4px;
  height: 18px;
  background-color: #165dff;
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
  border: 1px solid #f2f4f7;
}

.el-table th {
  background-color: #f9fafb;
  font-weight: 600;
  color: #344054;
  height: 48px;
  padding: 0 16px;
  border-bottom: 1px solid #f2f4f7;
}

.el-table td {
  color: #344054;
  height: 48px;
  padding: 0 16px;
  border-bottom: 1px solid #f2f4f7;
}

/* 高亮显示错误次数最多的行 */
:deep(.highlight-row td) {
  color: #d92d20;
  font-weight: 500;
  background-color: #fef3f2 !important;
}

:deep(.el-table__body tr:hover > td) {
  background-color: #f9fafb !important;
}

/* 页脚样式 */
.page-footer {
  margin-top: 48px;
  padding: 24px 0;
  border-top: 1px solid #f2f4f7;
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
