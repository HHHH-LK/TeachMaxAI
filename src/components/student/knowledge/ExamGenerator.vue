<template>
  <div class="exam-generator">
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h2>AI生成试题</h2>
    </div>
    
    <!-- 炫酷加载动画 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-container">
        <!-- 粒子效果背景 -->
        <div class="particles">
          <div v-for="i in 30" :key="i" class="particle" :style="getParticleStyle(i)"></div>
        </div>
        
        <!-- 主加载动画 -->
        <div class="loading-content">
          <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
          </div>
          
          <div class="loading-progress">
            <span class="loading-title">AI正在生成试题……</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressWidth + '%' }"></div>
            </div>
            <span class="progress-text">{{ progressText }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 主要内容区域 -->
    <div v-else>
      <div class="dialog-box">
        <p class="dialog-description">
          请选择章节和知识点，或在下方输入您希望AI生成试题的描述。
        </p>

        <div class="selection-area">
          <div class="selection-group" :aria-disabled="loading">
            <label class="selection-label">选择章节 (单选):</label>
            <div class="radio-group" :aria-disabled="loading">
              <label
                  v-for="chapter in chapters"
                  :key="chapter.id"
                  class="radio-item"
              >
                <input
                    type="radio"
                    :value="chapter.id"
                    v-model="selectedChapter"
                    name="chapter"
                    @change="onChapterChange(chapter)"
                    :disabled="loading"
                />
                {{ chapter.name }}
              </label>
            </div>
          </div>

          <div class="selection-group" :aria-disabled="loading">
            <label class="selection-label">选择知识点 (可多选):</label>
            <div class="checkbox-group knowledge-points-group" :aria-disabled="loading">
              <label
                  v-for="kp in availableKnowledgePoints"
                  :key="kp.id"
                  class="checkbox-item"
              >
                <input
                    type="checkbox"
                    :value="kp.id"
                    v-model="selectedKnowledgePoints"
                    :disabled="loading"
                />
                {{ kp.name }}
              </label>
              <span
                  v-if="availableKnowledgePoints.length === 0 && selectedChapter"
                  class="no-data-hint"
              >当前章节无知识点</span
              >
              <span v-if="!selectedChapter" class="no-data-hint"
              >请先选择章节</span
              >
            </div>
          </div>
        </div>

        <textarea
            v-model="description"
            placeholder="请输入试题描述... (必选)"
            class="description-input"
            :disabled="loading"
        ></textarea>
        <button @click="generateExam" class="generate-btn" :disabled="loading">生成试题</button>
      </div>

      <ExamPaper
          v-if="examContent"
          :examContent="examContent"
          :courseId="props.courseId"
          :chapterId="chapterNumber"
          :minIds="minIds"
          :maxIds="maxIds"
          submitText="提交试题"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, defineEmits, computed, onMounted, defineProps, watch, onBeforeUnmount } from "vue";
import ExamPaper from "../../ExamPaper.vue";
import { studentService } from "@/services/api";
import { ElMessage } from "element-plus";

const emit = defineEmits(["back"]);
const description = ref("");
const examContent = ref("");
const loading = ref(false);
const error = ref(null);
const chapterNumber = ref('');
const dotIndex = ref(0);
const progressWidth = ref(0);
const progressText = ref('初始化中...');

// 接收父组件传递的courseId
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
    { width: 15, text: '分析题目要求...' },
    { width: 30, text: '选择合适知识点...' },
    { width: 50, text: '生成题目内容...' },
    { width: 70, text: '设置答案选项...' },
    { width: 85, text: '生成解析说明...' },
    { width: 95, text: '即将完成...' }
  ];
  
  let stepIndex = 0;
  progressInterval = setInterval(() => {
    if (!isMounted || !loading.value) return;
    
    if (stepIndex < steps.length) {
      progressWidth.value = steps[stepIndex].width;
      progressText.value = steps[stepIndex].text;
      stepIndex++;
    }
  }, 1000);
};

// 动态点动画
const startDotAnimation = () => {
  dotInterval = setInterval(() => {
    if (!isMounted || !loading.value) return;
    dotIndex.value = (dotIndex.value + 1) % 3;
  }, 500);
};

// 模拟章节和知识点数据
const chapters = ref(["正在加载中"]);

// 章节改为单选（存储选中的单个章节ID）
const selectedChapter = ref("");
const selectedKnowledgePoints = ref([]);
const minIds = ref("");
const maxIds = ref("");
// 根据选中的单个章节计算可用知识点
const availableKnowledgePoints = computed(() => {
  if (!selectedChapter.value) return [];
  const chapter = chapters.value.find((c) => c.id === selectedChapter.value);
  return chapter ? chapter.knowledgePoints : [];
});

// 章节切换时清空已选知识点
const onChapterChange = () => {
  selectedKnowledgePoints.value = [];
};

const generateExam = async () => {
  // console.log("选择的章节",selectedChapter.value);
  // console.log("选择的知识点", selectedKnowledgePoints.value)
  // console.log("课程", props.courseId)
  // console.log("描述", description.value)

  const StringParams = {
    courseId: String(props.courseId),
    chapterId: String(selectedChapter.value),
    decprection: String(description.value) || "帮我生成一套合理的试题,共计10题",
  };
  chapterNumber.value = String(selectedChapter.value);

  loading.value = true;
  
  // 启动动画
  startDotAnimation();
  updateProgress();
  
  try {
    ElMessage.success("正在生成中，请勿刷新页面");
    const response = await studentService.generateExamByKnowledgePoints(
        selectedKnowledgePoints.value,
        StringParams
    );
    
    if (!isMounted) return;
    
    try {
      const jsonStrWithIds = response.data.data;
      const idsRegex = /ids:min:(\d+),max:(\d+)$/;
      const match = jsonStrWithIds.match(idsRegex);

      // 使用这两个 const 变量保存结果
      minIds.value = match ? parseInt(match[1]) : null;
      maxIds.value = match ? parseInt(match[2]) : null;
      console.log("收到的id", minIds, maxIds);
      // 解析response.data.data，然后生成试卷内容
      const jsonStr = response.data.data
          .replace(/ids:min:\d+,max:\d+$/, "")
          .trim();
      const examData = JSON.parse(jsonStr);
      examContent.value = generateRealExamContent(examData);
      console.log("组卷结果", examData);
    } catch (e) {
      console.error("解析试卷数据失败", e);
    }
  } catch (e) {
    console.error("解析试卷数据失败", e);
  } finally {
    if (!isMounted) return;
    
    // 完成进度
    progressWidth.value = 100;
    progressText.value = '试题生成完成！';
    
    // 延迟关闭加载状态，让用户看到完成效果
    setTimeout(() => {
      if (isMounted) {
        loading.value = false;
      }
    }, 500);
  }

  let prompt = description.value;
  // 处理选中的章节
  if (selectedChapter.value) {
    const chapter = chapters.value.find((c) => c.id === selectedChapter.value);
    const chapterName = chapter ? chapter.name : "";
    prompt = `请生成关于 ${chapterName} 章节的试题。` + prompt;
  }
  // 处理选中的知识点
  if (selectedKnowledgePoints.value.length > 0) {
    const kpNames = selectedKnowledgePoints.value
        .map((id) => {
          for (const chapter of chapters.value) {
            const kp = chapter.knowledgePoints.find((k) => k.id === id);
            if (kp) return kp.name;
          }
          return null;
        })
        .filter(Boolean);
    prompt = `${prompt} 重点关注知识点：${kpNames.join("、")}。`;
  }

  // examContent.value = `根据您的要求，生成以下试题：\n\n${prompt}\n\n1. [single-choice] 以下哪个是Java的关键字？\nA. class\nB. Class\nC. object\nD. Object\nAnswer: A\nExplanation: class是Java中定义类的关键字。\n\n2. [multiple-choice] 哪些是面向对象编程的特性？\nA. 封装\nB. 继承\nC. 多态\nD. 抽象\nAnswer: A,B,C,D\nExplanation: 面向对象编程的四大特性是封装、继承、多态和抽象。\n\n3. [fill-in-blank] Java中的基本数据类型包括整型、浮点型、字符型和______。\nAnswer: 布尔型\nExplanation: Java的基本数据类型包括byte, short, int, long, float, double, char, boolean。\n\n4. [true-false] Java中所有类都直接或间接继承自Object类。\nAnswer: A\nExplanation: Object类是所有Java类的根类。\n\n5. [short-answer] 请简述Java中接口和抽象类的区别。\nAnswer: 接口和抽象类都可以定义抽象方法，但接口只能包含抽象方法和常量，不能有实例变量和具体方法（Java 8以后可以有default和static方法）。抽象类可以包含抽象方法、具体方法、实例变量和构造器。一个类可以实现多个接口，但只能继承一个抽象类。\nExplanation: 接口主要用于定义行为规范，抽象类主要用于代码复用和模板方法模式。\n`;
};

const generateRealExamContent = (examData) => {
  if (!examData || !examData.questions) return "试卷数据格式错误";

  return examData.questions
      .map((q, idx) => {
        let content = `${idx + 1}. `;

        // 修复内容显示 - 处理含换行符的问题
        const cleanedContent = q.question_content.replace(/\\\\n/g, "\n");

        // 添加多选题支持
        content += `[${
            q.question_type === "single_choice"
                ? "single-choice"
                : q.question_type === "multiple_choice"  // 新增多选题
                    ? "multiple-choice"
                    : q.question_type === "true_false"
                        ? "true-false"
                        : "fill-in-blank"
        }] ${cleanedContent}\n`;

        // 显示选项（适用于单选、多选和判断题）
        if (q.question_options) {
          q.question_options.forEach((opt) => {
            content += `${opt.label}. ${opt.content}\n`;
          });
        }

        // 填空题的特殊处理
        if (q.question_type === "fill_blank") {
          const cleanedAnswer = q.correct_answer.replace(/\\\\n/g, "\n");
          content += `答案: ${cleanedAnswer}\n`;
        }
        // 多选题的特殊处理 - 显示多个正确答案
        else if (q.question_type === "multiple_choice") {
          // 将逗号分隔的答案转换为选项标签列表
          // const answerLabels = cleanedAnswer.split(',').map(a => a.trim()).join(', ');
          content += `正确答案: ${q.correct_answer}\n`;
        }
        // 其他题型（单选、判断）
        else {
          const cleanedAnswer = q.correct_answer.replace(/\\\\n/g, "\n");
          content += `正确答案: ${cleanedAnswer}\n`;
        }

        // 处理含换行符的解析说明
        const cleanedExplanation = q.explanation.replace(/\\\\n/g, "\n");
        content += `解析: ${cleanedExplanation}\n\n`;

        return content;
      })
      .join("");
};

//获取数据
const fetchCourseCatalog = async () => {
  const knowledge = ref([]); //存储知识点信息
  const allKnowledge = [];
  try {
    loading.value = true;
    error.value = null;

    console.log(props.courseId);
    // 调用API获取课程目录
    const responseChapter = await studentService.getChapterInfo(props.courseId);
    // console.log(response.data.data)
    if (responseChapter.data) {
      chapters.value = responseChapter.data.data.map((chapter) => ({
        id: chapter.chapterId,
        name: chapter.chapterName,
        knowledgePoints: [],
      }));
    }
    console.log("chapter", chapters.value[0].id);
    console.log("length", chapters.value.length);
    for (var i = 0; i < chapters.value.length; i++) {
      const responseKnw = await studentService.getChapterKnow(
          chapters.value[i].id
      );
      if (responseKnw.data) {
        const chapterKnowledge = responseKnw.data.data.map((knowledge) => ({
          id: knowledge.pointId,
          chapterId: chapters.value[i].id,
          name: knowledge.pointName,
        }));
        chapters.value[i].knowledgePoints = chapterKnowledge;
        allKnowledge.push(...chapterKnowledge);
      }
    }
    console.log("chapters", chapters.data);
    knowledge.value = allKnowledge;
  } catch (err) {
    console.error("获取课程目录失败:", err);
    error.value = "加载课程目录失败";
  } finally {
    loading.value = false;
  }
};

// 监听courseId的变化
watch(
    () => props.courseId,
    (newCourseId) => {
      if (newCourseId) {
        fetchCourseCatalog();
      }
    },
    { immediate: true }
); // 立即执行一次

const goBack = () => {
  emit('back');
};

onBeforeUnmount(() => {
  isMounted = false;
  if (dotInterval) {
    clearInterval(dotInterval);
  }
  if (progressInterval) {
    clearInterval(progressInterval);
  }
});
</script>

<style scoped>
.exam-generator {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
  background: linear-gradient(to bottom right, #f5f7fa, #e4ebf5);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  font-family: "Segoe UI", "Roboto", "Helvetica Neue", sans-serif;
  position: relative;
  min-height: 400px;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid #ddd;
  padding-bottom: 12px;
}

.header h2 {
  flex-grow: 1;
  text-align: center;
  font-size: 30px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.back-btn {
  background: transparent;
  border: none;
  color: #4b5563;
  font-size: 16px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background-color: #e0e7ff;
  color: #1d4ed8;
}

.dialog-box {
  background: #ffffff;
  padding: 35px;
  border-radius: 14px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.05);
  margin-bottom: 35px;
  border: 1px solid #e5e7eb;
}

.dialog-description {
  font-size: 16px;
  color: #374151;
  margin-bottom: 20px;
  line-height: 1.7;
  text-align: center;
}

.selection-area {
  display: flex;
  gap: 25px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.selection-group {
  flex: 1;
  min-width: 300px;
  background-color: #f0f4f8;
  padding: 20px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
}

.selection-group[aria-disabled="true"],
.radio-group[aria-disabled="true"],
.checkbox-group[aria-disabled="true"] {
  opacity: 0.5;
  pointer-events: none;
  filter: grayscale(60%);
}

.selection-label {
  display: block;
  font-size: 15px;
  font-weight: bold;
  color: #2d3748;
  margin-bottom: 15px;
  border-bottom: 2px solid #cbd5e1;
  padding-bottom: 8px;
}

/* 单选按钮组样式 */
.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 10px;
}

.radio-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #4a5568;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s ease;
}

.radio-item:hover {
  color: #2563eb;
}

.radio-item input[type="radio"] {
  margin-right: 8px;
  accent-color: #2563eb;
  width: 16px;
  height: 16px;
  cursor: pointer;
}

/* 复选框组样式（知识点） */
.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 10px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #4a5568;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s ease;
}

.checkbox-item:hover {
  color: #2563eb;
}

.checkbox-item input[type="checkbox"] {
  margin-right: 8px;
  accent-color: #2563eb;
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.knowledge-points-group {
  border-top: 1px dashed #cbd5e1;
  padding-top: 15px;
  margin-top: 5px;
}

.no-data-hint {
  font-size: 13px;
  color: #718096;
  font-style: italic;
  width: 100%;
  text-align: center;
  padding: 10px 0;
}

.description-input {
  width: 100%;
  min-height: 120px;
  padding: 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  resize: vertical;
  font-size: 16px;
  line-height: 1.6;
  transition: all 0.3s ease;
  background-color: #f9fafb;
  margin-top: 25px;
}

.description-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.25);
  outline: none;
}

.description-input:disabled {
  background: #f1f1f1;
  color: #b0b0b0;
  cursor: not-allowed;
  opacity: 0.7;
}

.generate-btn {
  background: linear-gradient(to right, #2563eb, #3b82f6);
  color: white;
  border: none;
  padding: 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 18px;
  font-weight: bold;
  margin-top: 15px;
  width: 100%;
  transition: background 0.3s, transform 0.2s;
}

.generate-btn:hover {
  background: linear-gradient(to right, #1d4ed8, #2563eb);
  transform: translateY(-2px);
}

.generate-btn:active {
  transform: translateY(0);
}

.generate-btn:disabled {
  background: #b0b0b0 !important;
  color: #fff !important;
  cursor: not-allowed !important;
  opacity: 0.7;
}

.exam-paper {
  background: #ffffff;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  border: 1px solid #e5e7eb;
}

.exam-paper h3 {
  font-size: 24px;
  color: #1f2937;
  margin-bottom: 25px;
  text-align: center;
}

.exam-content {
  white-space: pre-wrap;
  background-color: #f3f4f6;
  padding: 24px;
  border-radius: 10px;
  border: 1px dashed #cbd5e1;
  font-family: "Segoe UI", "Roboto", sans-serif;
  color: #374151;
  line-height: 1.8;
  font-size: 16px;
}

/* 加载动画样式 */
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
  border-radius: 16px;
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
  width: clamp(1400px, 60vw, 400px);
  min-height: 400px;
  max-height: 60vh;
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
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
    width: 85%;
    min-height: 250px;
    max-height: 50vh;
    padding: 25px 15px;
  }
  
  .loading-content {
    gap: 15px;
  }
  
  .spinner-ring {
    border-width: 3px;
  }
  
  .loading-progress {
    max-width: 300px;
  }
}

@media (max-width: 480px) {
  .loading-container {
    width: 90%;
    min-height: 200px;
    max-height: 40vh;
    padding: 20px 10px;
  }
  
  .loading-content {
    gap: 12px;
  }
  
  .spinner-ring {
    border-width: 2px;
  }
  
  .loading-progress {
    max-width: 250px;
  }
  
  .particles {
    display: none; /* 在小屏幕上隐藏粒子效果以提升性能 */
  }
}

@media (max-height: 600px) {
  .loading-container {
    min-height: 180px;
    max-height: 50vh;
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
