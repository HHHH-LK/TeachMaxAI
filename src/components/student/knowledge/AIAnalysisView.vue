<template>
  <div class="ai-analysis-container">
    <div class="header">
      <button @click="goBack" class="back-button">← 返回</button>
      <h2>AI学情分析</h2>
    </div>
    
    <!-- 使用加载动画 -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-container">
        <!-- 粒子效果背景————修改数量 -->
        <div class="particles">
          <div v-for="i in 25" :key="i" class="particle" :style="getParticleStyle(i)"></div>
        </div>
        
        <!-- 主加载动画 -->
        <div class="loading-content">
          <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
          </div>
          
          <div class="loading-progress">
            <span class="loading-title">AI正在分析您的学习数据……</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressWidth + '%' }"></div>
            </div>
            <span class="progress-text">{{ progressText }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Markdown 渲染区域优化 -->
    <div v-else class="markdown-content" v-html="renderedMarkdown"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { studentService } from '@/services/api';

const emit = defineEmits(['back']);
const markdownContent = ref('');
const isLoading = ref(false);
const error = ref(null);
const dotIndex = ref(0);
const progressWidth = ref(0);
const progressText = ref('初始化中...');

const props = defineProps({
  courseId: {
    type: Number,
    required: true,
  },
});

let isMounted = true;
let dotInterval = null;
let progressInterval = null;

// 粒子样式生成
const getParticleStyle = (index) => {
  const delay = Math.random() * 3;
  const duration = 2 + Math.random() * 2;
  const size = 2 + Math.random() * 4;
  const left = Math.random() * 100;
  const top = Math.random() * 100;
  
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  };
};

// 动态进度更新
const updateProgress = () => {
  const steps = [
    { width: 20, text: '连接服务器...' },
    { width: 40, text: '获取学习数据...' },
    { width: 60, text: 'AI分析中...' },
    { width: 80, text: '生成报告...' },
    { width: 95, text: '即将完成...' }
  ];
  
  let stepIndex = 0;
  progressInterval = setInterval(() => {
    if (!isMounted || !isLoading.value) return;
    
    if (stepIndex < steps.length) {
      progressWidth.value = steps[stepIndex].width;
      progressText.value = steps[stepIndex].text;
      stepIndex++;
    }
  }, 800);
};

// 动态点动画
const startDotAnimation = () => {
  dotInterval = setInterval(() => {
    if (!isMounted || !isLoading.value) return;
    dotIndex.value = (dotIndex.value + 1) % 3;
  }, 500);
};

onMounted(async () => {
  isLoading.value = true;
  error.value = null;
  
  // 启动动画
  startDotAnimation();
  updateProgress();
  
  try {
    const courseId = 2;
    const response = await studentService.getCourseAnalysis(props.courseId);
    
    if (!isMounted) return;
    
    if (response?.data?.code === 0 && response.data.data) {
      markdownContent.value = response.data.data;
    } else {
      throw new Error(response?.data?.message || 'API返回空数据');
    }
    console.log(markdownContent.value);
  } catch (err) {
    if (!isMounted) return;
    
    console.error('获取课程分析失败:', err);
    error.value = err;
    markdownContent.value = getDefaultMarkdown();
  } finally {
    if (!isMounted) return;
    
    // 完成进度
    progressWidth.value = 100;
    progressText.value = '分析完成！';
    
    // 延迟关闭加载状态，让用户看到完成效果
    setTimeout(() => {
      if (isMounted) {
        isLoading.value = false;
      }
    }, 500);
  }
});

onBeforeUnmount(() => {
  isMounted = false;
  if (dotInterval) clearInterval(dotInterval);
  if (progressInterval) clearInterval(progressInterval);
});

// 计算属性：将 Markdown 转换为安全的 HTML
const renderedMarkdown = computed(() => {
  if (!markdownContent.value) return '';
  
  try {
    const rawHtml = marked.parse(markdownContent.value);
    const cleanHtml = DOMPurify.sanitize(rawHtml);
    return `<div class="markdown-styles">${cleanHtml}</div>`;
  } catch (e) {
    console.error('Markdown 解析失败:', e);
    return `<div class="error">Markdown 解析失败: ${e.message}</div>`;
  }
});

const goBack = () => {
  emit('back');
};

// 默认 Markdown 内容
const getDefaultMarkdown = () => `
# AI学情分析

## 默认分析报告

由于正在获取到您的学情数据，以下是示例分析报告，请耐心等待：

### 学习现状总览
- 学生在核心课程中表现良好
- 部分知识点需要加强练习

### 建议学习计划
1. 优先复习薄弱知识点
2. 每天坚持练习30分钟
3. 定期进行知识检测

> 请刷新页面重试或联系管理员
`;
</script>

<style scoped>
.ai-analysis-container {
  padding: clamp(20px, 4vw, 40px) clamp(15px, 3vw, 20px);
  max-width: 1200px;
  margin: 0 auto;
  background: linear-gradient(to bottom right, #f5f7fa, #e4ebf5);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  font-family: 'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif;
  color: #333;
  min-height: 400px;
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: clamp(20px, 4vw, 30px);
  border-bottom: 1px solid #ddd;
  padding-bottom: 12px;
  flex-shrink: 0;
}

.header h2 {
  flex-grow: 1;
  text-align: center;
  font-size: clamp(24px, 4vw, 30px);
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.back-button {
  background: transparent;
  border: none;
  color: #4b5563;
  font-size: 16px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.back-button:hover {
  background-color: #e0e7ff;
  color: #1d4ed8;
}

.analysis-section {
  background: #ffffff;
  padding: 35px;
  border-radius: 14px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
  margin-bottom: 35px;
  border: 1px solid #e5e7eb;
}

.analysis-section h4 {
  font-size: 24px;
  color: #2563eb;
  margin-bottom: 20px;
  border-bottom: 2px solid #e0e7ff;
  padding-bottom: 10px;
}

.analysis-section h5 {
  font-size: 18px;
  color: #1f2937;
  margin-top: 25px;
  margin-bottom: 15px;
}

.analysis-section p {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 15px;
  color: #374151;
}

.analysis-section ul,
.analysis-section ol {
  margin-bottom: 15px;
  padding-left: 25px;
}

.analysis-section li {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 8px;
  color: #374151;
}

.summary-text {
  font-size: 18px;
  font-weight: bold;
  color: #2563eb;
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px dashed #e0e7ff;
}

/* Markdown内容区域样式 */
.markdown-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  margin-top: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.markdown-content :deep(.markdown-styles) {
  line-height: 1.6;
  color: #374151;
}

.markdown-content :deep(.markdown-styles h1) {
  font-size: clamp(24px, 4vw, 32px);
  margin-bottom: 20px;
  color: #1f2937;
}

.markdown-content :deep(.markdown-styles h2) {
  font-size: clamp(20px, 3.5vw, 28px);
  margin: 25px 0 15px 0;
  color: #1f2937;
}

.markdown-content :deep(.markdown-styles h3) {
  font-size: clamp(18px, 3vw, 24px);
  margin: 20px 0 12px 0;
  color: #1f2937;
}

.markdown-content :deep(.markdown-styles p) {
  font-size: clamp(14px, 2.5vw, 16px);
  margin-bottom: 15px;
}

.markdown-content :deep(.markdown-styles ul),
.markdown-content :deep(.markdown-styles ol) {
  padding-left: 20px;
  margin-bottom: 15px;
}

.markdown-content :deep(.markdown-styles li) {
  font-size: clamp(14px, 2.5vw, 16px);
  margin-bottom: 8px;
}

/* 新增加载动画样式 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(240, 245, 255, 0.95));
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(10px);
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.loading-container {
  position: relative;
  width: 100%;
  max-width: 1400px;
  min-height: 400px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f6f9fc, #eef2f6);
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: containerFloat 3s ease-in-out infinite;
  padding: 40px 20px;
}

@keyframes containerFloat {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

.particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: -1;
}

.particle {
  position: absolute;
  background: rgba(134, 142, 240, 0.3);
  border-radius: 50%;
  opacity: 0.7;
  transform: translate(-50%, -50%);
  animation: float 3s infinite ease-in-out;
}

@keyframes float {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.6;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.8;
  }
  100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.6;
  }
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  position: relative;
  gap: 20px;
}

.loading-spinner {
  position: relative;
  width: clamp(60px, 8vw, 100px);
  height: clamp(60px, 8vw, 100px);
  margin-bottom: 10px;
}

.spinner-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 4px solid transparent;
  border-radius: 50%;
  animation: spin 1.5s linear infinite;
}

.spinner-ring:nth-child(1) {
  border-top-color: #4f46e5;
  border-left-color: #4f46e5;
  border-bottom-color: #4f46e5;
  animation-delay: 0s;
}

.spinner-ring:nth-child(2) {
  border-top-color: #3b82f6;
  border-left-color: #3b82f6;
  border-bottom-color: #3b82f6;
  animation-delay: 0.2s;
}

.spinner-ring:nth-child(3) {
  border-top-color: #10b981;
  border-left-color: #10b981;
  border-bottom-color: #10b981;
  animation-delay: 0.4s;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-title {
  font-size: clamp(18px, 3vw, 24px);
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 10px;
  text-align: center;
  line-height: 1.4;
}

.loading-progress {
  width: 100%;
  max-width: 400px;
  margin-top: 10px;
  text-align: center;
}

.progress-bar {
  width: 100%;
  height: 10px;
  background-color: #e5e7eb;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 5px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(to right, #4f46e5, #3b82f6, #10b981);
  border-radius: 5px;
  transition: width 0.3s ease-in-out;
  position: relative;
  overflow: hidden;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

.progress-text {
  font-size: clamp(14px, 2.5vw, 16px);
  color: #6b7280;
  font-weight: 500;
  animation: textGlow 2s ease-in-out infinite alternate;
  margin-top: 8px;
}

@keyframes textGlow {
  from {
    text-shadow: 0 0 5px rgba(79, 70, 229, 0.3);
  }
  to {
    text-shadow: 0 0 10px rgba(79, 70, 229, 0.6);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-container {
    width: 95%;
    min-height: 350px;
    max-height: 70vh;
    padding: 30px 15px;
  }
  
  .loading-content {
    gap: 15px;
  }
  
  .spinner-ring {
    border-width: 3px;
  }
  
  .loading-progress {
    max-width: 350px;
  }
}

@media (max-width: 480px) {
  .loading-container {
    width: 98%;
    min-height: 300px;
    max-height: 60vh;
    padding: 20px 10px;
  }
  
  .loading-content {
    gap: 12px;
  }
  
  .spinner-ring {
    border-width: 2px;
  }
  
  .loading-progress {
    max-width: 300px;
  }
  
  .particles {
    display: none; /* 在小屏幕上隐藏粒子效果以提升性能 */
  }
}

@media (max-height: 600px) {
  .loading-container {
    min-height: 250px;
    max-height: 90vh;
    padding: 20px 15px;
  }
  
  .loading-content {
    gap: 10px;
  }
  
  .loading-spinner {
    width: clamp(50px, 6vw, 80px);
    height: clamp(50px, 6vw, 80px);
  }
  
  .loading-title {
    font-size: clamp(16px, 2.5vw, 20px);
  }
}
</style>