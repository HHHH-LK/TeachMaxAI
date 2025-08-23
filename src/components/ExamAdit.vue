<template>
  <div class="exam-attempt">
    <h2 class="exam-title">{{ paper.title }}</h2>
    <div class="exam-info">
      <span>总分：{{ totalScore }}分</span>
      <span>题目数：{{ paper.questions.length }}</span>
    </div>

    <!-- 添加题目按钮 -->
    <div class="add-question-btn">
      <el-button type="primary" @click="showAddQuestionDialog = true">
        <el-icon><Plus /></el-icon>添加题目
      </el-button>
    </div>
    <div v-for="(q, idx) in paper.questions" :key="q.id" class="question-block">
      <div class="question-header">
        <span class="q-index">{{ idx + 1 }}.</span>
        <span class="q-title">
          <el-input
            v-if="editingQuestionId === q.id"
            v-model="editingQuestion.title"
            placeholder="请输入题目"
            size="small"
          />
          <span v-else>{{ q.title }}</span>
        </span>
        <!-- 修改分数功能 -->
        <span class="q-score">
          <!-- 修复：使用v-model.number确保数字类型 -->
          <el-input-number
            v-if="editingQuestionId === q.id"
            v-model.number="editingQuestion.score"
            :min="1"
            :max="100"
            size="small"
            style="width: 100px;"
          />
          <span v-else>（{{ q.score }}分）</span>
        </span>
        <span class="q-type">[{{ getTypeText(q.type) }}]</span>
        
        <!-- 编辑按钮 -->
        <div class="question-actions">
          <el-button
            v-if="!editingQuestionId"
            type="primary"
            size="small"
            @click="startEditQuestion(q)"
          >
            编辑
          </el-button>
          <el-button
            v-if="!editingQuestionId"
            type="danger"
            size="small"
            @click="deleteQuestion(q.id)"
          >
            删除
          </el-button>
          <el-button
            v-if="editingQuestionId === q.id"
            type="success"
            size="small"
            @click="saveQuestionEdit(q)"
          >
            保存
          </el-button>
          <el-button
            v-if="editingQuestionId === q.id"
            type="info"
            size="small"
            @click="cancelEdit"
          >
            取消
          </el-button>
        </div>
      </div>

      <div class="question-body">
        <!-- 单选题 -->
        <template v-if="q.type === 'single'">
          <div v-if="editingQuestionId === q.id" class="edit-options">
            <div
              v-for="(opt, optIdx) in editingQuestion.options"
              :key="optIdx"
              class="option-item"
            >
              <!-- 修复：使用v-model绑定到选项对象的属性 -->
              <el-input
                v-model="opt.label"
                placeholder="选项标签 (如 A、B)"
                size="small"
                style="width: 100px; margin-right: 10px"
              />
              <el-input
                v-model="opt.content"
                placeholder="选项内容"
                size="small"
                style="width: 300px; margin-right: 10px"
              />
              <el-button
                type="danger"
                size="small"
                circle
                @click="removeOption(optIdx)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" size="small" @click="addOption">
              <el-icon><Plus /></el-icon>添加选项
            </el-button>

            <!-- 添加正确答案输入框 -->
            <div class="correct-answer-input">
              <label>正确答案：</label>
              <el-input
                v-model="editingQuestion.correctAnswerLabel"
                placeholder="请输入正确答案的大写字母 (如 A)"
                size="small"
                style="width: 200px; margin-left: 10px"
                @input="validateAnswerInput"
              />
            </div>
          </div>
          <el-radio-group
            v-else
            v-model="userAnswers[q.id]"
            :disabled="readonly"
          >
            <el-radio
              v-for="opt in q.options"
              :key="opt.value"
              :value="opt.value"
            >
              <span v-if="opt.label">{{ opt.label }}. </span>{{ opt.content }}
            </el-radio>
          </el-radio-group>
        </template>

        <!-- 多选题 -->
        <template v-else-if="q.type === 'multiple'">
          <div v-if="editingQuestionId === q.id" class="edit-options">
            <div
              v-for="(opt, optIdx) in editingQuestion.options"
              :key="optIdx"
              class="option-item"
            >
              <!-- 修复：使用v-model绑定到选项对象的属性 -->
              <el-input
                v-model="opt.label"
                placeholder="选项标签 (如 A、B)"
                size="small"
                style="width: 100px; margin-right: 10px"
              />
              <el-input
                v-model="opt.content"
                placeholder="选项内容"
                size="small"
                style="width: 300px; margin-right: 10px"
              />
              <el-button
                type="danger"
                size="small"
                circle
                @click="removeOption(optIdx)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" size="small" @click="addOption">
              <el-icon><Plus /></el-icon>添加选项
            </el-button>

            <!-- 添加正确答案输入框 -->
            <div class="correct-answer-input">
              <label>正确答案：</label>
              <el-input
                v-model="editingQuestion.correctAnswerLabel"
                placeholder="请输入正确答案的大写字母 (如 A,B)"
                size="small"
                style="width: 200px; margin-left: 10px"
                @input="validateAnswerInput"
              />
            </div>
          </div>
          <el-checkbox-group
            v-else
            v-model="userAnswers[q.id]"
            :disabled="readonly"
          >
            <el-checkbox
              v-for="opt in q.options"
              :key="opt.value"
              :value="opt.value"
            >
              <span v-if="opt.label">{{ opt.label }}. </span>{{ opt.content }}
            </el-checkbox>
          </el-checkbox-group>
        </template>

        <!-- 判断题 -->
        <template v-else-if="q.type === 'judge'">
          <div v-if="editingQuestionId === q.id" class="edit-options">
            <div class="correct-answer-input">
              <label>正确答案：</label>
              <el-select
                v-model="editingQuestion.correctAnswer"
                placeholder="请选择"
              >
                <el-option label="正确" :value="true" />
                <el-option label="错误" :value="false" />
              </el-select>
            </div>
          </div>
          <el-radio-group
            v-else
            v-model="userAnswers[q.id]"
            :disabled="readonly"
          >
            <el-radio :value="true">正确</el-radio>
            <el-radio :value="false">错误</el-radio>
          </el-radio-group>
        </template>

        <!-- 简答题 -->
        <template v-else-if="q.type === 'short'">
          <div v-if="editingQuestionId === q.id">
            <div class="correct-answer-input">
              <label>正确答案：</label>
              <el-input
                v-model="editingQuestion.correctAnswer"
                type="textarea"
                placeholder="请输入正确答案"
                :rows="3"
              />
            </div>
          </div>
          <el-input
            v-else
            type="textarea"
            v-model="userAnswers[q.id]"
            :disabled="readonly"
            :rows="3"
          />
        </template>

        <!-- 填空题 -->
        <template v-else-if="q.type === 'blank'">
          <div v-if="editingQuestionId === q.id">
            <div class="correct-answer-input">
              <label>正确答案：</label>
              <el-input
                v-model="editingQuestion.correctAnswer"
                placeholder="请输入正确答案"
              />
            </div>
          </div>
          <el-input
            v-else
            v-model="userAnswers[q.id]"
            :disabled="readonly"
            placeholder="请填写答案"
          />
        </template>
      </div>

      <!-- 新增：题目解析编辑 -->
      <div v-if="editingQuestionId === q.id" class="analysis-edit">
        <div class="analysis-title">题目解析：</div>
        <el-input
          v-model="editingQuestion.explanation"
          type="textarea"
          placeholder="请输入题目解析"
          :rows="3"
        />
      </div>

      <!-- 新增：只读时显示学生答案和正确答案 -->
      <div
        v-if="readonly && showUserAnswers && editingQuestionId !== q.id"
        class="answer-compare"
      >
        <div>学生答案：{{ formatUserAnswer(userAnswers[q.id], q) }}</div>
        <div
          v-if="
            wrongAnalysis && wrongAnalysis[q.id] && wrongAnalysis[q.id].answer
          "
        >
          正确答案：{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}
        </div>
      </div>
      <!-- 只显示正确答案 -->
      <div
        v-if="
          readonly &&
          !showUserAnswers &&
          wrongAnalysis &&
          wrongAnalysis[q.id] &&
          wrongAnalysis[q.id].answer &&
          editingQuestionId !== q.id
        "
        class="answer-compare"
      >
        <div>
          正确答案：{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}
        </div>
      </div>
      <!-- 错题解析（只读且有解析时显示） -->
      <div
        v-if="
          readonly &&
          wrongAnalysis &&
          wrongAnalysis[q.id] &&
          editingQuestionId !== q.id
        "
        class="wrong-analysis"
      >
        <div class="analysis-title">题目解析：</div>
        <div class="analysis-content">
          <div v-if="wrongAnalysis[q.id].explanation">
            {{ wrongAnalysis[q.id].explanation }}
          </div>
          <div v-if="wrongAnalysis[q.id].answer">
            正确答案：{{ formatCorrectAnswer(wrongAnalysis[q.id].answer, q) }}
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showAddQuestionDialog"
      title="添加新题目"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="add-question-form">
        <el-form label-width="100px">
          <el-form-item label="题目类型">
            <el-select v-model="newQuestion.type" placeholder="请选择题目类型">
              <el-option label="单选题" value="single"></el-option>
              <el-option label="多选题" value="multiple"></el-option>
              <el-option label="判断题" value="judge"></el-option>
              <el-option label="简答题" value="short"></el-option>
              <el-option label="填空题" value="blank"></el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="题目内容">
            <el-input
              v-model="newQuestion.title"
              type="textarea"
              placeholder="请输入题目内容"
              :rows="3"
            />
          </el-form-item>

          <el-form-item label="题目分值">
            <el-input-number
              v-model="newQuestion.score"
              :min="1"
              :max="100"
              placeholder="请输入分值"
            />
          </el-form-item>

          <!-- 选项编辑区域（仅选择题显示） -->
          <template v-if="['single', 'multiple'].includes(newQuestion.type)">
            <el-form-item label="选项设置">
              <div class="option-list">
                <div
                  v-for="(opt, index) in newQuestion.options"
                  :key="index"
                  class="option-item"
                >
                  <el-input
                    v-model="opt.label"
                    placeholder="选项标签 (如 A)"
                    size="small"
                    style="width: 100px; margin-right: 10px"
                  />
                  <el-input
                    v-model="opt.content"
                    placeholder="选项内容"
                    size="small"
                    style="width: 300px; margin-right: 10px"
                  />
                  <el-button
                    type="danger"
                    size="small"
                    circle
                    @click="removeNewOption(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <el-button type="primary" size="small" @click="addNewOption">
                  <el-icon><Plus /></el-icon>添加选项
                </el-button>
              </div>
            </el-form-item>

            <el-form-item label="正确答案">
              <el-input
                v-model="newQuestion.correctAnswerLabel"
                :placeholder="
                  newQuestion.type === 'single'
                    ? '请输入正确答案的大写字母 (如 A)'
                    : '请输入正确答案的大写字母 (如 A,B)'
                "
                size="small"
                style="width: 300px"
              />
            </el-form-item>
          </template>

          <!-- 判断题正确答案 -->
          <el-form-item v-if="newQuestion.type === 'judge'" label="正确答案">
            <el-radio-group v-model="newQuestion.correctAnswer">
              <el-radio :label="true">正确</el-radio>
              <el-radio :label="false">错误</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 简答题/填空题正确答案 -->
          <el-form-item
            v-if="['short', 'blank'].includes(newQuestion.type)"
            label="正确答案"
          >
            <el-input
              v-model="newQuestion.correctAnswer"
              :type="newQuestion.type === 'short' ? 'textarea' : 'text'"
              :placeholder="
                newQuestion.type === 'short'
                  ? '请输入正确答案'
                  : '请输入填空答案'
              "
              :rows="newQuestion.type === 'short' ? 3 : 1"
            />
          </el-form-item>

          <el-form-item label="题目解析">
            <el-input
              v-model="newQuestion.explanation"
              type="textarea"
              placeholder="请输入题目解析"
              :rows="3"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showAddQuestionDialog = false">取消</el-button>
        <el-button type="primary" @click="addQuestionToPaper">添加</el-button>
      </template>
    </el-dialog>
    <div v-if="!readonly" class="exam-actions">
      <el-button type="primary" @click="submitPaper">提交试卷</el-button>
    </div>
    <div v-else class="readonly-tip">本试卷为只读模式，仅供查看。</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Plus } from "@element-plus/icons-vue";
import axios from "axios"; 
import { teacherService } from "@/services/api";

// props: paper(试卷对象), readonly(是否只读), wrongAnalysis(错题解析对象)
const props = defineProps({
  paper: {
    type: Object,
    required: true,
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  wrongAnalysis: {
    type: Object,
    default: () => ({}),
  },
  userAnswers: {
    type: Object,
    default: undefined,
  },
  showUserAnswers: {
    type: Boolean,
    default: true,
  },
  courseId: {
    type: [String, Number],
    default: 1,
  }
});

const emit = defineEmits(["submit", "update-question", "delete-question", "add-question"]);

// 用户答案
const userAnswers = ref({});

// 编辑状态
const editingQuestionId = ref(null);
const editingQuestion = ref({
  id: null,
  title: "",
  type: "",
  score: 5, // 添加分数属性
  options: [],
  correctAnswer: null,
  correctAnswerLabel: "",
  explanation: ""
});

// 优先使用外部传入的userAnswers
watch(
  [() => props.paper, () => props.userAnswers, () => props.readonly],
  ([paper, extUserAnswers, readonly]) => {
    if (readonly && extUserAnswers && Object.keys(extUserAnswers).length > 0) {
      userAnswers.value = { ...extUserAnswers };
    } else if (readonly && paper && paper.userAnswers) {
      userAnswers.value = { ...paper.userAnswers };
    } else {
      userAnswers.value = {};
    }
  },
  { immediate: true }
);

const totalScore = computed(() => {
  return props.paper.questions.reduce((sum, q) => sum + (q.score || 0), 0);
});

function getTypeText(type) {
  switch (type) {
    case "single": return "单选";
    case "multiple": return "多选";
    case "judge": return "判断";
    case "short": return "简答";
    case "blank": return "填空";
    default: return "未知";
  }
}

function submitPaper() {
  // 只提交答案，不显示正确答案
  emit("submit", { answers: { ...userAnswers.value } });
  ElMessage.success("试卷已提交！");
}

function formatUserAnswer(val, q) {
  if (q.type === "multiple" && Array.isArray(val)) {
    return val.join(", ");
  }
  if (q.type === "judge") {
    if (val === true || val === "true") return "正确";
    if (val === false || val === "false") return "错误";
  }
  return val !== undefined && val !== null ? val : "未作答";
}

function formatCorrectAnswer(val, q) {
  if (!val) return "";
  
  // 确保val是字符串类型
  const strVal = String(val);
  
  if (q.type === "multiple") {
    // 如果是数组，直接join
    if (Array.isArray(val)) {
      return val.join(", ");
    }
    // 如果是字符串，按逗号分割
    return strVal.split(",").map(a => a.trim()).join(", ");
  }
  if (q.type === "judge") {
    if (val === true || val === "true") return "正确";
    if (val === false || val === "false") return "错误";
    if (val === "正确" || val === "错误") return val;
  }
  return strVal;
}

// 开始编辑题目
function startEditQuestion(question) {
  editingQuestionId.value = question.id;
  
  // 初始化正确答案标签
  let correctAnswerLabel = "";
  if (question.correctAnswer) {
    if (["single", "multiple"].includes(question.type)) {
      // 查找对应的选项标签
      const correctOption = question.options.find(
        (opt) => 
          opt.value === question.correctAnswer || 
          (Array.isArray(question.correctAnswer) && question.correctAnswer.includes(opt.value))
      );
      
      if (correctOption) {
        if (question.type === "single") {
          correctAnswerLabel = correctOption.label;
        } else {
          // 多选题：获取所有正确答案的标签
          correctAnswerLabel = question.options
            .filter(opt => question.correctAnswer.includes(opt.value))
            .map(opt => opt.label)
            .join(",");
        }
      }
    } else {
      // 其他题型直接使用原值
      correctAnswerLabel = question.correctAnswer;
    }
  }
  
  // 修复：确保使用深拷贝，避免引用问题
  editingQuestion.value = {
    id: question.id,
    title: question.title,
    type: question.type,
    score: question.score, // 使用原题目的分数作为默认值
    options: question.options ? JSON.parse(JSON.stringify(question.options)) : [],
    correctAnswer: question.correctAnswer,
    correctAnswerLabel: correctAnswerLabel, // 设置正确答案标签
    explanation: question.explanation || "" // 初始化解析
  };
}

// 映射前端题目类型到后端类型
const questionTypeMap = {
  single: "single_choice",
  multiple: "multiple_choice",
  judge: "true_false",
  short: "short_answer",
  blank: "fill_blank"
};

// 将前端题目对象转换为后端实体
function convertToBackendQuestion(frontendQuestion) {
  console.log("转换前端题目对象:", frontendQuestion);
  
  // 创建基础后端实体
  const backendQuestion = {
    courseId: props.courseId,
    pointId: frontendQuestion.pointId || 0,
    questionType: questionTypeMap[frontendQuestion.type] || frontendQuestion.type,
    questionContent: frontendQuestion.title,
    scorePoints: frontendQuestion.scorePoints,
    explanation: frontendQuestion.explanation,
    difficultyLevel: "medium",
    createdBy: 1,
    questionOptions: null // 初始化为null
  };

  // 处理所有题型的正确答案
  if (frontendQuestion.correctAnswer) {
    if (["single", "multiple"].includes(frontendQuestion.type)) {
      backendQuestion.correctAnswer = JSON.stringify(frontendQuestion.correctAnswer);
    } else {
      backendQuestion.correctAnswer = JSON.stringify(frontendQuestion.correctAnswer);
    }
  }

  // 处理所有题型的选项
  if (frontendQuestion.options && frontendQuestion.options.length > 0) {
    let optionsJson;
    
    if (["single", "multiple"].includes(frontendQuestion.type)) {
      // 选择题：添加is_correct属性
      const correctAnswerLabels = frontendQuestion.type === "single"
        ? [frontendQuestion.correctAnswer] 
        : frontendQuestion.correctAnswer;
      
      optionsJson = frontendQuestion.options.map(opt => ({
        label: opt.label,
        content: opt.content,
        is_correct: correctAnswerLabels.includes(opt.label)
      }));
    } else {
      // 非选择题：只包含基本属性
      optionsJson = frontendQuestion.options.map(opt => ({
        label: opt.label,
        content: opt.content
      }));
    }
    
    console.log("选项JSON对象:", optionsJson);
    backendQuestion.questionOptions = JSON.stringify(optionsJson);
  }

  console.log("转换后的后端实体:", backendQuestion);
  return backendQuestion;
}

function saveQuestionEdit(question) {
  // 转换正确答案标签为实际值
  let correctAnswer = null;
  
  if (["single", "multiple"].includes(editingQuestion.value.type)) {
    if (editingQuestion.value.correctAnswerLabel) {
      if (editingQuestion.value.type === "single") {
        // 单选题：直接存储标签（如"A"）
        correctAnswer = editingQuestion.value.correctAnswerLabel;
      } else {
        // 多选题：存储标签数组（如["A","C"]）
        correctAnswer = editingQuestion.value.correctAnswerLabel.split(",").map(l => l.trim());
      }
    }
  } else if (editingQuestion.value.type === "judge") {
    // 判断题
    correctAnswer = editingQuestion.value.correctAnswer;
  } else {
    // 简答题和填空题
    correctAnswer = editingQuestion.value.correctAnswer;
  }
  
  // 更新题目数据
  const updatedQuestion = {
    ...question,
    title: editingQuestion.value.title,
    type: editingQuestion.value.type,
    scorePoints: editingQuestion.value.score, // 修复：使用编辑后的分数
    options: JSON.parse(JSON.stringify(editingQuestion.value.options)), // 深拷贝选项
    correctAnswer: correctAnswer,
    explanation: editingQuestion.value.explanation
  };
  
  // 转换为后端实体
  const backendQuestion = convertToBackendQuestion(updatedQuestion);
  backendQuestion.questionId = question.id; // 设置题目ID
  
  console.log("准备更新题目:", backendQuestion);
  // 调用API更新题目
  updateExamQuestion(backendQuestion);
  
  // 退出编辑状态
  editingQuestionId.value = null;
}

// 更新题目API调用
async function updateExamQuestion(question) {
  try {
    // 准备API所需的数据
    const requestData = {
      questionId: question.questionId,
      courseId: question.courseId,
      pointId: question.pointId,
      questionType: question.questionType,
      questionContent: question.questionContent,
      questionOptions: question.questionOptions, // 修改字段名
      correctAnswer: question.correctAnswer,
      explanation: question.explanation,
      difficultyLevel: question.difficultyLevel,
      scorePoints: question.scorePoints,
      createdBy: question.createdBy
    };
    
    console.log("更新题目请求数据:", requestData);
    
    // 调用teacherService更新题目
    const response = await teacherService.updateQuestion(requestData);
    
    if (response.data && response.data.success) {
      ElMessage.success("题目更新成功");
      // 通知父组件更新
      emit("update-question", question);
    } else {
      ElMessage.error("题目更新失败: " + (response.data?.message || "未知错误"));
    }
  } catch (error) {
    ElMessage.error("请求失败: " + error.message);
  }
}

// 取消编辑
function cancelEdit() {
  editingQuestionId.value = null;
}

// 添加选项（选择题）
function addOption() {
  const newOption = {
    value: `option${editingQuestion.value.options.length + 1}`,
    label: `选项${editingQuestion.value.options.length + 1}`,
    content: `选项内容${editingQuestion.value.options.length + 1}`
  };
  editingQuestion.value.options.push(newOption);
}

// 移除选项
function removeOption(index) {
  editingQuestion.value.options.splice(index, 1);
}

// 验证答案输入
function validateAnswerInput(value) {
  // 只允许输入大写字母和逗号
  const regex = /^[A-Z,]*$/;
  if (!regex.test(value)) {
    // 过滤非法字符
    editingQuestion.value.correctAnswerLabel = value.replace(/[^A-Z,]/g, "");
  }
}

// 删除题目
function deleteQuestion(questionId) {
  ElMessageBox.confirm("确定要删除这道题目吗？", "删除确认", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    // 通知父组件删除题目
    // console.log("Deleting question with ID:", questionId);
    deletePaperQuestion(questionId);
    // emit("delete-question", questionId);
    ElMessage.success("题目已删除");
  }).catch(() => {
    // 取消删除
  });
}

// 删除题目API调用
async function deletePaperQuestion(questionId) {
  try {
    console.log("Deleting question with ID:", questionId);
    const response = await teacherService.deleteQuestion({
      questionId: questionId,
      examId: props.paper.examId // 试卷ID
    });
    if (response.data.success) {
      ElMessage.success("题目删除成功");
      // 通知父组件删除题目
      emit("delete-question", questionId);
    } else {
      ElMessage.error("题目删除失败: " + response.data.message);
    }
  } catch (error) {
    ElMessage.error("请求失败: " + error.message);
  }
}


// 添加题目相关状态
const showAddQuestionDialog = ref(false);
const newQuestion = ref({
  id: null,
  title: '',
  type: 'single',
  score: 5,
  options: [
    { value: 'option1', label: 'A', content: '选项内容1' },
    { value: 'option2', label: 'B', content: '选项内容2' }
  ],
  correctAnswer: null,
  correctAnswerLabel: '',
  explanation: ''
});

// 添加新选项
function addNewOption() {
  const newOption = {
    value: `option${newQuestion.value.options.length + 1}`,
    label: `选项${newQuestion.value.options.length + 1}`,
    content: `选项内容${newQuestion.value.options.length + 1}`
  };
  newQuestion.value.options.push(newOption);
}

// 移除选项
function removeNewOption(index) {
  newQuestion.value.options.splice(index, 1);
}

// 添加题目到试卷
function addQuestionToPaper() {
  // 验证题目内容
  if (!newQuestion.value.title.trim()) {
    ElMessage.warning('请输入题目内容');
    return;
  }
  
  // 验证选择题选项
  if (['single', 'multiple'].includes(newQuestion.value.type)) {
    // 检查是否有选项内容为空
    const emptyOption = newQuestion.value.options.find(
      opt => !opt.content.trim()
    );
    
    if (emptyOption) {
      ElMessage.warning('请填写所有选项内容');
      return;
    }
    
    // 验证答案是否为空
    if (!newQuestion.value.correctAnswerLabel.trim()) {
      ElMessage.warning('请输入正确答案');
      return;
    }
    
    // 验证答案是否在选项中
    const labels = newQuestion.value.options.map(opt => opt.label);
    const answerLabels = newQuestion.value.correctAnswerLabel.split(',').map(l => l.trim());
    
    for (const label of answerLabels) {
      if (!labels.includes(label)) {
        ElMessage.warning(`选项${label}不存在`);
        return;
      }
    }
    
    // 设置正确答案
    newQuestion.value.correctAnswer = newQuestion.value.type === 'single' 
      ? newQuestion.value.options.find(opt => opt.label === answerLabels[0])?.value
      : newQuestion.value.options.filter(opt => answerLabels.includes(opt.label)).map(opt => opt.value);
  }
  
  // 验证判断题答案
  if (newQuestion.value.type === 'judge' && newQuestion.value.correctAnswer === null) {
    ElMessage.warning('请选择正确答案');
    return;
  }
  
  // 验证简答题/填空题答案
  if (['short', 'blank'].includes(newQuestion.value.type) && !newQuestion.value.correctAnswer?.trim()) {
    ElMessage.warning('请输入正确答案');
    return;
  }
  
  console.log("courseID", props.courseId)
  const question = {
    questionId: `q${Date.now()}`, // 生成唯一ID
    courseId: props.courseId,
    title: newQuestion.value.title,
    questionType: newQuestion.value.type,
    scorePoints: newQuestion.value.score,
    options: JSON.parse(JSON.stringify(newQuestion.value.options)), // 深拷贝选项
    correctAnswer: null, // 初始化为null
    explanation: newQuestion.value.explanation
  };
  
  // 设置正确答案
  if (['single', 'multiple'].includes(newQuestion.value.type)) {
    // 获取正确答案标签
    const answerLabels = newQuestion.value.correctAnswerLabel.split(',').map(l => l.trim());
    
    // 存储正确答案标签（而不是值）
    question.correctAnswer = newQuestion.value.type === 'single' 
      ? answerLabels[0] // 单选题：单个标签
      : answerLabels;   // 多选题：标签数组
  } else if (newQuestion.value.type === 'judge') {
    question.correctAnswer = newQuestion.value.correctAnswer;
  } else {
    question.correctAnswer = newQuestion.value.correctAnswer;
  }
  
  console.log("添加题目对象:", question);
  const backendQuestion = convertToBackendQuestion(question);
  
  console.log("转换后的后端实体:", backendQuestion);
  
  // 调用后端API添加题目
  addExamQuestion(backendQuestion);
  
  // 重置表单
  resetNewQuestionForm();
  
  // 关闭对话框
  showAddQuestionDialog.value = false;
  
  ElMessage.success('题目已添加');
}

// 添加题目API调用
async function addExamQuestion(question) {
  try {
    console.log(props.paper.examId);
    const response = await teacherService.addQuestion({
      examId: props.paper.examId, // 试卷ID
      questionBank: question
    });

    if (response.data.success) {
      ElMessage.success("题目添加成功");
      // 通知父组件添加题目
      // emit("add-question", question);
    } else {
      ElMessage.error("题目添加失败: " + response.data.message);
    }
  } catch (error) {
    ElMessage.error("请求失败: " + error.message);
  }
}

// 重置新题目表单
function resetNewQuestionForm() {
  newQuestion.value = {
    id: null,
    title: '',
    type: 'single',
    score: 5,
    options: [
      { value: 'option1', label: 'A', content: '选项内容1' },
      { value: 'option2', label: 'B', content: '选项内容2' }
    ],
    correctAnswer: null,
    correctAnswerLabel: '',
    explanation: ''
  };
}

onMounted(() =>{
  // console.log("courseId", props.courseId)
  console.log("paper", props.paper)
})
</script>

<style scoped>
.exam-attempt {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 32px 24px;
}
.exam-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 12px;
}
.exam-info {
  color: #888;
  margin-bottom: 24px;
}
.question-block {
  margin-bottom: 32px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}
.question-header {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.q-score {
  color: #409eff;
}
.q-type {
  color: #aaa;
  font-size: 13px;
}
.question-body {
  margin-bottom: 8px;
}
.wrong-analysis {
  background: #fff7e6;
  border-left: 4px solid #faad14;
  padding: 10px 16px;
  border-radius: 4px;
  margin-top: 8px;
  color: #ad6800;
}
.exam-actions {
  text-align: center;
  margin-top: 32px;
}
.readonly-tip {
  color: #888;
  text-align: center;
  margin-top: 32px;
}
.answer-compare {
  margin: 8px 0 0 0;
  color: #444;
  font-size: 15px;
  background: #f6f8fa;
  border-radius: 4px;
  padding: 8px 14px;
}
.question-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.edit-options {
  margin-top: 10px;
}
.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 10px;
}
.analysis-edit {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #eee;
}
.analysis-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #555;
}
.correct-answer-input {
  margin-top: 15px;
  display: flex;
  align-items: center;
}
</style>