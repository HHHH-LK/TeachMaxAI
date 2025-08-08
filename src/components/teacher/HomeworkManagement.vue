<template>
  <div class="homework-management-container">
    <!-- 顶部操作栏 -->
    <div class="top-actions">
      <h2>课堂作业管理</h2>
      <el-button
          type="primary"
          @click="showCreateDialog = true"
          class="create-btn"
      >
        <el-icon><Plus /></el-icon>
        智能创建作业
      </el-button>
    </div>

    <!-- 作业状态筛选 -->
    <div class="filter-section">
      <el-radio-group v-model="currentStatus" @change="filterHomework">
        <el-radio-button label="all">全部作业</el-radio-button>
        <!-- <el-radio-button label="published">已发布</el-radio-button>
        <el-radio-button label="completed">已完成</el-radio-button> -->
      </el-radio-group>
    </div>

    <!-- 作业列表 -->
    <div class="homework-list">
      <div
          v-for="homework in filteredHomework"
          :key="homework.id"
          class="homework-card"
      >
        <div class="homework-header">
          <div class="homework-title">
            <h3>{{ homework.title }}</h3>
            <el-tag :type="getStatusType(homework.status)" size="small">
              {{ getStatusText(homework.status) }}
            </el-tag>
          </div>
          <div class="homework-actions">
            <el-button size="small" @click="viewHomework(homework)"
            >查看</el-button
            >
            <el-button
                size="small"
                type="info"
                @click="viewSubmissions(homework)"
            >提交情况</el-button
            >
          </div>
        </div>

        <div class="homework-content">
          <!-- <p class="homework-description">{{ homework.description }}</p> -->
          <div class="homework-meta">
            <!-- <span><el-icon><Calendar /></el-icon> 截止时间: {{ formatDate(homework.deadline) }}</span> -->
            <span
            ><el-icon><User /></el-icon> 已提交:
              {{ homework.submittedCount }}/{{ homework.totalStudents }}</span
            >
            <span
            ><el-icon><Clock /></el-icon> 创建时间:
              {{ formatDate(homework.createdAt) }}</span
            >
          </div>
        </div>
      </div>
    </div>

    <!-- 创建作业对话框 -->
    <el-dialog
        v-model="showCreateDialog"
        title="创建并发布新作业"
        width="50%"
        :before-close="handleClose"
    >
      <el-form
          :model="newHomework"
          :rules="rules"
          ref="homeworkForm"
          label-width="100px"
      >
        <el-form-item label="作业标题" prop="title">
          <el-input
              v-model="newHomework.title"
              placeholder="请输入作业标题"
          ></el-input>
        </el-form-item>
        <el-form-item label="作业描述" prop="description">
          <el-input
              type="textarea"
              v-model="newHomework.description"
              :rows="3"
              placeholder="请输入作业描述"
          ></el-input>
        </el-form-item>
        <!-- <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="newHomework.deadline" type="datetime" placeholder="选择截止时间"></el-date-picker>
        </el-form-item> -->
        <!-- <el-form-item label="满分" prop="maxScore">
          <el-input-number v-model="newHomework.maxScore" :min="1" :max="100"></el-input-number>
        </el-form-item> -->
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="createHomework"
          >创建并发布</el-button
          >
        </span>
      </template>
    </el-dialog>

    <!-- 作业详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="作业详情" width="60%">
      <div v-if="selectedHomework" class="homework-detail">
        <h3>{{ selectedHomework.title }}</h3>
        <p class="description">{{ selectedHomework.description }}</p>
        <div class="meta-info">
          <!-- <p><strong>截止时间：</strong>{{ formatDate(selectedHomework.deadline) }}</p> -->
          <p><strong>满分：</strong>{{ selectedHomework.maxScore }}分</p>
          <p>
            <strong>状态：</strong>{{ getStatusText(selectedHomework.status) }}
          </p>
          <p>
            <strong>提交情况：</strong>{{ selectedHomework.submittedCount }}/{{
              selectedHomework.totalStudents
            }}
          </p>
        </div>
      </div>
    </el-dialog>

    <!-- 提交情况对话框 -->
    <el-dialog v-model="showSubmissionsDialog" title="学生提交情况" width="80%">
      <div class="submissions-header">
        <div class="submissions-info">
          <span>作业：{{ selectedHomework?.title }}</span>
          <span>已提交：{{ submittedCount }}/{{ totalStudents }}</span>
        </div>
        <div class="submissions-actions">
          <el-button
              type="primary"
              @click="startBatchReview"
              :loading="batchReviewLoading"
          >
            <el-icon><MagicStick /></el-icon>
            批量智能评阅
          </el-button>
        </div>
      </div>

      <div class="submissions-list">
        <el-table
            :data="submissions"
            style="width: 100%"
            v-loading="tableLoading"
        >
          <el-table-column
              prop="studentName"
              label="学生姓名"
              width="120"
          ></el-table-column>
          <el-table-column
              prop="studentId"
              label="学号"
              width="120"
          ></el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="180">
            <template #default="scope">
              {{
                scope.row.submitTime
                    ? formatDate(scope.row.submitTime)
                    : "未提交"
              }}
            </template>
          </el-table-column>
          <!-- <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'submitted' ? 'success' : 'warning'">
                {{ scope.row.status === 'submitted' ? '已提交' : '未提交' }}
              </el-tag>
            </template>
          </el-table-column> -->
          <el-table-column prop="reviewStatus" label="评阅状态" width="120">
            <template #default="scope">
              <el-tag
                  :type="getReviewStatusType(scope.row.reviewStatus)"
                  size="small"
              >
                {{ getReviewStatusText(scope.row.reviewStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button
                  size="small"
                  type="primary"
                  @click="viewSubmission(scope.row)"
                  :disabled="scope.row.status !== 'submitted'"
              >
                查看
              </el-button>
              <el-button
                  size="small"
                  type="success"
                  @click="startAIGrading(scope.row)"
                  :disabled="
                  scope.row.status !== 'submitted' ||
                  scope.row.reviewStatus === 'reviewed'
                "
                  :loading="scope.row.aiGradingLoading"
              >
                智能评阅
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 作业详情查看对话框 -->
    <el-dialog
        v-model="showSubmissionDetailDialog"
        title="作业详情"
        width="80%"
    >
      <div v-if="selectedSubmission" class="submission-detail">
        <div class="student-info">
          <h4>
            {{ selectedSubmission.studentName }} ({{
              selectedSubmission.studentId
            }})
          </h4>
          <p>提交时间：{{ formatDate(selectedSubmission.submitTime) }}</p>
        </div>

        <div class="submission-content">
          <h5>作业内容：</h5>
          <div class="content-area">
            <ExamPaper
                :examContent="
                selectedSubmission.examContent ||
                selectedSubmission.content ||
                ''
              "
                :readonly="true"
                :userAnswers="selectedSubmission.studentAnswers || []"
            />
          </div>
        </div>

        <div class="ai-review-section" v-if="selectedSubmission.aiReview">
          <h5>AI评阅结果：</h5>
          <div class="ai-review-content">
            <div class="score-section">
              <span class="score-label">建议分数：</span>
              <span class="score-value">{{
                  selectedSubmission.aiReview.suggestedScore
                }}</span>
            </div>
            <div class="feedback-section">
              <span class="feedback-label">评阅意见：</span>
              <p class="feedback-content">
                {{ selectedSubmission.aiReview.feedback }}
              </p>
            </div>
            <div class="criteria-section">
              <span class="criteria-label">评分标准：</span>
              <ul class="criteria-list">
                <li
                    v-for="(criterion, index) in selectedSubmission.aiReview
                    .criteria"
                    :key="index"
                >
                  {{ criterion.name }}: {{ criterion.score }}分
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div class="manual-review-section">
          <h5>手动评阅：</h5>
          <el-form :model="manualReview" label-width="100px">
            <el-form-item label="最终分数">
              <el-input-number
                  v-model="manualReview.finalScore"
                  :min="0"
                  :max="selectedHomework?.maxScore || 100"
                  :precision="1"
              ></el-input-number>
            </el-form-item>
            <el-form-item label="评阅意见">
              <el-input
                  type="textarea"
                  v-model="manualReview.comments"
                  :rows="3"
                  placeholder="请输入评阅意见"
              ></el-input>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showSubmissionDetailDialog = false"
          >取消</el-button
          >
          <el-button type="primary" @click="saveManualReview"
          >保存评阅</el-button
          >
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Calendar,
  User,
  Clock,
  MagicStick,
} from "@element-plus/icons-vue";
import ExamPaper from "@/components/ExamPaper.vue";
import {teacherService} from "@/services/api.js";

const res = teacherService.getHomework('1','3')
console.log("获取作业数据成功:", res)

// 响应式数据
const showCreateDialog = ref(false);
const showDetailDialog = ref(false);
const showSubmissionsDialog = ref(false);
const showSubmissionDetailDialog = ref(false);
const currentStatus = ref("all");
const selectedHomework = ref(null);
const submissions = ref([]);
const selectedSubmission = ref(null);
const tableLoading = ref(false);
const batchReviewLoading = ref(false);

// 手动评阅数据
const manualReview = ref({
  finalScore: 0,
  comments: "",
});

// 新作业表单数据
const newHomework = ref({
  title: "",
  description: "",
  deadline: "",
  maxScore: 100,
});

// 表单验证规则
const rules = {
  title: [{ required: true, message: "请输入作业标题", trigger: "blur" }],
  description: [{ required: true, message: "请输入作业描述", trigger: "blur" }],
  deadline: [{ required: true, message: "请选择截止时间", trigger: "change" }],
  maxScore: [{ required: true, message: "请输入满分", trigger: "blur" }],
};

// 模拟作业数据
const homeworkList = ref([
  {
    id: 1,
    title: "第一次作业",
    // description: '完成线性表、栈、队列的基本操作实现',
    // deadline: new Date('2024-01-15 23:59:59'),
    maxScore: 100,
    status: "published",
    createdAt: new Date("2024-01-01 10:00:00"),
    submittedCount: 5,
    totalStudents: 7,
  },
  {
    id: 2,
    title: "第二次作业",
    // description: '设计并实现排序算法',
    // deadline: new Date('2024-01-20 23:59:59'),
    maxScore: 80,
    status: "published",
    createdAt: new Date("2024-01-05 14:30:00"),
    submittedCount: 4,
    totalStudents: 7,
  },
  {
    id: 3,
    title: "第三次作业",
    // description: '实现二叉树的基本操作',
    // deadline: new Date('2024-01-10 23:59:59'),
    maxScore: 90,
    status: "published",
    createdAt: new Date("2023-12-28 09:15:00"),
    submittedCount: 0,
    totalStudents: 7,
  },
]);

// 计算属性
const filteredHomework = computed(() => {
  if (currentStatus.value === "all") {
    return homeworkList.value;
  }
  return homeworkList.value.filter((hw) => hw.status === currentStatus.value);
});

// 方法
const getStatusType = (status) => {
  const types = {
    published: "success",
    completed: "primary",
  };
  return types[status] || "info";
};

const getStatusText = (status) => {
  const texts = {
    published: "已发布",
    completed: "已完成",
  };
  return texts[status] || "未知";
};

const formatDate = (date) => {
  if (!date) return "";
  return new Date(date).toLocaleString("zh-CN");
};

const filterHomework = () => {
  // 筛选逻辑已在计算属性中处理
};

const createHomework = () => {
  const homework = {
    id: homeworkList.value.length + 1,
    ...newHomework.value,
    status: "published",
    createdAt: new Date(),
    submittedCount: 0,
    totalStudents: 32,
  };
  homeworkList.value.unshift(homework);
  showCreateDialog.value = false;
  ElMessage.success("作业创建并发布成功！");
  // 重置表单
  newHomework.value = {
    title: "",
    description: "",
    deadline: "",
    maxScore: 100,
  };
};

const viewHomework = (homework) => {
  selectedHomework.value = homework;
  showDetailDialog.value = true;
};

// 删除publishHomework方法，因为不再需要

// 计算属性
const submittedCount = computed(() => {
  return submissions.value.filter((s) => s.status === "submitted").length;
});

const totalStudents = computed(() => {
  return submissions.value.length;
});

const viewSubmissions = (homework) => {
  selectedHomework.value = homework;
  // 模拟学生提交数据
  submissions.value = [
    {
      studentName: "陈小明",
      studentId: "2023001",
      submitTime: new Date("2024-01-14 15:30:00"),
      status: "submitted",
      content:
          "我完成了线性表的基本操作，包括插入、删除、查找等功能。代码已经过测试，运行正常。",
      examContent: `1. [single-choice] ArrayList是线程安全的。\nA. 插入、删除、查找\nB. 排序、合并、分割\nC. 遍历、复制、清空\nD. 以上都是\nAnswer: A\nExplanation: 线性表的基本操作主要包括插入、删除、查找等操作。\n\n2. [fill-in-blank] 栈的特点是___。\nAnswer: 后进先出\nExplanation: 栈是一种后进先出(LIFO)的数据结构。\n\n3. [short-answer] 请简述线性表的顺序存储和链式存储的区别。\nAnswer: 顺序存储使用连续的内存空间，访问速度快但插入删除慢；链式存储使用指针连接，插入删除快但访问速度慢。\nExplanation: 两种存储方式各有优缺点，需要根据具体应用场景选择。`,
      studentAnswers: ["A", "后进先出", "顺序存储使用连续的内存空间，访问速度快但插入删除慢；链式存储使用指针连接，插入删除快但访问速度慢。"],
      score: undefined,
      reviewStatus: "pending",
      aiGradingLoading: false,
    },
    {
      studentName: "刘小红",
      studentId: "2023001",
      submitTime: new Date("2024-01-14 15:30:00"),
      status: "submitted",
      content:
          "我完成了线性表的基本操作，包括插入、删除、查找等功能。代码已经过测试，运行正常。",
      examContent: `1. [single-choice] ArrayList是线程安全的。
A. 插入、删除、查找
B. 排序、合并、分割
C. 遍历、复制、清空
D. 以上都是
Answer: A
Explanation: 线性表的基本操作主要包括插入、删除、查找等操作。

2. [fill-in-blank] 栈的特点是___。
Answer: 后进先出
Explanation: 栈是一种后进先出(LIFO)的数据结构。

3. [short-answer] 请简述线性表的顺序存储和链式存储的区别。
Answer: 顺序存储使用连续的内存空间，访问速度快但插入删除慢；链式存储使用指针连接，插入删除快但访问速度慢。
Explanation: 两种存储方式各有优缺点，需要根据具体应用场景选择。`,
      studentAnswers: ["B", "后进先出", "顺序存储使用连续的内存空间，访问速度快但插入删除慢；链式存储使用指针连接，插入删除快但访问速度慢。"],
      score: undefined,
      reviewStatus: "pending",
      aiGradingLoading: false,
    },
    {
      studentName: "张小华",
      studentId: "2023002",
      submitTime: new Date("2024-01-14 16:45:00"),
      status: "submitted",
      content: "实现了栈和队列的基本操作，代码结构清晰，注释详细。",
      examContent: `1. [single-choice] 队列的特点是___？
A. 先进先出
B. 后进先出
C. 随机访问
D. 双向访问
Answer: A
Explanation: 队列是一种先进先出(FIFO)的数据结构。

2. [multiple-choice] 以下哪些是栈的基本操作？
A. push
B. pop
C. peek
D. enqueue
Answer: A,B,C
Explanation: push(入栈)、pop(出栈)、peek(查看栈顶)是栈的基本操作，enqueue是队列的操作。

3. [short-answer] 请说明栈和队列在计算机科学中的应用场景。
Answer: 栈用于函数调用、表达式求值、括号匹配等；队列用于任务调度、缓冲区管理、广度优先搜索等。
Explanation: 栈和队列在算法和系统设计中都有广泛应用。`,
      studentAnswers: ["A", "A,B,D", "栈用于函数调用、任务调度、缓冲区管理、广度优先搜索等。"],
      score: 85,
      reviewStatus: "reviewed",
      aiGradingLoading: false,
    },

    {
      studentName: "周小雯",
      studentId: "2023004",
      submitTime: new Date("2024-01-15 10:20:00"),
      status: "submitted",
      content: "完成了所有要求的功能，代码质量较高，算法实现正确。",
      examContent: `1. [true-false] 线性表可以是空表。
Answer: true
Explanation: 线性表可以为空，即不包含任何元素。

2. [fill-in-blank] 在顺序表中，元素的存储位置是___的。
Answer: 连续
Explanation: 顺序表使用连续的内存空间存储元素。

3. [short-answer] 请分析顺序表和链表的优缺点。
Answer: 顺序表优点：随机访问快、存储密度高；缺点：插入删除慢、需要连续空间。链表优点：插入删除快、空间利用灵活；缺点：随机访问慢、需要额外空间存储指针。
Explanation: 两种实现方式各有特点，需要根据具体需求选择。`,
      studentAnswers: ["true", "连续", "顺序表优点：随机访问快、存储密度高；缺点：插入删除慢、需要连续空间。链表优点：插入删除快、空间利用灵活；缺点：随机访问慢、需要额外空间存储指针。"],
      score: undefined,
      reviewStatus: "pending",
      aiGradingLoading: false,
    },
    {
      studentName: "冯小琴",
      studentId: "2023004",
      submitTime: new Date("2024-01-15 10:20:00"),
      status: "submitted",
      content: "完成了所有要求的功能，代码质量较高，算法实现正确。",
      examContent: `1. [true-false] 线性表可以是空表。
Answer: true
Explanation: 线性表可以为空，即不包含任何元素。

2. [fill-in-blank] 在顺序表中，元素的存储位置是___的。
Answer: 连续
Explanation: 顺序表使用连续的内存空间存储元素。

3. [short-answer] 请分析顺序表和链表的优缺点。
Answer: 顺序表优点：随机访问快、存储密度高；缺点：插入删除慢、需要连续空间。链表优点：插入删除快、空间利用灵活；缺点：随机访问慢、需要额外空间存储指针。
Explanation: 两种实现方式各有特点，需要根据具体需求选择。`,
      studentAnswers: ["true", "连续", "顺序表优点：随机访问快、存储密度高；缺点：插入删除慢、需要连续空间。链表优点：插入删除快、空间利用灵活；缺点：随机访问慢、需要额外空间存储指针。"],
      score: undefined,
      reviewStatus: "pending",
      aiGradingLoading: false,
    },
    {
      studentName: "韩小慧",
      studentId: "2023003",
      submitTime: null,
      status: "not_submitted",
      content: "",
      examContent: "",
      score: undefined,
      reviewStatus: "not_submitted",
      aiGradingLoading: false,
    },
    {
      studentName: "孙小亮",
      studentId: "2023003",
      submitTime: null,
      status: "not_submitted",
      content: "",
      examContent: "",
      score: undefined,
      reviewStatus: "not_submitted",
      aiGradingLoading: false,
    },
  ];
  showSubmissionsDialog.value = true;
};

// 获取成绩样式类
const getScoreClass = (score) => {
  if (score >= 90) return "score-excellent";
  if (score >= 80) return "score-good";
  if (score >= 70) return "score-average";
  if (score >= 60) return "score-pass";
  return "score-fail";
};

// 获取评阅状态类型
const getReviewStatusType = (status) => {
  const types = {
    pending: "warning",
    reviewed: "success",
    not_submitted: "info",
  };
  return types[status] || "info";
};

// 获取评阅状态文本
const getReviewStatusText = (status) => {
  const texts = {
    pending: "待评阅",
    reviewed: "已评阅",
    not_submitted: "未提交",
  };
  return texts[status] || "未知";
};

// 查看作业详情
const viewSubmission = (submission) => {
  selectedSubmission.value = submission;
  manualReview.value = {
    finalScore: submission.score || 0,
    comments: submission.comments || "",
  };
  // 直接传递studentAnswers
  selectedSubmission.value.studentAnswers = submission.studentAnswers || [];
  showSubmissionDetailDialog.value = true;
};

// 开始AI智能评阅
const startAIGrading = async (submission) => {
  submission.aiGradingLoading = true;

  try {
    // 模拟AI评阅过程
    await new Promise((resolve) => setTimeout(resolve, 2000));

    // 模拟AI评阅结果
    const aiReview = {
      suggestedScore: Math.floor(Math.random() * 30) + 70, // 70-100分
      feedback: generateAIFeedback(submission.content),
      criteria: [
        { name: "代码质量", score: Math.floor(Math.random() * 20) + 15 },
        { name: "功能完整性", score: Math.floor(Math.random() * 20) + 15 },
        { name: "算法正确性", score: Math.floor(Math.random() * 20) + 15 },
        { name: "注释规范性", score: Math.floor(Math.random() * 10) + 5 },
      ],
    };

    submission.aiReview = aiReview;
    submission.reviewStatus = "ai_reviewed";

    ElMessage.success(`${submission.studentName} 的作业AI评阅完成！`);
  } catch (error) {
    ElMessage.error("AI评阅失败，请重试");
  } finally {
    submission.aiGradingLoading = false;
  }
};

// 生成AI评阅意见
const generateAIFeedback = (content) => {
  const feedbacks = [
    "作业完成度较高，代码结构清晰，逻辑正确。建议在注释方面可以更加详细。",
    "功能实现完整，算法思路正确。代码风格良好，但可以进一步优化性能。",
    "整体表现优秀，代码质量较高。建议在边界条件处理上更加严谨。",
    "作业质量良好，实现了基本功能。建议加强代码的可读性和维护性。",
    "代码实现正确，功能完整。建议在算法复杂度方面进行优化。",
  ];
  return feedbacks[Math.floor(Math.random() * feedbacks.length)];
};

// 批量智能评阅
const startBatchReview = async () => {
  const pendingSubmissions = submissions.value.filter(
      (s) => s.status === "submitted" && s.reviewStatus === "pending"
  );

  if (pendingSubmissions.length === 0) {
    ElMessage.warning("没有待评阅的作业");
    return;
  }

  batchReviewLoading.value = true;

  try {
    for (const submission of pendingSubmissions) {
      await startAIGrading(submission);
      await new Promise((resolve) => setTimeout(resolve, 500)); // 间隔500ms
    }
    ElMessage.success(
        `批量评阅完成，共评阅 ${pendingSubmissions.length} 份作业`
    );
  } catch (error) {
    ElMessage.error("批量评阅过程中出现错误");
  } finally {
    batchReviewLoading.value = false;
  }
};

// 保存手动评阅
const saveManualReview = () => {
  if (!selectedSubmission.value) return;

  selectedSubmission.value.score = manualReview.value.finalScore;
  selectedSubmission.value.comments = manualReview.value.comments;
  selectedSubmission.value.reviewStatus = "reviewed";

  ElMessage.success("评阅结果保存成功！");
  showSubmissionDetailDialog.value = false;
};

// 删除exportSubmissions方法，因为不再需要

const handleClose = () => {
  showCreateDialog.value = false;
};
</script>

<style scoped>
.homework-management-container {
  padding: 20px;
}

.top-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.top-actions h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-section {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.homework-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 24px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.homework-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.homework-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.homework-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.homework-title h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 18px;
  font-weight: 600;
}

.homework-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.homework-content {
  color: #666;
}

.homework-description {
  margin-bottom: 16px;
  line-height: 1.6;
}

.homework-meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  font-size: 14px;
  color: #888;
}

.homework-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.homework-detail {
  padding: 20px;
}

.homework-detail h3 {
  color: #2c3e50;
  margin-bottom: 16px;
}

.description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
}

.meta-info p {
  margin-bottom: 8px;
  color: #666;
}

.submissions-list {
  max-height: 400px;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 提交情况相关样式 */
.submissions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.submissions-info {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.submissions-actions {
  display: flex;
  gap: 12px;
}

.submissions-list {
  max-height: 500px;
  overflow-y: auto;
}

/* 成绩样式 */
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

.no-score {
  color: #9ca3af;
  font-style: italic;
}

/* 作业详情样式 */
.submission-detail {
  padding: 20px 0;
}

.student-info {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.student-info h4 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 18px;
}

.student-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.submission-content {
  margin-bottom: 24px;
}

.submission-content h5 {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 16px;
}

.content-area {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
  max-height: 60vh;
  overflow-y: auto;
}

.content-area p {
  margin: 0;
  line-height: 1.6;
  color: #374151;
}

/* AI评阅样式 */
.ai-review-section {
  margin-bottom: 24px;
  padding: 20px;
  background: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #bae6fd;
}

.ai-review-section h5 {
  margin: 0 0 16px 0;
  color: #0369a1;
  font-size: 16px;
}

.ai-review-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.score-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  font-weight: 600;
  color: #374151;
}

.score-value {
  font-size: 18px;
  font-weight: bold;
  color: #10b981;
}

.feedback-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-label {
  font-weight: 600;
  color: #374151;
}

.feedback-content {
  margin: 0;
  padding: 12px;
  background: white;
  border-radius: 6px;
  line-height: 1.6;
  color: #4b5563;
}

.criteria-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.criteria-label {
  font-weight: 600;
  color: #374151;
}

.criteria-list {
  margin: 0;
  padding-left: 20px;
  color: #4b5563;
}

.criteria-list li {
  margin-bottom: 4px;
}

/* 手动评阅样式 */
.manual-review-section {
  padding: 20px;
  background: #fef3c7;
  border-radius: 8px;
  border: 1px solid #fbbf24;
}

.manual-review-section h5 {
  margin: 0 0 16px 0;
  color: #92400e;
  font-size: 16px;
}
</style>
