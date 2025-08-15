<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:modelValue', $event)"
    title="智能阅卷"
    width="1200px"
    :before-close="handleClose"
  >
    <div v-if="assessment" class="grading-content">
      <div class="grading-header">
        <h3>{{ assessment.title }} - 智能阅卷</h3>
        <div class="grading-stats">
          <el-tag type="info">总提交: {{ totalSubmissions }}</el-tag>
          <el-tag type="warning">待阅卷: {{ pendingGradingCount }}</el-tag>
          <el-tag type="success">已阅卷: {{ gradedCount }}</el-tag>
        </div>
        <div class="grading-actions-header">
          <el-button
            type="success"
            @click="autoGradeAll"
            :loading="autoGradingLoading"
          >
            <el-icon><Star /></el-icon>
            一键智能阅卷
          </el-button>
          <el-button
            type="primary"
            @click="saveAllGrading"
            :loading="savingLoading"
          >
            <el-icon><Check /></el-icon>
            保存所有评分
          </el-button>
        </div>
      </div>

      <div class="grading-progress">
        <el-progress
          :percentage="gradingProgress"
          :format="(format) => `${format}%`"
          status="success"
        />
      </div>

      <!-- 学生列表 -->
      <div class="students-list">
        <el-table :data="submissions" style="width: 100%" v-loading="loading">
          <el-table-column prop="studentName" label="学生姓名" width="120" />
          <el-table-column prop="studentNumber" label="学号" width="120" />
          <el-table-column prop="submitTime" label="提交时间" width="180" />
          <el-table-column label="阅卷状态" width="120">
            <template #default="scope">
              <el-tag :type="getGradingStatusType(scope.row)">
                {{ getGradingStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="总分" width="100">
            <template #default="scope">
              <span v-if="scope.row.score !== null">
                {{ scope.row.score }}/{{ assessment.totalScore }}
              </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewSubmissionDetail(scope.row)"
                >查看详情</el-button
              >
            </template>
          </el-table-column>
          <el-table-column label="智能操作" width="220">
            <template #default="scope">
              <el-button
                size="small"
                type="primary"
                @click="aiGradeSingleStudent(scope.row)"
                :loading="scope.row.aiLoading"
                >智能阅卷</el-button
              >
              <el-button
                size="small"
                type="success"
                @click="generateReport(scope.row)"
                :loading="scope.row.reportLoading"
                >生成报告</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </el-dialog>

  <!-- 学生详情对话框 -->
  <SubmissionDetailModal
    v-model="showSubmissionDetail"
    :submission="selectedSubmissionDetail"
    :assessment="assessment"
    @save="$emit('save-submission', $event)"
    @ai-grade="aiGradeSingleStudent"
    @ai-grade-question="aiGradeSingleQuestion"
  />

  <el-dialog
    v-model="showReportDialog"
    title="学情报告"
    width="500px"
    :show-close="true"
  >
    <pre style="white-space: pre-wrap; word-break: break-all">{{
      currentReportText
    }}</pre>
    <template #footer>
      <el-button @click="showReportDialog = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Star, Check } from "@element-plus/icons-vue";
import { ElDialog } from "element-plus";
import { teacherService } from "@/services/api";
import SubmissionDetailModal from "./SubmissionDetailModal.vue";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  assessment: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(["update:modelValue", "close", "save-submission"]);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const loading = ref(false);
const submissions = ref([]);
const showSubmissionDetail = ref(false);
const selectedSubmissionDetail = ref(null);
const autoGradingLoading = ref(false);
const savingLoading = ref(false);
const showReportDialog = ref(false);
const currentReportText = ref("");

// 计算属性
const totalSubmissions = computed(() => submissions.value.length);

const pendingGradingCount = computed(() => {
  if (!props.assessment) return 0;
  return props.assessment.submittedCount - props.assessment.gradedCount;
});

const gradedCount = computed(() => {
  if (!props.assessment) return 0;
  return props.assessment.gradedCount;
});

const gradingProgress = computed(() => {
  if (totalSubmissions.value === 0) return 0;
  const gradedSubmissions = submissions.value.filter((s) => s.isGraded).length;
  return Math.round((gradedSubmissions / totalSubmissions.value) * 100);
});

// 获取阅卷状态类型
const getGradingStatusType = (submission) => {
  if (
    !submission.isGraded &&
    submission.gradedCount >= submission.totalQuestions
  ) {
    return "success";
  } else if (submission.gradedCount > 0) {
    return "warning";
  } else {
    return "info";
  }
};

// 获取阅卷状态文本
const getGradingStatusText = (submission) => {
  if (
    submission.isGraded &&
    submission.gradedCount >= submission.totalQuestions
  ) {
    return "已阅卷";
  } else if (submission.gradedCount > 0) {
    return "部分阅卷";
  } else {
    return "待阅卷";
  }
};

// 查看学生详情
const viewSubmissionDetail = async (submission) => {
  try {
    // 设置加载状态
    submission.detailLoading = true;

    // 调用API获取考试详情
    const response = await teacherService.assessment.getSubmissionDetail(
      props.assessment.id,
      submission.studentId
    );

    if (response.data && response.data.success) {
      const examData = response.data.data;

      // 转换数据结构
      const transformedSubmission = {
        ...submission,
        answers: examData.map((answer) => ({
          questionId: answer.questionId,
          question: answer.questionContent,
          type: answer.questionTypeCn, 
          score: answer.scorePoints,
          studentAnswer: answer.studentAnswer,
          correctAnswer: answer.correctAnswer.answer, 
          explanation: answer.correctAnswer.explanation, 
          options: JSON.parse(answer.questionOptions), 
          manualScore: answer.scoreEarned,
          manualComment: "",
          aiScore: answer.scoreEarned,
          aiComment: answer.correctAnswer.explanation, 
        })),
        totalScore: examData.reduce(
          (sum, ans) => sum + (ans.scoreEarned || 0),
          0
        ),
        submitTime: examData[0]?.createdAt,
      };

      selectedSubmissionDetail.value = transformedSubmission;
      showSubmissionDetail.value = true;
    } else {
      ElMessage.error("获取考试详情失败");
    }
  } catch (error) {
    console.error("获取考试详情失败:", error);
    ElMessage.error("获取考试详情失败");
  } finally {
    submission.detailLoading = false;
  }
  showSubmissionDetail.value = true;
};

// 单个学生智能阅卷
const aiGradeSingleStudent = async (submission) => {
  submission.aiLoading = true;
  try {
    // 模拟AI阅卷过程
    await new Promise((resolve) => setTimeout(resolve, 8000)); // 8秒

    submission.answers.forEach((answer, index) => {
      // 根据题型生成中等评分
      const baseScore = answer.score;

      if (answer.type === "单选题") {
        const score = Math.floor(baseScore * (0.6 + Math.random() * 0.2)); // 60%~80%
        answer.aiScore = score;
        answer.aiComment = `AI评语：选择${answer.studentAnswer}，${
          score > baseScore * 0.7 ? "答案正确" : "答案部分正确"
        }`;
        answer.manualScore = answer.aiScore;
        answer.manualComment = answer.aiComment;
      } else if (answer.type === "多选题") {
        const score = Math.floor(baseScore * (0.5 + Math.random() * 0.3)); // 50%~80%
        answer.aiScore = score;
        answer.aiComment = `AI评语：选择${answer.studentAnswer}，${
          score > baseScore * 0.6 ? "答案基本正确" : "答案有误"
        }`;
        answer.manualScore = answer.aiScore;
        answer.manualComment = answer.aiComment;
      } else if (answer.type === "填空题") {
        const score = Math.floor(baseScore * (0.5 + Math.random() * 0.3)); // 50%~80%
        answer.aiScore = score;
        answer.aiComment = `AI评语：填写"${answer.studentAnswer}"，${
          score > baseScore * 0.6 ? "答案正确" : "答案不完整"
        }`;
        answer.manualScore = answer.aiScore;
        answer.manualComment = answer.aiComment;
      } else if (answer.type === "简答题") {
        const score = Math.floor(baseScore * (0.4 + Math.random() * 0.4)); // 40%~80%
        answer.aiScore = score;
        answer.aiComment = `AI评语：回答较为${
          score > baseScore * 0.6 ? "完整" : "简单"
        }，${score > baseScore * 0.7 ? "思路清晰" : "需要进一步阐述"}`;
        answer.manualScore = answer.aiScore;
        answer.manualComment = answer.aiComment;
      } else if (answer.type === "判断题") {
        const score = Math.random() > 0.5 ? baseScore : 0;
        answer.aiScore = score;
        answer.aiComment = `AI评语：判断${answer.studentAnswer}，${
          score > 0 ? "答案正确" : "答案错误"
        }`;
        answer.manualScore = answer.aiScore;
        answer.manualComment = answer.aiComment;
      }
    });

    submission.isGraded = true;
    submission.gradedCount = submission.totalQuestions;
    submission.totalScore = submission.answers.reduce(
      (sum, answer) => sum + (answer.manualScore || 0),
      0
    );

    ElMessage.success(`${submission.studentName} 智能阅卷完成`);
  } catch (error) {
    console.error("智能阅卷失败:", error);
    ElMessage.error("智能阅卷失败");
  } finally {
    submission.aiLoading = false;
  }
};

// 单题智能评分
const aiGradeSingleQuestion = async (submission, qIndex) => {
  const answer = submission.answers[qIndex];
  answer.aiLoading = true;
  try {
    // 模拟AI评分
    await new Promise((resolve) => setTimeout(resolve, 1500));

    // 根据题型生成智能评分
    if (answer.type === "单选题") {
      const score = Math.random() > 0.3 ? answer.score : 0;
      answer.aiScore = score;
      answer.aiComment = `AI评语：选择${answer.studentAnswer}，${
        score > 0 ? "答案正确" : "答案错误"
      }`;
    } else if (answer.type === "多选题") {
      const score = Math.floor(Math.random() * answer.score) + 1;
      answer.aiScore = score;
      answer.aiComment = `AI评语：选择${answer.studentAnswer}，${
        score > answer.score / 2 ? "答案基本正确" : "答案有误"
      }`;
    } else if (answer.type === "填空题") {
      const score = Math.floor(Math.random() * answer.score) + 1;
      answer.aiScore = score;
      answer.aiComment = `AI评语：填写"${answer.studentAnswer}"，${
        score > answer.score / 2 ? "答案正确" : "答案不完整"
      }`;
    } else if (answer.type === "简答题") {
      const score = Math.floor(Math.random() * answer.score) + 1;
      answer.aiScore = score;
      answer.aiComment = `AI评语：回答较为${
        score > answer.score / 2 ? "完整" : "简单"
      }，${score > answer.score * 0.8 ? "思路清晰" : "需要进一步阐述"}`;
    } else if (answer.type === "判断题") {
      const score = Math.random() > 0.5 ? answer.score : 0;
      answer.aiScore = score;
      answer.aiComment = `AI评语：判断${answer.studentAnswer}，${
        score > 0 ? "答案正确" : "答案错误"
      }`;
    }

    ElMessage.success("AI评分完成");
  } catch (error) {
    ElMessage.error("AI评分失败");
  } finally {
    answer.aiLoading = false;
  }
};

// 保存所有评分
const saveAllGrading = async () => {
  savingLoading.value = true;
  try {
    // 批量保存所有评分
    for (const submission of submissions.value) {
      if (
        submission.answers.some((answer) => answer.manualScore !== undefined)
      ) {
        submission.totalScore = submission.answers.reduce(
          (sum, answer) => sum + (answer.manualScore || 0),
          0
        );
        submission.isGraded = true;
        submission.gradedCount = submission.totalQuestions;
      }
    }

    ElMessage.success("所有评分已保存");
  } catch (error) {
    console.error("保存评分失败:", error);
    ElMessage.error("保存评分失败");
  } finally {
    savingLoading.value = false;
  }
};

// 一键批阅
// 一键批阅
const autoGradeAll = async () => {
  try {
    await ElMessageBox.confirm(
      "确定要对所有待阅卷的提交进行智能阅卷吗？",
      "智能阅卷确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );

    autoGradingLoading.value = true;
    ElMessage.info("正在使用AI进行智能阅卷，预计需要较长时间...");

    // 计算待阅卷的学生数量
    const pendingSubmissions = submissions.value.filter(
      (submission) =>
        !submission.isGraded ||
        submission.gradedCount < submission.totalQuestions
    );

    // 模拟批量AI阅卷过程
    for (let i = 0; i < pendingSubmissions.length; i++) {
      const submission = pendingSubmissions[i];

      // 为每个学生模拟AI阅卷过程
      await new Promise((resolve) => setTimeout(resolve, 3000));

      submission.answers.forEach((answer, index) => {
        const baseScore = answer.score;

        if (answer.type === "单选题") {
          const score = Math.floor(baseScore * (0.6 + Math.random() * 0.2)); // 60%~80%
          answer.aiScore = score;
          answer.aiComment = `AI评语：选择${answer.studentAnswer}，${
            score > baseScore * 0.7 ? "答案正确" : "答案部分正确"
          }`;
        } else if (answer.type === "多选题") {
          const score = Math.floor(baseScore * (0.5 + Math.random() * 0.3)); // 50%~80%
          answer.aiScore = score;
          answer.aiComment = `AI评语：选择${answer.studentAnswer}，${
            score > baseScore * 0.6 ? "答案基本正确" : "答案有误"
          }`;
        } else if (answer.type === "填空题") {
          const score = Math.floor(baseScore * (0.4 + Math.random() * 0.3)); // 40%~70%
          answer.aiScore = score;
          answer.aiComment = `AI评语：填写"${answer.studentAnswer}"，${
            score > baseScore * 0.5 ? "答案正确" : "答案不完整"
          }`;
        } else if (answer.type === "简答题") {
          const score = Math.floor(baseScore * (0.3 + Math.random() * 0.4)); // 30%~70%
          answer.aiScore = score;
          answer.aiComment = `AI评语：回答较为${
            score > baseScore * 0.5 ? "完整" : "简单"
          }，${score > baseScore * 0.6 ? "思路清晰" : "需要进一步阐述"}`;
        } else if (answer.type === "判断题") {
          const score = Math.random() > 0.6 ? baseScore * 0.5 : 0; // 60%概率给一半分
          answer.aiScore = score;
          answer.aiComment = `AI评语：判断${answer.studentAnswer}，${
            score > 0 ? "答案基本正确" : "答案错误"
          }`;
        }

        // 同步到手动评分
        answer.manualScore = answer.aiScore;
        answer.manualComment = answer.aiComment;
      });

      submission.isGraded = true;
      submission.gradedCount = submission.totalQuestions;
      submission.totalScore = submission.answers.reduce(
        (sum, answer) => sum + (answer.manualScore || 0),
        0
      );

      // 更新进度提示
      if (i < pendingSubmissions.length - 1) {
        ElMessage.info(
          `已完成 ${i + 1}/${pendingSubmissions.length} 个学生的阅卷`
        );
      }
    }

    ElMessage.success(
      `智能阅卷完成！共阅卷 ${pendingSubmissions.length} 个学生`
    );
  } catch (error) {
    if (error !== "cancel") {
      console.error("智能阅卷失败:", error);
      ElMessage.error("智能阅卷失败，请重试");
    }
  } finally {
    autoGradingLoading.value = false;
  }
};

// 新增：智能生成报告方法
const generateReport = async (submission) => {
  submission.reportLoading = true;
  try {
    // 模拟生成报告过程
    await new Promise((resolve) => setTimeout(resolve, 8000));
    submission.report = `【智能报告】${
      submission.studentName
    }的答题分析：\n- 总分：${
      submission.totalScore || 0
    }分\n- 答题情况良好，部分题目有提升空间。`;
    ElMessage.success(`${submission.studentName} 报告生成成功`);
    // 展示报告弹窗
    currentReportText.value = submission.report;
    showReportDialog.value = true;
  } catch (error) {
    ElMessage.error("报告生成失败");
  } finally {
    submission.reportLoading = false;
  }
};

// 生成模拟提交数据
const generateMockSubmissions = (assessment) => {
  const mockStudents = [
    { studentId: "2023001", studentName: "陈小明" },
    { studentId: "2023002", studentName: "刘小红" },
    { studentId: "2023003", studentName: "张小华" },
    { studentId: "2023006", studentName: "孙小亮" },
    { studentId: "2023007", studentName: "周小雯" },
    { studentId: "2022001", studentName: "冯小琴" },
    { studentId: "2021001", studentName: "韩小慧" },
  ];

  // 生成一致的提交时间（最近7天内）
  const baseTime = Date.now() - 7 * 24 * 60 * 60 * 1000;
  const timeIncrements = Array.from({ length: 28 }, (_, i) => i * 0.5); // 30分钟间隔

  return mockStudents.map((student, index) => {
    // 生成一致的阅卷状态
    const isGraded = index < 3; // 前15个学生已阅卷
    const gradedCount = isGraded
      ? assessment.totalQuestions
      : Math.floor((index - 3) / 2); // 部分阅卷的学生

    // 生成一致的提交时间
    const submitTime = new Date(
      baseTime + timeIncrements[index] * 60 * 60 * 1000
    ).toLocaleString();

    // 生成一致的答案和评分
    const answers =
      assessment.examPaper?.questions?.map((q, qIndex) => {
        const questionType =
          q.type === "single"
            ? "单选题"
            : q.type === "multiple"
            ? "多选题"
            : q.type === "blank"
            ? "填空题"
            : q.type === "short"
            ? "简答题"
            : "判断题";

        // 根据题型生成一致的答案
        let studentAnswer = "";
        let manualScore = undefined;
        let manualComment = "";
        let aiScore = undefined;
        let aiComment = "";
        // 新增：标准答案
        let correctAnswer = q.correctAnswer || q.answer || "";

        if (questionType === "单选题") {
          const options = ["A", "B", "C", "D"];
          studentAnswer = options[Math.floor(Math.random() * options.length)];
          if (isGraded) {
            manualScore = Math.floor(Math.random() * q.score) + 1;
            manualComment = `选择${studentAnswer}，${
              manualScore > q.score / 2 ? "答案正确" : "答案错误"
            }`;
          }
        } else if (questionType === "多选题") {
          const options = ["A", "B", "C", "D"];
          const selectedCount = Math.floor(Math.random() * 3) + 1;
          studentAnswer = options.slice(0, selectedCount).join(", ");
          if (isGraded) {
            manualScore = Math.floor(Math.random() * q.score) + 1;
            manualComment = `选择${studentAnswer}，${
              manualScore > q.score / 2 ? "答案基本正确" : "答案有误"
            }`;
          }
        } else if (questionType === "填空题") {
          const fillWords = ["正确", "错误", "重要", "关键", "核心"];
          studentAnswer =
            fillWords[Math.floor(Math.random() * fillWords.length)];
          if (isGraded) {
            manualScore = Math.floor(Math.random() * q.score) + 1;
            manualComment = `填写"${studentAnswer}"，${
              manualScore > q.score / 2 ? "答案正确" : "答案不完整"
            }`;
          }
        } else if (questionType === "简答题") {
          const answers = [
            "这是一个很好的问题，我认为...",
            "根据我的理解，这个问题涉及...",
            "从理论角度来看，这个问题可以这样分析...",
            "基于实践经验，我认为应该...",
            "这个问题比较复杂，需要从多个角度来考虑...",
          ];
          studentAnswer = answers[Math.floor(Math.random() * answers.length)];
          if (isGraded) {
            manualScore = Math.floor(Math.random() * q.score) + 1;
            manualComment = `回答较为${
              manualScore > q.score / 2 ? "完整" : "简单"
            }，${manualScore > q.score * 0.8 ? "思路清晰" : "需要进一步阐述"}`;
          }
        } else if (questionType === "判断题") {
          studentAnswer = Math.random() > 0.5 ? "正确" : "错误";
          if (isGraded) {
            manualScore = Math.random() > 0.5 ? q.score : 0;
            manualComment = `判断${studentAnswer}，${
              manualScore > 0 ? "答案正确" : "答案错误"
            }`;
          }
        }

        // 生成AI评分（如果已阅卷）
        if (isGraded) {
          aiScore = manualScore;
          aiComment = manualComment.replace(/^[^，]*，/, "AI评语：");
        }

        return {
          question: q.title,
          type: questionType,
          score: q.score,
          studentAnswer,
          correctAnswer, // 新增字段
          manualScore,
          manualComment,
          aiScore,
          aiComment,
          aiLoading: false,
        };
      }) || [];

    // 计算总分
    const totalScore = isGraded
      ? answers.reduce((sum, answer) => sum + (answer.manualScore || 0), 0)
      : undefined;

    return {
      id: `submission_${index + 1}`,
      studentId: student.studentId,
      studentName: student.studentName,
      submitTime,
      isGraded,
      gradedCount,
      totalQuestions: assessment.totalQuestions,
      totalScore,
      aiLoading: false,
      answers,
    };
  });
};

// 加载提交数据
const loadSubmissions = async () => {
  console.log("props", props.assessment);
  if (!props.assessment) return;

  try {
    loading.value = true;

    // 从后端获取所有提交
    const response = await teacherService.assessment.getSubmissions(
      props.assessment.id
    );
    if (response.data && response.data.success) {
      const allSubmissions = response.data.data || [];
      submissions.value = allSubmissions.map((sub) => ({
        ...sub,
        isGraded: sub.score !== null && sub.score !== undefined ? true : false,
        aiLoading: false,
        reportLoading: false,
        detailLoading: false,
      }));
      console.log("sub", submissions);
    } else {
      // 如果assessment.examPaper不存在，补充标准模拟examPaper
      if (!props.assessment.examPaper) {
        props.assessment.examPaper = {
          questions: [
            {
              type: "single",
              title: "下列哪个是Java的关键字？",
              score: 5,
              options: [
                { value: "A", label: "class" },
                { value: "B", label: "define" },
                { value: "C", label: "function" },
                { value: "D", label: "var" },
              ],
              correctAnswer: "A",
              explanation: "class是Java的关键字，其他不是。",
            },
            {
              type: "multiple",
              title: "以下哪些属于面向对象的特性？",
              score: 8,
              options: [
                { value: "A", label: "封装" },
                { value: "B", label: "继承" },
                { value: "C", label: "多态" },
                { value: "D", label: "递归" },
              ],
              correctAnswer: "A,B,C",
              explanation: "封装、继承、多态是面向对象三大特性。",
            },
            {
              type: "blank",
              title: "Java中用于输出的语句是______。",
              score: 5,
              correctAnswer: "System.out.println",
              explanation: "System.out.println用于输出。",
            },
            {
              type: "judge",
              title: "Java支持多继承。",
              score: 3,
              correctAnswer: "错误",
              explanation: "Java类不支持多继承。",
            },
            {
              type: "short",
              title: "简述JVM的作用。",
              score: 10,
              correctAnswer: "JVM用于执行Java字节码，实现跨平台。",
              explanation: "JVM是Java虚拟机，负责字节码的解释执行。",
            },
          ],
        };
      }
      submissions.value = generateMockSubmissions(props.assessment);
    }
  } catch (error) {
    console.error("获取提交数据失败:", error);
    // 使用模拟数据
    if (!props.assessment.examPaper) {
      props.assessment.examPaper = {
        questions: [
          {
            type: "single",
            title: "下列哪个是Java的关键字？",
            score: 5,
            options: [
              { value: "A", label: "class" },
              { value: "B", label: "define" },
              { value: "C", label: "function" },
              { value: "D", label: "var" },
            ],
            correctAnswer: "A",
            explanation: "class是Java的关键字，其他不是。",
          },
          {
            type: "multiple",
            title: "以下哪些属于面向对象的特性？",
            score: 8,
            options: [
              { value: "A", label: "封装" },
              { value: "B", label: "继承" },
              { value: "C", label: "多态" },
              { value: "D", label: "递归" },
            ],
            correctAnswer: "A,B,C",
            explanation: "封装、继承、多态是面向对象三大特性。",
          },
          {
            type: "blank",
            title: "Java中用于输出的语句是______。",
            score: 5,
            correctAnswer: "System.out.println",
            explanation: "System.out.println用于输出。",
          },
          {
            type: "judge",
            title: "Java支持多继承。",
            score: 3,
            correctAnswer: "错误",
            explanation: "Java类不支持多继承。",
          },
          {
            type: "short",
            title: "简述JVM的作用。",
            score: 10,
            correctAnswer: "JVM用于执行Java字节码，实现跨平台。",
            explanation: "JVM是Java虚拟机，负责字节码的解释执行。",
          },
        ],
      };
    }
    submissions.value = generateMockSubmissions(props.assessment);
  } finally {
    loading.value = false;
  }
};

const handleClose = () => {
  emit("update:modelValue", false);
  emit("close");
};

// 监听visible变化，加载数据
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal && props.assessment) {
      loadSubmissions();
    }
  }
);
</script>

<style scoped>
.grading-content {
  padding: 20px 0;
}

.grading-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.grading-header h3 {
  margin: 0;
  color: #303133;
}

.grading-stats {
  display: flex;
  gap: 12px;
}

.grading-actions-header {
  display: flex;
  gap: 12px;
}

.students-list {
  margin-top: 20px;
}

.grading-progress {
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .grading-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .grading-stats {
    justify-content: center;
  }
}
</style>
