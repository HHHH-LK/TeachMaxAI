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
          <div class="homework-description" v-if="homework.description">
            <p>{{ homework.description }}</p>
          </div>
          <div class="homework-meta">
            <span
              ><el-icon><User /></el-icon> 已提交:
              {{ homework.submittedCount }}/{{ homework.totalStudents }}</span
            >
            <span v-if="homework.questions && homework.questions.length > 0"
              ><el-icon><QuestionFilled /></el-icon> 题目数量:
              {{ homework.questions.length }}</span
            >
            <span v-if="homework.difficultyLevel"
              ><el-icon><Star /></el-icon> 难度:
              <span
                :class="`difficulty-tag difficulty-${homework.difficultyLevel}`"
              >
                {{ getDifficultyText(homework.difficultyLevel) }}
              </span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建作业对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="智能创建并发布新作业"
      width="60%"
      :before-close="handleClose"
    >
      <!-- 提示信息 -->
      <div class="create-tips">
        <el-alert
          title="智能创建作业提示"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>1. 选择章节：选择要创建作业的课程章节</p>
            <p>2. 作业描述：详细描述作业要求，AI将根据描述智能生成相应题目</p>
            <p>3. 题目数量：设置要生成的题目数量（1-20题）</p>
            <p>4. 难度等级：选择题目难度，AI将根据难度调整题目复杂度</p>
          </template>
        </el-alert>
      </div>
      <el-form
        :model="newHomework"
        :rules="rules"
        ref="homeworkForm"
        label-width="100px"
      >
        <el-form-item label="选择章节" prop="chapterId">
          <el-select
            v-model="newHomework.chapterId"
            placeholder="请选择章节"
            @change="onChapterChange"
            style="width: 100%"
          >
            <el-option
              v-for="chapter in chapters"
              :key="chapter.chapterId"
              :label="chapter.chapterName"
              :value="chapter.chapterId"
            ></el-option>
          </el-select>
        </el-form-item>
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
            placeholder="请输入作业描述，AI将根据描述智能生成题目"
          ></el-input>
        </el-form-item>
        <el-form-item label="题目数量" prop="questionCount">
          <el-input-number
            v-model="newHomework.questionCount"
            :min="1"
            :max="20"
            placeholder="请输入题目数量"
          ></el-input-number>
        </el-form-item>
        <el-form-item label="难度等级" prop="difficultyLevel">
          <el-select
            v-model="newHomework.difficultyLevel"
            placeholder="请选择难度等级"
            style="width: 100%"
          >
            <el-option label="简单" value="easy"></el-option>
            <el-option label="中等" value="medium"></el-option>
            <el-option label="困难" value="hard"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button
            type="primary"
            @click="createHomework"
            :loading="createLoading"
          >
            <el-icon><MagicStick /></el-icon>
            智能创建并发布
          </el-button>
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

      <div class="questions-section">
        <h4>题目列表：</h4>
        <ExamPaperHome
          :examContent="formatHomeworkQuestions(selectedHomework)"
          :readonly="true"
          :chapterId="String(selectedHomework?.chapterId || '')"
          :courseId="props.courseId"
          :questionIds="getQuestionIds(selectedHomework)"
        ></ExamPaperHome>
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
          <el-table-column
            label="班级信息"
            width="150"
            v-if="submissions.some((s) => s.studentDetail?.className)"
          >
            <template #default="scope">
              <span v-if="scope.row.studentDetail?.className">
                {{ scope.row.studentDetail.className }}
              </span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="提交时间" width="180">
            <template #default="scope">
              {{
                scope.row.submitTime
                  ? formatDate(scope.row.submitTime)
                  : "未提交"
              }}
            </template>
          </el-table-column>

          <el-table-column prop="reviewStatus" label="评阅状态" width="120">
            <template #default="scope">
              <!-- 如果状态是ai_reviewed，只显示一个"AI已评阅"标签 -->
              <el-tag
                v-if="scope.row.reviewStatus === 'ai_reviewed'"
                :type="getReviewStatusType(scope.row.reviewStatus)"
                size="small"
              >
                AI已评阅
              </el-tag>
              <!-- 其他状态正常显示 -->
              <el-tag
                v-else
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
              :chapterId="selectedHomework?.chapterId || ''"
              :courseId="props.courseId"
            />
          </div>
        </div>

        <div class="ai-review-section" v-if="selectedSubmission.aiReview">
          <h5>AI评阅结果：</h5>
          <div class="ai-review-content">
            <!-- 总体评阅信息 -->
            <div class="overall-review-section">
              <div class="review-summary">
                <div class="summary-item">
                  <span class="summary-label">总体评价：</span>
                  <el-tag
                    :type="
                      getTestLevelType(selectedSubmission.aiReview.testLevel)
                    "
                    size="small"
                  >
                    {{ selectedSubmission.aiReview.testLevel }}
                  </el-tag>
                </div>

                <div class="summary-item">
                  <span class="summary-label">正确率：</span>
                  <span class="summary-value"
                    >{{
                      (selectedSubmission.aiReview.accuracy * 100).toFixed(1)
                    }}%</span
                  >
                </div>
              </div>

              <div class="question-count-section">
                <div class="count-item">
                  <span class="count-label">题目总数：</span>
                  <span class="count-value">{{
                    selectedSubmission.aiReview.totalQuestions
                  }}</span>
                </div>
              </div>
            </div>

            <!-- 答题统计 -->
            <div class="answer-stats-section">
              <h6>答题统计</h6>
              <div class="stats-grid">
                <div class="stat-item">
                  <span class="stat-label">已答题：</span>
                  <span class="stat-value">{{
                    selectedSubmission.aiReview.answeredQuestions
                  }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">未答题：</span>
                  <span class="stat-value">{{
                    selectedSubmission.aiReview.unansweredQuestions
                  }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">正确题：</span>
                  <span class="stat-value correct">{{
                    selectedSubmission.aiReview.correctQuestions
                  }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">错误题：</span>
                  <span class="stat-value wrong">{{
                    selectedSubmission.aiReview.wrongQuestions
                  }}</span>
                </div>
              </div>
            </div>

            <!-- 题目类型统计 -->
            <div
              class="question-type-stats"
              v-if="selectedSubmission.aiReview.questionTypeStats"
            >
              <h6>题目类型统计</h6>
              <div class="type-stats-grid">
                <div
                  class="type-stat-item"
                  v-if="
                    selectedSubmission.aiReview.questionTypeStats.singleChoice
                      .totalCount > 0
                  "
                >
                  <span class="type-label">单选题：</span>
                  <span class="type-value"
                    >{{
                      selectedSubmission.aiReview.questionTypeStats.singleChoice
                        .correctCount
                    }}/{{
                      selectedSubmission.aiReview.questionTypeStats.singleChoice
                        .totalCount
                    }}</span
                  >
                </div>
                <div
                  class="type-stat-item"
                  v-if="
                    selectedSubmission.aiReview.questionTypeStats.multipleChoice
                      .totalCount > 0
                  "
                >
                  <span class="type-label">多选题：</span>
                  <span class="type-value"
                    >{{
                      selectedSubmission.aiReview.questionTypeStats
                        .multipleChoice.correctCount
                    }}/{{
                      selectedSubmission.aiReview.questionTypeStats
                        .multipleChoice.totalCount
                    }}</span
                  >
                </div>
                <div
                  class="type-stat-item"
                  v-if="
                    selectedSubmission.aiReview.questionTypeStats.trueFalse
                      .totalCount > 0
                  "
                >
                  <span class="type-label">判断题：</span>
                  <span class="type-value"
                    >{{
                      selectedSubmission.aiReview.questionTypeStats.trueFalse
                        .correctCount
                    }}/{{
                      selectedSubmission.aiReview.questionTypeStats.trueFalse
                        .totalCount
                    }}</span
                  >
                </div>
                <div
                  class="type-stat-item"
                  v-if="
                    selectedSubmission.aiReview.questionTypeStats.fillBlank
                      .totalCount > 0
                  "
                >
                  <span class="type-label">填空题：</span>
                  <span class="type-value"
                    >{{
                      selectedSubmission.aiReview.questionTypeStats.fillBlank
                        .correctCount
                    }}/{{
                      selectedSubmission.aiReview.questionTypeStats.fillBlank
                        .totalCount
                    }}</span
                  >
                </div>
                <div
                  class="type-stat-item"
                  v-if="
                    selectedSubmission.aiReview.questionTypeStats.shortAnswer
                      .totalCount > 0
                  "
                >
                  <span class="type-label">简答题：</span>
                  <span class="type-value"
                    >{{
                      selectedSubmission.aiReview.questionTypeStats.shortAnswer
                        .correctCount
                    }}/{{
                      selectedSubmission.aiReview.questionTypeStats.shortAnswer
                        .totalCount
                    }}</span
                  >
                </div>
              </div>
            </div>

            <!-- 评阅意见 -->
            <div class="feedback-section">
              <span class="feedback-label">评阅意见：</span>
              <p class="feedback-content">
                {{ selectedSubmission.aiReview.feedback }}
              </p>
            </div>

            <!-- 学习建议 -->
            <div
              class="suggestion-section"
              v-if="selectedSubmission.aiReview.suggestion"
            >
              <span class="suggestion-label">学习建议：</span>
              <p class="suggestion-content">
                {{ selectedSubmission.aiReview.suggestion }}
              </p>
            </div>
          </div>
        </div>

        <div class="manual-review-section">
          <h5>手动评阅：</h5>
          <div class="ai-review-reference" v-if="selectedSubmission.aiReview">
            <el-alert
              title="AI评阅参考"
              type="info"
              :closable="false"
              show-icon
            >
              <template #default>
                <div class="ai-reference-content">
                  <p v-if="selectedSubmission.aiReview.feedback">
                    <strong>AI评阅意见：</strong
                    >{{ selectedSubmission.aiReview.feedback }}
                  </p>
                  <p v-if="selectedSubmission.aiReview.suggestion">
                    <strong>AI学习建议：</strong
                    >{{ selectedSubmission.aiReview.suggestion }}
                  </p>
                  <p v-if="selectedSubmission.aiReview.testLevel">
                    <strong>AI评价等级：</strong>
                    <el-tag
                      :type="
                        getTestLevelType(selectedSubmission.aiReview.testLevel)
                      "
                      size="small"
                    >
                      {{ selectedSubmission.aiReview.testLevel }}
                    </el-tag>
                  </p>
                </div>
              </template>
            </el-alert>
          </div>
          <el-form :model="manualReview" label-width="100px">
            <el-form-item label="评阅意见">
              <el-input
                type="textarea"
                v-model="manualReview.comments"
                :rows="3"
                placeholder="请输入评阅意见，可参考AI评阅结果"
              ></el-input>
            </el-form-item>
            <el-form-item label="评分等级">
              <el-select
                v-model="manualReview.grade"
                placeholder="请选择评分等级"
                style="width: 100%"
              >
                <el-option label="优秀" value="优秀"></el-option>
                <el-option label="良好" value="良好"></el-option>
                <el-option label="中等" value="中等"></el-option>
                <el-option label="及格" value="及格"></el-option>
                <el-option label="不及格" value="不及格"></el-option>
              </el-select>
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
import { ref, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  Plus,
  User,
  MagicStick,
  QuestionFilled,
  Star,
} from "@element-plus/icons-vue";
import ExamPaper from "@/components/ExamPaper.vue";
import { teacherService, studentService } from "@/services/api.js";
import ExamPaperHome from "./ExamPaperHome.vue";

// 定义props接收课程ID
const props = defineProps({
  courseId: {
    type: [String, Number],
    required: true,
  },
});

// 作业列表数据
const homeworkList = ref([]);

// 响应式数据
const showCreateDialog = ref(false);
const showDetailDialog = ref(false);
const showSubmissionsDialog = ref(false);
const showSubmissionDetailDialog = ref(false);
const selectedHomework = ref(null);
const submissions = ref([]);
const selectedSubmission = ref(null);
const tableLoading = ref(false);
const batchReviewLoading = ref(false);
const createLoading = ref(false); // 新增：创建作业时的加载状态

// 课程和学生数据
const courseStudents = ref([]);
const chapters = ref([]); // 新增：章节列表

// 手动评阅数据
const manualReview = ref({
  comments: "",
  grade: "",
});

const getQuestionIds = (homework) => {
  if (!homework || !homework.questions) return [];
  return homework.questions.map(q => q.id);
};

// 将作业题目转换为ExamPaper需要的格式
const formatHomeworkQuestions = (homework) => {
  if (!homework || !homework.questions || homework.questions.length === 0) {
    return "该作业暂无题目";
  }
  
  return homework.questions.map((q, index) => {
    let questionText = `${index + 1}. [${q.type}] ${q.text || q.title || q.questionContent}`;
    
    // 添加选项（如果是选择题）
    if (q.options && q.options.length > 0) {
      questionText += "\n" + q.options.map((opt, optIndex) => {
        return `${String.fromCharCode(65 + optIndex)}. ${opt.content || opt.label || opt.text}`;
      }).join("\n");
    }
    
    // 添加正确答案
    if (q.answer || q.correctAnswer) {
      questionText += `\n答案: ${q.answer || q.correctAnswer}`;
    }
    
    // 添加解析
    if (q.explanation) {
      questionText += `\n解析: ${q.explanation}`;
    }
    
    return questionText;
  }).join("\n\n");
};

// 新作业表单数据
const newHomework = ref({
  chapterId: "", // 新增：章节ID
  title: "",
  description: "",
  questionCount: 10, // 新增：题目数量
  difficultyLevel: "medium", // 新增：难度等级
});

// 表单验证规则
const rules = {
  chapterId: [{ required: true, message: "请选择章节", trigger: "change" }],
  title: [{ required: true, message: "请输入作业标题", trigger: "blur" }],
  description: [{ required: true, message: "请输入作业描述", trigger: "blur" }],
  questionCount: [
    { required: true, message: "请输入题目数量", trigger: "blur" },
  ],
  difficultyLevel: [
    { required: true, message: "请选择难度等级", trigger: "change" },
  ],
};

// 获取课程学生列表
const fetchCourseStudents = async () => {
  try {
    const response = await teacherService.getClassStudents(props.courseId);
    console.log("获取课程学生列表:", response);
    if (response.data && response.data.success && response.data.data) {
      courseStudents.value = response.data.data;
      console.log("课程学生列表:", courseStudents.value);
    }
  } catch (error) {
    console.error("获取课程学生列表失败:", error);
    ElMessage.error("获取课程学生列表失败");
  }
};

// 获取学生详细信息
const getStudentDetail = async (studentNumber) => {
  try {
    const response = await studentService.findByStudentNumber(studentNumber);
    if (response.data && response.data.success && response.data.data) {
      return response.data.data;
    }
    return null;
  } catch (error) {
    console.error("获取学生详细信息失败:", error);
    return null;
  }
};

// 获取学生作业信息
const getStudentHomework = async (studentId, courseId, chapterId) => {
  try {
    const response = await teacherService.getHomeworkByStudent(
      studentId,
      courseId,
      chapterId
    );
    console.log("获取学生作业信息:", response);
    if (response.data && response.data.success && response.data.data) {
      return response.data.data;
    }
    return [];
  } catch (error) {
    console.error("获取学生作业信息失败:", error);
    return [];
  }
};

// 获取作业列表
const fetchAllWork = async () => {
  try {
    // 获取章节信息
    const responseChapter = await studentService.getChapterInfo(props.courseId);
    console.log("章节列表", responseChapter);

    if (responseChapter.data && responseChapter.data.data) {
      chapters.value = responseChapter.data.data;
      console.log("章节列表:", chapters.value);

      // 获取每个章节的作业信息
      for (const chapter of chapters.value) {
        try {
          const response = await teacherService.getHomework(
            props.courseId,
            chapter.chapterId
          );
          console.log(`章节 ${chapter.chapterName} 的作业:`, response);

          if (
            response.data &&
            response.data.data &&
            response.data.data.length > 0
          ) {
            // 检查是否已经存在该章节的作业
            const existingHomework = homeworkList.value.find(
              (h) => h.chapterId === chapter.chapterId
            );
            if (!existingHomework) {
              const homework = {
                id: `${props.courseId}-${chapter.chapterId}-${Date.now()}`,
                title: `${chapter.chapterName} - 课堂作业`,
                chapterId: chapter.chapterId,
                chapterName: chapter.chapterName,
                description: `基于${chapter.chapterName}的课堂练习`,
                questions: response.data.data.map((item) => {
                  let parsedOptions = [];
                  try {
                    parsedOptions = JSON.parse(item.questionOptions || "[]");
                  } catch (e) {
                    console.warn("选项解析失败:", item.questionOptions);
                    parsedOptions = [];
                  }

                  return {
                    id: item.questionId,
                    type: item.questionType,
                    text: item.questionContent,
                    options: parsedOptions,
                    answer: item.correctAnswer
                      ? item.correctAnswer.replace(/"/g, "")
                      : "",
                    explanation: item.explanation,
                    difficultyLevel: item.difficultyLevel,
                    pointName: item.pointName,
                  };
                }),
                status: "published",
                createdAt: new Date(),
                submittedCount: 0,
                totalStudents: courseStudents.value.length || 0,
                questionCount: response.data.data.length,
                difficultyLevel: "medium",
              };

              homeworkList.value.push(homework);
            }
          }
        } catch (error) {
          console.warn(`获取章节 ${chapter.chapterName} 作业失败:`, error);
        }
      }
    }
  } catch (error) {
    console.error("获取章节列表失败:", error);
    ElMessage.error("获取章节列表失败");
  }
};

// 获取学生提交情况
const fetchStudentSubmissions = async (homework) => {
  if (!homework || !homework.chapterId) return;

  tableLoading.value = true;
  try {
    const submissionsData = [];

    for (const student of courseStudents.value) {
      // 获取学生详细信息
      const studentDetail = await getStudentDetail(student.studentNumber);

      // 获取学生作业信息
      const homeworkData = await getStudentHomework(
        studentDetail?.studentId || student.studentNumber,
        props.courseId,
        homework.chapterId
      );

      const submission = {
        studentName: student.realName,
        studentId: student.studentNumber,
        studentDetail: studentDetail,
        submitTime: null,
        status: "not_submitted",
        content: "",
        examContent: "",
        studentAnswers: [],
        reviewStatus: "not_submitted",
        aiGradingLoading: false,
        homeworkData: homeworkData,
      };

      // 如果有作业数据，说明学生已提交
      if (homeworkData && homeworkData.length > 0) {
        submission.status = "submitted";
        submission.submitTime = new Date();
        submission.reviewStatus = "pending";

        // 处理作业内容 - 转换为ExamPaper期望的格式
        submission.examContent = homeworkData
          .map((item, index) => {
            // 转换题目类型为ExamPaper期望的格式
            let questionType = "question";
            if (item.questionType) {
              switch (item.questionType.toLowerCase()) {
                case "single":
                case "single_choice":
                case "single-choice":
                  questionType = "single-choice";
                  break;
                case "multiple":
                case "multiple_choice":
                case "multiple-choice":
                  questionType = "multiple-choice";
                  break;
                case "fill":
                case "fill_blank":
                case "fill-in-blank":
                  questionType = "fill-in-blank";
                  break;
                case "judge":
                case "true_false":
                case "true-false":
                  questionType = "true-false";
                  break;
                case "short":
                case "short_answer":
                case "short-answer":
                  questionType = "short-answer";
                  break;
                default:
                  questionType = "question";
              }
            }

            const questionText = `${index + 1}. [${questionType}] ${
              item.questionContent || ""
            }`;

            // 处理选项 - 修复[object Object]问题
            let optionsText = "";
            if (item.questionOptions) {
              try {
                const options = JSON.parse(item.questionOptions);

                if (Array.isArray(options) && options.length > 0) {
                  // 过滤掉无效的选项内容
                  const validOptions = options.filter((opt) => {
                    if (typeof opt === "string")
                      return opt && opt.trim() !== "";
                    if (typeof opt === "object" && opt !== null) {
                      // 如果是对象，尝试提取有用的字段
                      return opt.content || opt.text || opt.label || opt.value;
                    }
                    return false;
                  });

                  if (validOptions.length > 0) {
                    optionsText = validOptions
                      .map((opt, i) => {
                        let optionText = "";
                        if (typeof opt === "string") {
                          optionText = opt;
                        } else if (typeof opt === "object" && opt !== null) {
                          // 尝试从对象中提取选项文本
                          optionText =
                            opt.content ||
                            opt.text ||
                            opt.label ||
                            opt.value ||
                            JSON.stringify(opt);
                        }
                        return `${String.fromCharCode(65 + i)}. ${optionText}`;
                      })
                      .join("\n");
                  }
                }
              } catch (e) {
                console.warn("选项解析失败:", item.questionOptions, e);
                // 如果JSON解析失败，尝试直接使用字符串
                if (typeof item.questionOptions === "string") {
                  const lines = item.questionOptions
                    .split("\n")
                    .filter((line) => line.trim() !== "");
                  if (lines.length > 0) {
                    optionsText = lines
                      .map(
                        (line, i) =>
                          `${String.fromCharCode(65 + i)}. ${line.trim()}`
                      )
                      .join("\n");
                  }
                }
              }
            }

            // 处理答案 - 使用ExamPaper期望的格式
            const answer = `答案: ${item.studentAnswer || ""}`;

            // 处理解析
            const explanation = item.explanation
              ? `\n解析: ${item.explanation}`
              : "";

            return `${questionText}\n${optionsText}\n${answer}${explanation}`;
          })
          .join("\n\n");

        // 处理学生答案 - 确保空答案被正确识别
        submission.studentAnswers = homeworkData.map((item) => {
          const answer = item.studentAnswer;
          // 如果答案为空、null、undefined或只包含空白字符，则视为未作答
          if (!answer || (typeof answer === "string" && answer.trim() === "")) {
            return "";
          }
          return answer;
        });
      }

      submissionsData.push(submission);
    }

    submissions.value = submissionsData;
    console.log("学生提交情况:", submissionsData);
  } catch (error) {
    console.error("获取学生提交情况失败:", error);
    ElMessage.error("获取学生提交情况失败");
  } finally {
    tableLoading.value = false;
  }
};

// 组件挂载时获取数据
onMounted(async () => {
  await fetchCourseStudents();
  await fetchAllWork();
});

// 计算属性
const filteredHomework = computed(() => {
  return homeworkList.value;
});

// 方法
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

const createHomework = async () => {
  if (
    !newHomework.value.chapterId ||
    !newHomework.value.title ||
    !newHomework.value.description ||
    !newHomework.value.questionCount ||
    !newHomework.value.difficultyLevel
  ) {
    ElMessage.warning("请填写所有必填项");
    return;
  }

  createLoading.value = true;
  try {
    // 构建AI创建作业的内容描述
    const content = `请为${newHomework.value.title}创建${newHomework.value.questionCount}道题目，难度等级为${newHomework.value.difficultyLevel}。作业要求：${newHomework.value.description}`;

    const response = await teacherService.teacherCreateTest(
      content,
      props.courseId,
      newHomework.value.chapterId
    );

    console.log("智能创建作业成功:", response);
    if (response.data && response.data.success && response.data.data) {
      // 创建新的作业对象
      const newHomeworkData = {
        id: `${props.courseId}-${newHomework.value.chapterId}-${Date.now()}`,
        title: newHomework.value.title,
        chapterId: newHomework.value.chapterId,
        chapterName:
          chapters.value.find(
            (c) => c.chapterId === newHomework.value.chapterId
          )?.chapterName || "",
        description: newHomework.value.description,
        questions: response.data.data || [],
        status: "published",
        createdAt: new Date(),
        submittedCount: 0,
        totalStudents: courseStudents.value.length || 0,
        questionCount: newHomework.value.questionCount,
        difficultyLevel: newHomework.value.difficultyLevel,
      };

      homeworkList.value.unshift(newHomeworkData);
      ElMessage.success("作业创建并发布成功！");
      showCreateDialog.value = false;

      // 重置表单
      newHomework.value = {
        chapterId: "",
        title: "",
        description: "",
        questionCount: 10,
        difficultyLevel: "medium",
      };
    } else {
      ElMessage.error("作业创建失败");
    }
  } catch (error) {
    console.error("智能创建作业失败:", error);
    ElMessage.error("作业创建失败，请重试");
  } finally {
    createLoading.value = false;
  }
};

const viewHomework = (homework) => {
  selectedHomework.value = homework;
  showDetailDialog.value = true;
};

const viewSubmissions = (homework) => {
  selectedHomework.value = homework;
  fetchStudentSubmissions(homework);
  showSubmissionsDialog.value = true;
};

// 计算属性
const submittedCount = computed(() => {
  return submissions.value.filter((s) => s.status === "submitted").length;
});

const totalStudents = computed(() => {
  return courseStudents.value.length;
});

// 获取评阅状态类型
const getReviewStatusType = (status) => {
  const types = {
    pending: "warning",
    reviewed: "success",
    not_submitted: "info",
    ai_reviewed: "primary",
  };
  return types[status] || "info";
};

// 获取评阅状态文本
const getReviewStatusText = (status) => {
  const texts = {
    pending: "待评阅",
    reviewed: "已评阅",
    not_submitted: "未提交",
    ai_reviewed: "AI已评阅",
  };
  return texts[status] || "未知";
};

// 查看作业详情
const viewSubmission = (submission) => {
  selectedSubmission.value = submission;

  // 初始化手动评阅表单
  manualReview.value = {
    comments: submission.comments || "",
    grade: submission.manualGrade || "",
  };

  // 直接传递studentAnswers
  selectedSubmission.value.studentAnswers = submission.studentAnswers || [];

  showSubmissionDetailDialog.value = true;
};

// 开始AI智能评阅
const startAIGrading = async (submission) => {
  submission.aiGradingLoading = true;

  try {
    // 检查是否有作业数据
    if (!submission.homeworkData || submission.homeworkData.length === 0) {
      ElMessage.warning("该学生没有提交作业数据，无法进行AI评阅");
      return;
    }

    // 构建请求参数
    const studentAnswerDTOList = submission.homeworkData.map((item) => ({
      questionId: item.questionId || 0,
      studentAnswer: item.studentAnswer || "",
    }));
    // answerEmpty: !item.studentAnswer || item.studentAnswer.trim() === "",
    // valid: true,
    // formattedAnswer: item.studentAnswer || ""

    // 验证请求数据完整性
    if (!selectedHomework.value?.chapterId) {
      throw new Error("章节ID不能为空");
    }
    if (!props.courseId) {
      throw new Error("课程ID不能为空");
    }
    if (studentAnswerDTOList.length === 0) {
      throw new Error("学生答案列表不能为空");
    }

    const requestData = {
      chapterId: selectedHomework.value.chapterId,
      type: "test", // 作业类型
      courseId: props.courseId,
      studentAnswerDTOList: studentAnswerDTOList,
    };

    console.log("AI评阅请求数据:", requestData);
    console.log("请求数据详情:", {
      chapterId: requestData.chapterId,
      type: requestData.type,
      courseId: requestData.courseId,
      studentAnswerCount: requestData.studentAnswerDTOList.length,
      firstAnswer: requestData.studentAnswerDTOList[0],
    });

    // 调用AI评阅接口
    const response = await teacherService.judgeChapterTest(requestData);
    console.log("AI评阅接口响应:", response);

    if (response.data && response.data.success) {
      const result = response.data.data;

      // 处理AI评阅结果
      const aiReview = {
        feedback:
          result.overallComment ||
          result.feedback ||
          generateAIFeedback(submission),
        criteria: result.criteria || generateAICriteria(submission),
        questionResults: result.questionResults || [],
        // 新增：从接口响应中提取更多评阅信息
        accuracy: result.accuracy || 0,
        totalQuestions: result.totalQuestions || 0,
        correctQuestions: result.correctQuestions || 0,
        wrongQuestions: result.wrongQuestions || 0,
        answeredQuestions: result.answeredQuestions || 0,
        unansweredQuestions: result.unansweredQuestions || 0,
        testLevel: result.testLevel || "未知",
        suggestion: result.suggestion || "",
        durationMinutes: result.durationMinutes || null,
        testStartTime: result.testStartTime || null,
        testEndTime: result.testEndTime || null,
        // 题目类型统计
        questionTypeStats: {
          singleChoice: result.singleChoiceStats || {},
          multipleChoice: result.multipleChoiceStats || {},
          trueFalse: result.trueFalseStats || {},
          fillBlank: result.fillBlankStats || {},
          shortAnswer: result.shortAnswerStats || {},
        },
      };

      submission.aiReview = aiReview;
      submission.reviewStatus = "ai_reviewed";

      ElMessage.success(`${submission.studentName} 的作业AI评阅完成！`);
    } else {
      console.error("AI评阅接口返回失败:", response);
      throw new Error(
        response.data?.message || `AI评阅失败: ${response.status || "未知错误"}`
      );
    }
  } catch (error) {
    console.error("AI评阅失败:", error);
    console.error("错误详情:", {
      message: error.message,
      stack: error.stack,
      response: error.response,
    });

    ElMessage.error(`AI评阅失败：${error.message || "请重试"}`);

    // 如果AI评阅失败，不设置AI评阅状态，保持原状态
    // 只提供基本的评阅信息，不标记为AI已评阅
    const aiReview = {
      feedback: generateAIFeedback(submission),
      criteria: generateAICriteria(submission),
    };
    submission.aiReview = aiReview;
    // 不改变reviewStatus，保持原状态
  } finally {
    submission.aiGradingLoading = false;
  }
};

// 生成AI评阅意见
const generateAIFeedback = (submission) => {
  if (!submission.homeworkData || submission.homeworkData.length === 0) {
    return "作业数据不完整，无法进行AI评阅。";
  }

  const totalQuestions = submission.homeworkData.length;
  const correctAnswers = submission.homeworkData.filter(
    (item) => item.isCorrect
  ).length;
  const accuracy =
    totalQuestions > 0
      ? ((correctAnswers / totalQuestions) * 100).toFixed(1)
      : 0;

  if (accuracy >= 90) {
    return `作业完成度很高，正确率${accuracy}%。答案准确，理解深入，表现优秀。`;
  } else if (accuracy >= 80) {
    return `作业完成度良好，正确率${accuracy}%。大部分答案正确，个别地方需要改进。`;
  } else if (accuracy >= 60) {
    return `作业完成度一般，正确率${accuracy}%。部分答案正确，建议加强相关知识点的学习。`;
  } else {
    return `作业完成度较低，正确率${accuracy}%。建议重新学习相关知识点，提高理解程度。`;
  }
};

// 生成AI评阅标准
const generateAICriteria = (submission) => {
  if (!submission.homeworkData || submission.homeworkData.length === 0) {
    return [
      { name: "作业完整性", description: "作业是否完整提交" },
      { name: "答案正确性", description: "答案的准确程度" },
      { name: "理解深度", description: "对知识点的理解程度" },
    ];
  }

  const totalQuestions = submission.homeworkData.length;
  const correctAnswers = submission.homeworkData.filter(
    (item) => item.isCorrect
  ).length;
  const accuracy = totalQuestions > 0 ? correctAnswers / totalQuestions : 0;

  return [
    {
      name: "作业完整性",
      description:
        submission.status === "submitted" ? "作业已完整提交" : "作业未完整提交",
    },
    {
      name: "答案正确性",
      description: `正确率${(accuracy * 100).toFixed(1)}%`,
    },
    {
      name: "理解深度",
      description:
        accuracy >= 0.8
          ? "理解深入"
          : accuracy >= 0.6
          ? "理解一般"
          : "需要加强学习",
    },
  ];
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
  let successCount = 0;
  let failCount = 0;

  try {
    for (const submission of pendingSubmissions) {
      try {
        await startAIGrading(submission);
        successCount++;
        // 间隔500ms，避免请求过于频繁
        await new Promise((resolve) => setTimeout(resolve, 500));
      } catch (error) {
        console.error(`评阅学生 ${submission.studentName} 的作业失败:`, error);
        failCount++;
      }
    }

    if (failCount === 0) {
      ElMessage.success(`批量评阅完成，共成功评阅 ${successCount} 份作业`);
    } else {
      ElMessage.warning(
        `批量评阅完成，成功 ${successCount} 份，失败 ${failCount} 份`
      );
    }
  } catch (error) {
    console.error("批量评阅过程中出现错误:", error);
    ElMessage.error("批量评阅过程中出现错误");
  } finally {
    batchReviewLoading.value = false;
  }
};

// 保存手动评阅
const saveManualReview = () => {
  if (!selectedSubmission.value) return;

  selectedSubmission.value.comments = manualReview.value.comments;
  selectedSubmission.value.manualGrade = manualReview.value.grade;
  selectedSubmission.value.reviewStatus = "reviewed";

  ElMessage.success("评阅结果保存成功！");
  showSubmissionDetailDialog.value = false;
};

const handleClose = () => {
  showCreateDialog.value = false;
};

// 新增：章节选择变化时触发
const onChapterChange = () => {
  // 当章节选择变化时，清空作业标题和描述，并重置题目数量和难度
  newHomework.value.title = "";
  newHomework.value.description = "";
  newHomework.value.questionCount = 10;
  newHomework.value.difficultyLevel = "medium";
};

// 获取难度等级文本
const getDifficultyText = (difficulty) => {
  const texts = {
    easy: "简单",
    medium: "中等",
    hard: "困难",
  };
  return texts[difficulty] || "未知";
};

// 获取测试等级对应的标签类型
const getTestLevelType = (testLevel) => {
  if (!testLevel) return "info";

  const level = testLevel.toLowerCase();
  if (level.includes("优秀") || level.includes("a") || level.includes("90")) {
    return "success";
  } else if (
    level.includes("良好") ||
    level.includes("b") ||
    level.includes("80")
  ) {
    return "primary";
  } else if (
    level.includes("中等") ||
    level.includes("c") ||
    level.includes("70")
  ) {
    return "warning";
  } else if (
    level.includes("及格") ||
    level.includes("d") ||
    level.includes("60")
  ) {
    return "warning";
  } else if (
    level.includes("不及格") ||
    level.includes("f") ||
    level.includes("0")
  ) {
    return "danger";
  } else {
    return "info";
  }
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

.homework-description p {
  margin: 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #3b82f6;
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
  padding: 4px 8px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 13px;
}

.homework-meta span:hover {
  background: #e9ecef;
  transition: background-color 0.2s ease;
}

/* 班级信息样式 */
.class-info {
  background: #e0f2fe;
  color: #0277bd;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
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

.ai-review-section h6 {
  margin: 0 0 12px 0;
  color: #0369a1;
  font-size: 14px;
  font-weight: 600;
}

.ai-review-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 总体评阅信息样式 */
.overall-review-section {
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e0f2fe;
}

.review-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-label {
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.summary-value {
  font-weight: 600;
  color: #059669;
  font-size: 16px;
}

/* 题目数量显示样式 */
.question-count-section {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.count-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-label {
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.count-value {
  font-weight: 600;
  color: #2563eb;
  font-size: 16px;
}

/* 答题统计样式 */
.answer-stats-section {
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e0f2fe;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #374151;
}

.stat-value.correct {
  color: #059669;
}

.stat-value.wrong {
  color: #dc2626;
}

/* 题目类型统计样式 */
.question-type-stats {
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e0f2fe;
}

.type-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.type-stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f1f5f9;
  border-radius: 6px;
  border-left: 3px solid #3b82f6;
}

.type-label {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
}

.type-value {
  font-size: 14px;
  color: #2563eb;
  font-weight: 600;
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

/* 学习建议样式 */
.suggestion-section {
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e0f2fe;
}

.suggestion-label {
  font-weight: 600;
  color: #374151;
  display: block;
  margin-bottom: 8px;
}

.suggestion-content {
  margin: 0;
  padding: 12px;
  background: #fef3c7;
  border-radius: 6px;
  line-height: 1.6;
  color: #92400e;
  border-left: 3px solid #f59e0b;
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

.ai-review-reference {
  margin-bottom: 20px;
}

.ai-review-reference .el-alert {
  margin-bottom: 0;
}

.ai-review-reference p {
  margin: 4px 0;
  font-size: 13px;
  color: #4b5563;
}

.ai-reference-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-reference-content p {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-reference-content strong {
  color: #374151;
  min-width: 80px;
}

/* 新增样式 */
.text-muted {
  color: #9ca3af;
  font-style: italic;
}

/* 智能创建作业表单样式 */
.create-tips {
  margin-bottom: 20px;
}

.create-tips .el-alert {
  margin-bottom: 0;
}

.create-tips p {
  margin: 4px 0;
  font-size: 13px;
  color: #4b5563;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-select,
.el-input-number {
  width: 100%;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #2d58da 0%, #366de7 100%);
  border: none;
  transition: all 0.3s ease;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* 难度等级标签样式 */
.difficulty-tag {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.difficulty-easy {
  background: #d1fae5;
  color: #065f46;
}

.difficulty-medium {
  background: #fef3c7;
  color: #92400e;
}

.difficulty-hard {
  background: #fee2e2;
  color: #991b1b;
}
</style>
