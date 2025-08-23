<template>
  <el-dialog
      :model-value="visible"
      @update:model-value="$emit('update:modelValue', $event)"
      title="按章节智能生成试卷"
      width="700px"
      :close-on-click-modal="!generating"
      :close-on-press-escape="!generating"
      @close="$emit('close')"
  >
    <el-form
        :model="generateForm"
        :rules="generateRules"
        ref="generateFormRef"
        label-width="120px"
    >
      <el-form-item label="试卷标题" prop="title">
        <el-input v-model="generateForm.title" placeholder="请输入试卷标题" />
      </el-form-item>
      <el-form-item label="考核类型" prop="type">
        <el-select
            v-model="generateForm.type"
            placeholder="请选择考核类型"
            style="width: 100%"
        >
          <el-option label="期中考试" value="期中考试" />
          <el-option label="期末考试" value="期末考试" />
          <el-option label="单元测验" value="单元测验" />
          <el-option label="随堂测试" value="随堂测试" />
        </el-select>
      </el-form-item>
      <el-form-item label="章节范围" prop="chapters">
        <el-select
            v-model="generateForm.chapters"
            multiple
            placeholder="请选择章节"
            style="width: 100%"
            @change="loadKnowledgePoints"
        >
          <el-option
              v-for="chapter in chapters"
              :key="chapter.id"
              :label="chapter.title"
              :value="chapter.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="知识点范围" prop="knowledgePoints">
        <el-select
            v-model="generateForm.knowledgePoints"
            multiple
            placeholder="请选择知识点"
            style="width: 100%"
        >
          <el-option
              v-for="point in knowledgePoints"
              :key="point.id"
              :label="point.title"
              :value="point.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="题目数量" prop="totalQuestions">
        <el-input-number
            v-model="generateForm.totalQuestions"
            :min="1"
            :max="100"
        />
        <span style="margin-left: 8px">道题</span>
      </el-form-item>
    </el-form>

    <el-dialog
        v-model="showPaper"
        title="生成试卷"
        width="800px"
        :close-on-click-modal="false"
    >
      <div v-if="generating" class="cool-loading-wrapper">
        <div class="cool-spinner">
          <svg viewBox="0 0 50 50">
            <circle class="path" cx="25" cy="25" r="20" fill="none" stroke-width="5"/>
          </svg>
          <div class="cool-text">AI正在为你生成试卷...</div>
        </div>
      </div>
      <ExamPaperTeacher
          v-else
          :examContent="generatedExamContent"
      />
      <template #footer>
        <el-button @click="showPaper = false" :disabled="generating">关闭</el-button>
        <el-button @click="showReport = true" :disabled="generating">查看试题报告</el-button>
        <el-button type="primary" @click="handleSaveAndClose" :disabled="generating">保留试卷并关闭</el-button>
        <el-button type="warning" @click="handleRegenerate" :disabled="generating">重新AI生成</el-button>
      </template>
    </el-dialog>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="$emit('close')" :disabled="generating">取消</el-button>
        <el-button
            type="primary"
            @click="generateAssessment"
            :loading="generating"
        >
          {{ generating ? "正在生成中..." : "按章节生成" }}
        </el-button>
      </span>
    </template>

    <!-- 查看报告 -->
    <el-dialog v-model="showReport" title="试题报告" width="800px">
      <div v-html="renderedMarkdown"></div>

      <template #footer>
        <el-button @click="showReport = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import {ref, reactive, computed, watch, onMounted} from 'vue';
import {ElMessage} from 'element-plus';
import {teacherService} from '@/services/api';
import ExamPaperTeacher from '../ExamPaperTeacher.vue';
import {marked} from 'marked';
import DOMPurify from 'dompurify';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  courseId: {
    type: [String, Number],
    default: 1
  }
});

const emit = defineEmits(['update:modelValue', 'close', 'generated']);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const generateFormRef = ref(null);
const generating = ref(false);
const showPaper = ref(false);
const showReport = ref(false);
const generatedExamContent = ref();
const rawReportContent = ref('');
const chapters = ref([]); // 动态章节列表
const knowledgePoints = ref([]); // 动态知识点列表

// 智能生成表单
const generateForm = reactive({
  title: "",
  type: "",
  courseId: props.courseId,
  chapters: [],
  knowledgePoints: [],
  totalQuestions: 20,
});

// 生成表单验证规则
const generateRules = {
  title: [{required: true, message: "请输入试卷标题", trigger: "blur"}],
  type: [{required: true, message: "请选择考核类型", trigger: "change"}],
  courseId: [{required: true, message: "请选择课程", trigger: "change"}],
  chapters: [{required: true, message: "请选择章节", trigger: "change"}],
  knowledgePoints: [{required: true, message: "请选择知识点", trigger: "change"}],
  totalQuestions: [
    {required: true, message: "请输入题目数量", trigger: "blur"},
  ],
};

const generateDescription = computed(() => {
  const typeTag = getTypeTagType(generateForm.type);
  const formattedChapters = generateForm.chapters
      .map((chapterId) => {
        const chapterName = getChapterName(chapterId);
        const knowledgePoints = generateForm.knowledgePoints
            .map(pointId => getKnowledgePointName(pointId))
            .join("、");
        return knowledgePoints.length > 0
            ? `${chapterName}（${knowledgePoints}）`
            : chapterName;
      })
      .join("、");

  return (
      `试卷标题：${generateForm.title} \n` +
      `考试类型：${generateForm.type} \n` +
      `包含章节：${formattedChapters || "无"} \n` +
      `题目数量：${generateForm.totalQuestions}题` +
      `总分值为100分`
  );
});

const loadChapters = async () => {
  if (!props.courseId) return;

  try {
    const response = await teacherService.getChapterByCourseId(props.courseId);
    if (response.data.success) {
      // 将后端数据格式转换为组件需要的格式
      chapters.value = response.data.data.map(chapter => ({
        id: chapter.chapterId,
        title: chapter.chapterName,
        description: chapter.description,
        expanded: false,
        // 保留原始数据以备后用
        originalData: chapter
      }));
    } else {
      ElMessage.error('获取章节数据失败');
    }
  } catch (error) {
    // 如果后端没有数据，使用模拟数据
    console.warn('后端章节数据获取失败，使用模拟数据:', error.message);
    const mockResponse = getMockChapters(props.courseId);
    chapters.value = mockResponse.data;
  }
};

const loadKnowledgePoints = async () => {
  if (!generateForm.chapters.length) return;

  try {
    const promises = generateForm.chapters.map(async (chapterId) => {
      const response = await teacherService.getKnowledgePointsByChapterId(chapterId);
      if (response.data.success) {
        return response.data.data.map(point => ({
          id: point.pointId,
          title: point.pointName,
          description: point.description,
          // 保留原始数据以备后用
          originalData: point
        }));
      } else {
        ElMessage.error(`获取章节${chapterId}的知识点数据失败`);
        return [];
      }
    });

    const results = await Promise.all(promises);
    knowledgePoints.value = results.flat();
  } catch (error) {
    // 如果后端没有数据，使用模拟数据
    console.warn('后端知识点数据获取失败，使用模拟数据:', error.message);
    const mockResponse = getMockKnowledgePoints(props.courseId, generateForm.chapters);
    knowledgePoints.value = mockResponse.data;
  }
};

const renderedMarkdown = computed(() => {
  if (!rawReportContent.value) return '';

  try {
    const rawHtml = marked.parse(rawReportContent.value);
    const cleanHtml = DOMPurify.sanitize(rawHtml);
    return `<div class="markdown-styles">${cleanHtml}</div>`;
  } catch (e) {
    console.error('Markdown 解析失败:', e);
    return `<div class="error">Markdown 解析失败: ${e.message}</div>`;
  }
});

// 设置报告内容
const setReportContent = (content) => {
  rawReportContent.value = content;
};

// 考试类型标签颜色
const getTypeTagType = (type) => {
  const typeMap = {
    期中考试: "primary",
    期末考试: "danger",
    单元测验: "warning",
    实验报告: "success",
    随堂测试: "info",
  };
  return typeMap[type] || "info";
};

// 获取课程章节
const getChapterName = (chapterId) => {
  console.log("chapter", chapterId);
  const chapter = chapters.value.find(ch => ch.id === chapterId);
  return chapter ? chapter.title : `第${chapterId}章`;
};

// 获取知识点名称
const getKnowledgePointName = (pointId) => {
  const point = knowledgePoints.value.find(p => p.id === pointId);
  return point ? point.title : `知识点${pointId}`;
};

// 生成试卷
const generateAssessment = async () => {
  if (!generateFormRef.value) return;
  showPaper.value = true;
  generating.value = true;

  try {
    await generateFormRef.value.validate();

    ElMessage.info("正在根据选择的章节和知识点生成试卷内容...");

    console.log("生成描述:", generateDescription.value, generateForm.courseId)
    const response = await teacherService.assessment.createIntelligentExam(
        generateDescription.value,
        props.courseId
    );

    console.log("生成结果:", response)

    if (response.data && response.data.success) {
      const examPaperJson = JSON.parse(response.data.data.examPaperJson);
      const creationReport = parseCreationReport(response.data.data.creationReport);
      generatedExamContent.value = generateRealExamContent(examPaperJson);
      ElMessage.success("试卷生成成功");

      setReportContent(creationReport);

      // 新增：将新生成的试卷插入assessments列表
      const now = new Date();
      const examPaper = {
        title: generateForm.title,
        questions: convertQuestions(examPaperJson.questions || [])
      };

      const newAssessment = {
        id: response.data.data.examId || Date.now().toString(),
        title: generateForm.title,
        type: generateForm.type,
        status: "draft",
        totalQuestions: examPaperJson.questionCount || generateForm.totalQuestions,
        totalScore: examPaperJson.totalScore || 100,
        totalStudents: 28,
        submittedCount: 0,
        gradedCount: 0,
        createdAt: now.toISOString(),
        lastSubmissionTime: null,
        chapters: [...generateForm.chapters],
        knowledgePoints: [...generateForm.knowledgePoints],
        examPaper,
      };

      emit('generated', newAssessment);
    } else {
      ElMessage.error(response.data?.message || "试卷生成失败");
    }
  } catch (error) {
    console.error("试卷生成失败:", error);
    ElMessage.error("试卷生成失败，请重试");
  } finally {
    generating.value = false;
  }
};

// 兼容ExamAttempt组件的题型字段
function convertQuestions(questions) {
  return questions.map((q, idx) => {
    let type = q.questionType;
    if (type === 'single_choice') type = 'single';
    else if (type === 'multiple_choice') type = 'multiple';
    else if (type === 'true_false') type = 'judge';
    else if (type === 'fill_blank') type = 'blank';
    else if (type === 'short_answer') type = 'short';

    let options = q.options?.map(opt => ({
      value: opt.label || opt.value,
      label: opt.content || opt.label || opt.value
    })) || [];

    return {
      id: q.questionOrder || q.id || idx + 1,
      type,
      title: q.questionContent || q.title,
      score: q.scorePoints || q.score || 0,
      options,
      correctAnswer: q.correctAnswer,
      explanation: q.explanation
    };
  });
}

const generateRealExamContent = (examData) => {
  if (!examData || !examData.questions) return "试卷数据格式错误";

  return examData.questions
      .map((q, idx) => {
        let content = `${idx + 1}. `;

        let questionType = "";
        switch (q.questionType) {
          case "single_choice":
            questionType = "single-choice";
            break;
          case "multiple_choice":
            questionType = "multiple-choice";
            break;
          case "true_false":
            questionType = "true-false";
            break;
          case "fill_blank":
            questionType = "fill-blank";
            break;
          case "short_answer":
            questionType = "short-answer";
            break;
          default:
            questionType = "未知题型";
        }

        content += `[${questionType}] ${q.questionContent}\n`;

        if (q.options && q.options.length > 0) {
          q.options.forEach((opt) => {
            content += `${opt.label}. ${opt.content}\n`;
          });
        }

        if (q.questionType === "fill_blank") {
          content += `答案: ${
              Array.isArray(q.correctAnswer)
                  ? q.correctAnswer.join(", ")
                  : q.correctAnswer
          }\n`;
        } else if (q.questionType === "multiple_choice") {
          content += `正确答案: ${q.correctAnswer.join(", ")}\n`;
        } else {
          content += `正确答案: ${
              Array.isArray(q.correctAnswer)
                  ? q.correctAnswer.join(", ")
                  : q.correctAnswer
          }\n`;
        }

        content += `解析: ${q.explanation}\n\n`;
        return content;
      })
      .join("");
};

// 解析学情报告内容
const parseCreationReport = (report) => {
  if (!report) return "无学情报告数据";
  return report.replace(/\n/g, "<br>");
};

// 监听visible变化，重置表单
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    // 重置表单
    Object.assign(generateForm, {
      title: "",
      type: "",
      courseId: 1,
      chapters: [],
      knowledgePoints: [],
      totalQuestions: 20,
    });
    showPaper.value = false;
    showReport.value = false;
    generatedExamContent.value = null;
    rawReportContent.value = '';
  }
});

// 新增：保留试卷并关闭
function handleSaveAndClose() {
  showPaper.value = false;
  emit('close');
  // 可选：重置表单和状态
  Object.assign(generateForm, {
    title: "",
    type: "",
    courseId: 1,
    chapters: [],
    knowledgePoints: [],
    totalQuestions: 20,
  });
  showReport.value = false;
  generatedExamContent.value = null;
  rawReportContent.value = '';
}

// 新增：重新AI生成
function handleRegenerate() {
  showPaper.value = false;
  // 保留表单内容，允许用户修改参数后再次生成
}

onMounted(() => {
  if (props.courseId) {
    loadChapters();
  }
});
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cool-loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.cool-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cool-spinner svg {
  width: 80px;
  height: 80px;
  animation: rotate 2s linear infinite;
  margin-bottom: 16px;
}

.cool-spinner .path {
  stroke: #409eff;
  stroke-linecap: round;
  animation: dash 1.5s ease-in-out infinite;
}

.cool-text {
  font-size: 18px;
  color: #409eff;
  font-weight: bold;
  letter-spacing: 2px;
  margin-top: 8px;
}

@keyframes rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}
</style>