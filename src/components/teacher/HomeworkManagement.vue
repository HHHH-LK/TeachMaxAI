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
          <div class="homework-meta">
            <span
            ><el-icon><User /></el-icon> 已提交:
              {{ homework.submittedCount }}/{{ homework.totalStudents }}</span
            >
            <span v-if="homework.questions && homework.questions.length > 0"
            ><el-icon><QuestionFilled /></el-icon> 题目数量:
              {{ homework.questions.length }}</span
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
          <el-table-column 
              label="班级信息" 
              width="150"
              v-if="submissions.some(s => s.studentDetail?.className)"
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
          <el-table-column 
              label="得分" 
              width="100"
              v-if="submissions.some(s => s.score !== undefined)"
          >
            <template #default="scope">
              <span v-if="scope.row.score !== undefined" :class="getScoreClass(scope.row.score)">
                {{ scope.row.score }}/{{ scope.row.maxScore || 100 }}
              </span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
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
                :chapterId="selectedHomework?.chapterId || ''"
                :courseId="props.courseId"
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
import { ref, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  Plus,
  User,
  MagicStick,
  QuestionFilled,
} from "@element-plus/icons-vue";
import ExamPaper from "@/components/ExamPaper.vue";
import { teacherService, studentService } from "@/services/api.js";

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

// 课程和学生数据
const courseStudents = ref([]);
// 使用props中的courseId，而不是硬编码
// const courseId = ref('1'); // 删除这行

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

// 获取课程学生列表
const fetchCourseStudents = async () => {
  try {
    const response = await teacherService.getClassStudents(props.courseId);
    console.log('获取课程学生列表:', response);
    if (response.data && response.data.success && response.data.data) {
      courseStudents.value = response.data.data;
      console.log('课程学生列表:', courseStudents.value);
    }
  } catch (error) {
    console.error('获取课程学生列表失败:', error);
    ElMessage.error('获取课程学生列表失败');
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
    console.error('获取学生详细信息失败:', error);
    return null;
  }
};

// 获取学生作业信息
const getStudentHomework = async (studentId, courseId, chapterId) => {
  try {
    const response = await teacherService.getHomeworkByStudent(studentId, courseId, chapterId);
    console.log('获取学生作业信息:', response);
    if (response.data && response.data.success && response.data.data) {
      return response.data.data;
    }
    return [];
  } catch (error) {
    console.error('获取学生作业信息失败:', error);
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
      const chapters = responseChapter.data.data;
      let homeworkNumber = 1;
      
      for (const chapter of chapters) {
        const response = await teacherService.getHomework(props.courseId, chapter.chapterId);
        console.log("homework", response);
        
        if (response.data && response.data.data && response.data.data.length > 0) {
          const homework = {
            id: `${props.courseId}-${chapter.chapterId}-${homeworkNumber}`,
            title: `第${homeworkNumber}次作业`,
            chapterId: chapter.chapterId,
            chapterName: chapter.chapterName,
            questions: [],
            maxScore: 100,
            status: "published",
            createdAt: new Date(),
            submittedCount: 0,
            totalStudents: courseStudents.value.length || 0,
          };
          
          homework.questions = response.data.data.map((item) => {
            let parsedOptions = [];
            try {
              parsedOptions = JSON.parse(item.questionOptions || '[]');
            } catch (e) {
              console.warn("选项解析失败:", item.questionOptions);
              parsedOptions = [];
            }

            return {
              id: item.questionId,
              type: item.questionType,
              text: item.questionContent,
              options: parsedOptions,
              answer: item.correctAnswer ? item.correctAnswer.replace(/"/g, "") : "",
              explanation: item.explanation,
              difficultyLevel: item.difficultyLevel,
              scorePoints: item.scorePoints,
              pointName: item.pointName,
            };
          });

          if (homework.questions.length > 0) {
            homeworkNumber++;
            homeworkList.value.push(homework);
          }
        }
      }
    }
  } catch (error) {
    console.error("获取作业列表失败:", error);
    ElMessage.error("获取作业列表失败");
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
        score: undefined,
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
        submission.examContent = homeworkData.map((item, index) => {
          // 转换题目类型为ExamPaper期望的格式
          let questionType = 'question';
          if (item.questionType) {
            switch (item.questionType.toLowerCase()) {
              case 'single':
              case 'single_choice':
              case 'single-choice':
                questionType = 'single-choice';
                break;
              case 'multiple':
              case 'multiple_choice':
              case 'multiple-choice':
                questionType = 'multiple-choice';
                break;
              case 'fill':
              case 'fill_blank':
              case 'fill-in-blank':
                questionType = 'fill-in-blank';
                break;
              case 'judge':
              case 'true_false':
              case 'true-false':
                questionType = 'true-false';
                break;
              case 'short':
              case 'short_answer':
              case 'short-answer':
                questionType = 'short-answer';
                break;
              default:
                questionType = 'question';
            }
          }
          
          const questionText = `${index + 1}. [${questionType}] ${item.questionContent || ''}`;
          
                  // 处理选项 - 修复[object Object]问题
        let optionsText = '';
        if (item.questionOptions) {
          try {
            const options = JSON.parse(item.questionOptions);
            
            if (Array.isArray(options) && options.length > 0) {
              // 过滤掉无效的选项内容
              const validOptions = options.filter(opt => {
                if (typeof opt === 'string') return opt && opt.trim() !== '';
                if (typeof opt === 'object' && opt !== null) {
                  // 如果是对象，尝试提取有用的字段
                  return opt.content || opt.text || opt.label || opt.value;
                }
                return false;
              });
              
              if (validOptions.length > 0) {
                optionsText = validOptions.map((opt, i) => {
                  let optionText = '';
                  if (typeof opt === 'string') {
                    optionText = opt;
                  } else if (typeof opt === 'object' && opt !== null) {
                    // 尝试从对象中提取选项文本
                    optionText = opt.content || opt.text || opt.label || opt.value || JSON.stringify(opt);
                  }
                  return `${String.fromCharCode(65 + i)}. ${optionText}`;
                }).join('\n');
              }
            }
          } catch (e) {
            console.warn('选项解析失败:', item.questionOptions, e);
            // 如果JSON解析失败，尝试直接使用字符串
            if (typeof item.questionOptions === 'string') {
              const lines = item.questionOptions.split('\n').filter(line => line.trim() !== '');
              if (lines.length > 0) {
                optionsText = lines.map((line, i) => `${String.fromCharCode(65 + i)}. ${line.trim()}`).join('\n');
              }
            }
          }
        }
          
          // 处理答案 - 使用ExamPaper期望的格式
          const answer = `答案: ${item.studentAnswer || ''}`;
          
          // 处理解析
          const explanation = item.explanation ? `\n解析: ${item.explanation}` : '';
          
          return `${questionText}\n${optionsText}\n${answer}${explanation}`;
        }).join('\n\n');
        
        // 处理学生答案
        submission.studentAnswers = homeworkData.map(item => item.studentAnswer || '');
        
        // 计算得分
        let totalScore = 0;
        let earnedScore = 0;
        homeworkData.forEach(item => {
          totalScore += item.scorePoints || 0;
          if (item.isCorrect) {
            earnedScore += item.scorePoints || 0;
          }
        });
        submission.score = earnedScore;
        submission.maxScore = totalScore;
      }
      
      submissionsData.push(submission);
    }
    
    submissions.value = submissionsData;
    console.log('学生提交情况:', submissionsData);
    
  } catch (error) {
    console.error('获取学生提交情况失败:', error);
    ElMessage.error('获取学生提交情况失败');
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

// 获取成绩样式类
const getScoreClass = (score) => {
  if (!score && score !== 0) return "no-score";
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

    // 基于真实作业数据生成AI评阅结果
    const aiReview = {
      suggestedScore: submission.score || Math.floor(Math.random() * 30) + 70, // 使用实际得分或随机70-100分
      feedback: generateAIFeedback(submission),
      criteria: generateAICriteria(submission),
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
const generateAIFeedback = (submission) => {
  if (!submission.homeworkData || submission.homeworkData.length === 0) {
    return "作业数据不完整，无法进行AI评阅。";
  }
  
  const totalQuestions = submission.homeworkData.length;
  const correctAnswers = submission.homeworkData.filter(item => item.isCorrect).length;
  const accuracy = totalQuestions > 0 ? (correctAnswers / totalQuestions * 100).toFixed(1) : 0;
  
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

// 生成AI评分标准
const generateAICriteria = (submission) => {
  if (!submission.homeworkData || submission.homeworkData.length === 0) {
    return [
      { name: "作业完整性", score: 0 },
      { name: "答案正确性", score: 0 },
      { name: "理解深度", score: 0 },
    ];
  }
  
  const totalQuestions = submission.homeworkData.length;
  const correctAnswers = submission.homeworkData.filter(item => item.isCorrect).length;
  const accuracy = totalQuestions > 0 ? (correctAnswers / totalQuestions) : 0;
  
  return [
    { name: "作业完整性", score: Math.round(20 * (submission.status === "submitted" ? 1 : 0)) },
    { name: "答案正确性", score: Math.round(50 * accuracy) },
    { name: "理解深度", score: Math.round(30 * accuracy) },
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
  padding: 4px 8px;
  background: #f8f9fa;
  border-radius: 4px;
  font-size: 13px;
}

.homework-meta span:hover {
  background: #e9ecef;
  transition: background-color 0.2s ease;
}

/* 得分显示样式优化 */
.score-display {
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f8f9fa;
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

/* 新增样式 */
.text-muted {
  color: #9ca3af;
  font-style: italic;
}
</style>
