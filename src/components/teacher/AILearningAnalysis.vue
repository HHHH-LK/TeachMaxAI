<template>
  <div class="ai-analysis-container">
    <!-- 生成中的状态 -->
    <div v-if="isGenerating" class="generating-container">
      <div class="generating-content">
        <div class="loading-spinner">
          <div class="spinner"></div>
        </div>
        <h3>AI正在分析学情数据...</h3>
        <p class="generating-text">{{ generatingText }}</p>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <p class="progress-text">{{ Math.floor(progress) }}%</p>
      </div>
    </div>

    <!-- 分析报告 -->
    <div v-else class="analysis-report">
      <div class="report-header">
        <h2>AI学情分析报告</h2>
        <div class="report-meta">
          <span class="report-date">{{ reportDate }}</span>
        </div>
        <button class="regenerate-btn" @click="regenerateAnalysis">
          <el-icon><Refresh /></el-icon>
          重新生成
        </button>
      </div>

      <div class="report-content" v-html="renderedMarkdown"></div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, computed, watch} from "vue";
import {Refresh} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {marked} from 'marked';
import DOMPurify from 'dompurify';
import {teacherService} from "@/services/api";

const props = defineProps({
  studentData: {type: Object, default: () => ({})},
  courseId: {type: String, default: "1"}
});


console.log(props.courseId, "fjasdklf;");

const courseId = props.courseId
const isGenerating = ref(true);
const progress = ref(0);
const generatingText = ref("正在收集学习数据...");
const reportDate = ref("");
const analysisReport = ref("正在生成中");

// 生成进度模拟步骤
const generatingSteps = [
  "正在收集学习数据...",
  "分析知识点掌握情况...",
  "识别学习薄弱环节...",
  "生成个性化建议...",
  "整理分析报告...",
  "进入生成中..."
];

let stepIndex = 0;
let progressInterval = null;

// 安全提取Markdown文本，防止接口返回对象导致解析失败
function extractMarkdownText(raw) {
  if (!raw) return "";
  if (typeof raw === "string") return raw;
  // 尝试常见字段提取
  if (typeof raw.data === "string") return raw.data;
  if (typeof raw.content === "string") return raw.content;
  if (typeof raw.result === "string") return raw.result;
  // 否则转JSON显示，避免崩溃
  return JSON.stringify(raw, null, 2);
}

// 预处理Markdown，替换可能导致错误的序号或竖线符号
function preprocessMarkdown(md) {
  if (!md) return "";
  return md
      // 把数字序号转成无序列表避免解析报错
      .replace(/^(\s*)\d+\.\s/gm, "$1- ")
      // 避免 ``` 代码块里竖线破坏表格
      .replace(/(```[\s\S]*?```)/g, (m) => m.replace(/\|/g, "｜"))
      // 保证代码块末尾有换行，避免标记错误
      .replace(/```([^\n]*)$/, "```$1\n");
}

// 计算转换并安全渲染Markdown
const renderedMarkdown = computed(() => {
  if (!analysisReport.value) return "";

  try {
    const rawMd = extractMarkdownText(analysisReport.value);
    const safeMd = preprocessMarkdown(rawMd);
    const html = marked.parse(safeMd, {
      gfm: true,
      breaks: true,
      headerIds: false,
      mangle: false,
    });
    // 限制标签和属性，防止XSS
    return DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [
        "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "p", "ul", "ol", "li", "table", "thead", "tbody", "tr", "td", "th",
        "pre", "code", "span", "strong", "em", "blockquote", "hr", "br"
      ],
      ALLOWED_ATTR: ["class", "style", "id"],
    });
  } catch (e) {
    console.error("Markdown解析失败:", e);
    return `<p style="color:red;">Markdown解析失败: ${e.message}</p>`;
  }
});

// 拉取分析数据
async function fetchAnalysis() {

  try {
    const res = await teacherService.getClassAnalysis(courseId);
    if (res.data?.data) {
      analysisReport.value = res.data.data;
    } else {
      analysisReport.value = getDefaultMarkdown();
    }
  } catch (e) {
    console.error("获取学情分析失败:", e);
    analysisReport.value = getDefaultMarkdown();
  }
}

// 进度条逻辑
function startGenerating() {
  isGenerating.value = true;
  progress.value = 0;
  stepIndex = 0;
  generatingText.value = generatingSteps[stepIndex];

  if (progressInterval) clearInterval(progressInterval);
  progressInterval = setInterval(() => {
    // 进度增量随机，模拟生成过程
    progress.value += Math.random() * 15 + 5;

    if (progress.value >= 100) {
      progress.value = 100;
      clearInterval(progressInterval);
      setTimeout(() => {
        isGenerating.value = false;
      }, 1000);
    }

    // 更新生成步骤文本
    const currentStep = Math.floor((progress.value / 100) * (generatingSteps.length - 1));
    if (currentStep !== stepIndex && currentStep < generatingSteps.length) {
      stepIndex = currentStep;
      generatingText.value = generatingSteps[stepIndex];
    }
  }, 300);
}

// 点击重新生成
function regenerateAnalysis() {
  ElMessage.info("正在重新生成分析报告...");
  startGenerating();
  fetchAnalysis();
  updateReportDate();
}

function updateReportDate() {
  const now = new Date();
  reportDate.value = now.toLocaleString("zh-CN", {
    year: "numeric", month: "long", day: "numeric",
    hour: "2-digit", minute: "2-digit"
  });
}

// 默认占位内容
function getDefaultMarkdown() {
  return `
# AI学情分析

## 默认分析报告

正在获取学情数据，以下为示例内容，请稍候：

- 学生在核心课程中表现良好
- 部分知识点需要加强练习

### 建议学习计划

1. 优先复习薄弱知识点
2. 每天坚持练习30分钟
3. 定期进行知识检测

> 请刷新页面重试或联系管理员
  `;
}

onMounted(() => {
  startGenerating();
  fetchAnalysis();
  updateReportDate();
});

watch(() => props.courseId, (newCourseId, oldCourseId) => {
  if (newCourseId && newCourseId !== oldCourseId) {
    console.log("courseId 发生变化，重新生成分析数据", newCourseId);
    ElMessage.info("课程已切换，正在重新生成分析...");
    startGenerating();
    fetchAnalysis();
    updateReportDate();
  }
});
</script>

<style scoped>
.ai-analysis-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

/* 生成中状态样式 */
.generating-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 40px;
}

.generating-content {
  text-align: center;
  max-width: 500px;
}

.loading-spinner {
  margin-bottom: 30px;
}

.spinner {
  width: 60px;
  height: 60px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.generating-content h3 {
  font-size: 24px;
  color: #2c3e50;
  margin-bottom: 15px;
  font-weight: 600;
}

.generating-text {
  font-size: 16px;
  color: #7f8c8d;
  margin-bottom: 30px;
  min-height: 24px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #ecf0f1;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 15px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3498db, #2980b9);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: #7f8c8d;
  font-weight: 500;
}

/* 报告样式 */
.analysis-report {
  padding: 20px 0;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #ecf0f1;
}

.report-header h2 {
  font-size: 28px;
  color: #2c3e50;
  margin: 0;
  font-weight: 700;
}

.report-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.student-name {
  font-size: 18px;
  color: #3498db;
  font-weight: 600;
}

.report-date {
  font-size: 14px;
  color: #7f8c8d;
}

.regenerate-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #1d3fda 0%, #4b6ea2 100%);
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.regenerate-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.report-content {
  line-height: 1.8;
  color: #2c3e50;
}

.report-section {
  margin-bottom: 40px;
  padding: 25px;
  background: #f8f9fa;
  border-radius: 12px;
  border-left: 4px solid #3498db;
}

.report-section h3 {
  font-size: 22px;
  color: #2c3e50;
  margin-bottom: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}

.report-section h4 {
  font-size: 18px;
  color: #34495e;
  margin: 25px 0 15px 0;
  font-weight: 600;
}

.report-section p {
  margin-bottom: 15px;
  font-size: 16px;
  color: #2c3e50;
}

.report-section ul,
.report-section ol {
  margin: 15px 0;
  padding-left: 25px;
}

.report-section li {
  margin-bottom: 10px;
  font-size: 15px;
  color: #2c3e50;
}

.report-section strong {
  color: #e74c3c;
  font-weight: 600;
}

.report-summary {
  margin-top: 40px;
  padding: 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.report-summary h3 {
  font-size: 24px;
  margin-bottom: 20px;
  font-weight: 600;
}

.report-summary p {
  font-size: 16px;
  line-height: 1.8;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-analysis-container {
    padding: 15px;
  }

  .report-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .report-meta {
    align-items: flex-start;
  }

  .report-section {
    padding: 20px;
  }

  .report-section h3 {
    font-size: 20px;
  }
}
</style>
