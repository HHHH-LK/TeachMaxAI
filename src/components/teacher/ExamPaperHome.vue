<template>
  <div class="exam-paper-container">
    <div v-if="questions.length === 0" class="no-questions">
      <p>正在生成中</p>
    </div>
    <div v-else>
      <div
        v-for="(question, index) in questions"
        :key="index"
        class="question-card"
      >
        <div class="question-header">
          <span class="question-number">{{ index + 1 }}.</span>
          <span class="question-type"
            >[{{ getQuestionTypeLabel(question.type) }}]</span
          >
          <span class="question-text">{{ question.text }}</span>
          
          <!-- 操作按钮组 -->
          <div class="question-actions">
            <button 
              class="edit-question-btn"
              @click="editQuestion(index)"
            >
              <el-icon><Edit /></el-icon>
            </button>
            <button 
              class="delete-question-btn"
              @click="deleteQuestion(index)"
            >
              <el-icon><Delete /></el-icon>
            </button>
          </div>
        </div>

        <div class="question-body">
          <!-- 单选 -->
          <div v-if="question.type === 'single-choice'" class="options-list">
            <label
              v-for="(option, oIndex) in question.options"
              :key="oIndex"
              :class="[
                'option-item',
                submitted
                  ? getOptionClass(
                      option.value,
                      question.answer,
                      displayUserAnswers[index]
                    )
                  : '',
                { selected: displayUserAnswers[index] === option.value },
              ]"
            >
              <input
                type="radio"
                :name="'question-' + index"
                :value="option.value"
                v-model="localUserAnswers[index]"
                :disabled="isReadonly || submitted"
              />
              {{ option.label }}. {{ option.content }}
            </label>
          </div>

          <!-- 多选 -->
          <div
            v-else-if="question.type === 'multiple-choice'"
            class="options-list"
          >
            <label
              v-for="(option, oIndex) in question.options"
              :key="oIndex"
              :class="[
                'option-item',
                submitted
                  ? getOptionClass(
                      option.value,
                      question.answer,
                      displayUserAnswers[index]
                    )
                  : '',
                {
                  selected:
                    Array.isArray(displayUserAnswers[index]) &&
                    displayUserAnswers[index].includes(option.value),
                },
              ]"
            >
              <input
                type="checkbox"
                :name="'question-' + index + '-multiple'"
                :value="option.value"
                v-model="localUserAnswers[index]"
                :disabled="isReadonly || submitted"
              />
              {{ option.label }}. {{ option.content }}
            </label>
          </div>

          <!-- 填空 -->
          <div v-else-if="question.type === 'fill-in-blank'">
            <input
              type="text"
              v-model="localUserAnswers[index]"
              placeholder="请填写答案"
              class="fill-in-input"
              :disabled="isReadonly || submitted"
            />
          </div>

          <!-- 判断 -->
          <div v-else-if="question.type === 'true-false'" class="options-list">
            <label
              v-for="(option, oIndex) in question.options"
              :key="oIndex"
              :class="[
                'option-item',
                submitted
                  ? getOptionClass(
                      option.value,
                      question.answer,
                      displayUserAnswers[index]
                    )
                  : '',
                { selected: displayUserAnswers[index] === option.value },
              ]"
            >
              <input
                type="radio"
                :name="'question-tf-' + index"
                :value="option.value"
                v-model="localUserAnswers[index]"
                :disabled="isReadonly || submitted"
              />
              {{ option.label }}. {{ option.content }}
            </label>
          </div>

          <!-- 简答 -->
          <div v-else-if="question.type === 'short-answer'">
            <textarea
              v-model="localUserAnswers[index]"
              placeholder="请在此处作答"
              class="short-answer-textarea"
              :disabled="isReadonly || submitted"
            ></textarea>
          </div>
        </div>

        <!-- 答案与解析区域 -->
        <div v-if="isReadonly || submitted" class="answer-section">
          <p
            class="your-answer"
            :class="
              isCorrect(question, displayUserAnswers[index])
                ? 'correct'
                : 'wrong'
            "
          >
            你选择的答案：{{
              formatUserAnswer(displayUserAnswers[index], question.type)
            }}
            <span v-if="isCorrect(question, displayUserAnswers[index])"
              >（正确）</span
            >
            <span
              v-else-if="
                (displayUserAnswers[index] &&
                  typeof displayUserAnswers[index] === 'string' &&
                  displayUserAnswers[index].trim() !== '') ||
                (Array.isArray(displayUserAnswers[index]) &&
                  displayUserAnswers[index].length > 0)
              "
              >（错误）</span
            >
            <span v-else>（未作答）</span>
          </p>
          <p class="correct-answer">
            正确答案: {{ formatAnswer(question.answer, question.type) }}
          </p>
          <p v-if="question.explanation" class="explanation">
            解析: {{ question.explanation }}
          </p>
        </div>
      </div>

      <div class="exam-actions" v-if="!isReadonly">
        <button @click="submitExam" :disabled="submitted" class="submit-btn">
          提交试卷
        </button>
      </div>
    </div>
    
    <!-- 添加题目按钮 -->
    <div class="add-question-btn">
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>添加题目
      </el-button>
    </div>
    
    <!-- 题目编辑对话框 -->
    <el-dialog
      v-model="showEditDialog"
      :title="`编辑题目 (${getQuestionTypeLabel(editingQuestion.type)})`"
      width="600px"
    >
      <div class="edit-question-form">
        <el-form label-width="100px">
          <el-form-item label="题目类型">
            <el-select v-model="editingQuestion.type" disabled>
              <el-option label="单选题" value="single-choice"></el-option>
              <el-option label="多选题" value="multiple-choice"></el-option>
              <el-option label="填空题" value="fill-in-blank"></el-option>
              <el-option label="判断题" value="true-false"></el-option>
              <el-option label="简答题" value="short-answer"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="题目内容">
            <el-input
              type="textarea"
              v-model="editingQuestion.text"
              :rows="3"
              placeholder="请输入题目内容"
            ></el-input>
          </el-form-item>
          
          <!-- 选项编辑区域 -->
          <div v-if="editingQuestion.type === 'single-choice' || editingQuestion.type === 'multiple-choice'">
            <el-form-item label="题目选项">
              <div class="option-list">
                <div
                  v-for="(option, optIndex) in editingQuestion.options"
                  :key="optIndex"
                  class="option-item"
                >
                  <el-input
                    v-model="option.label"
                    placeholder="选项标签"
                    size="small"
                    style="width: 80px; margin-right: 10px"
                    disabled
                  />
                  <el-input
                    v-model="option.content"
                    placeholder="选项内容"
                    size="small"
                    style="width: 300px; margin-right: 10px"
                  />
                  <el-button
                    type="danger"
                    size="small"
                    circle
                    @click="removeOption(optIndex)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <el-button type="primary" size="small" @click="addOption">
                  <el-icon><Plus /></el-icon>添加选项
                </el-button>
              </div>
            </el-form-item>
          </div>
          
          <el-form-item label="正确答案">
            <!-- 单选题 -->
            <el-select 
              v-if="editingQuestion.type === 'single-choice'"
              v-model="editingQuestion.answer"
              placeholder="请选择正确答案"
            >
              <el-option
                v-for="option in editingQuestion.options"
                :key="option.value"
                :label="`${option.label}. ${option.content}`"
                :value="option.value"
              ></el-option>
            </el-select>
            
            <!-- 多选题 -->
            <el-select 
              v-else-if="editingQuestion.type === 'multiple-choice'"
              v-model="editingQuestion.answer"
              multiple
              placeholder="请选择正确答案"
            >
              <el-option
                v-for="option in editingQuestion.options"
                :key="option.value"
                :label="`${option.label}. ${option.content}`"
                :value="option.value"
              ></el-option>
            </el-select>
            
            <!-- 判断题 -->
            <el-select 
              v-else-if="editingQuestion.type === 'true-false'"
              v-model="editingQuestion.answer"
              placeholder="请选择正确答案"
            >
              <el-option label="正确" value="true"></el-option>
              <el-option label="错误" value="false"></el-option>
            </el-select>
            
            <!-- 填空题和简答题 -->
            <el-input
              v-else
              v-model="editingQuestion.answer"
              :placeholder="editingQuestion.type === 'fill-in-blank' ? '请输入填空答案' : '请输入参考答案'"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="题目解析">
            <el-input
              type="textarea"
              v-model="editingQuestion.explanation"
              :rows="3"
              placeholder="请输入题目解析"
            ></el-input>
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="saveEditedQuestion">保存</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 添加题目对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加新题目"
      width="600px"
    >
      <div class="add-question-form">
        <el-form label-width="100px">
          <el-form-item label="题目类型" prop="type">
            <el-select v-model="newQuestion.type" placeholder="请选择题目类型">
              <el-option label="单选题" value="single-choice"></el-option>
              <el-option label="多选题" value="multiple-choice"></el-option>
              <el-option label="填空题" value="fill-in-blank"></el-option>
              <el-option label="判断题" value="true-false"></el-option>
              <el-option label="简答题" value="short-answer"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="题目内容" prop="text">
            <el-input
              type="textarea"
              v-model="newQuestion.text"
              :rows="3"
              placeholder="请输入题目内容"
            ></el-input>
          </el-form-item>
          
          <!-- 选项编辑区域 -->
          <div v-if="newQuestion.type === 'single-choice' || newQuestion.type === 'multiple-choice'">
            <el-form-item label="题目选项">
              <div class="option-list">
                <div
                  v-for="(option, optIndex) in newQuestion.options"
                  :key="optIndex"
                  class="option-item"
                >
                  <el-input
                    v-model="option.label"
                    placeholder="选项标签"
                    size="small"
                    style="width: 80px; margin-right: 10px"
                    disabled
                  />
                  <el-input
                    v-model="option.content"
                    placeholder="选项内容"
                    size="small"
                    style="width: 300px; margin-right: 10px"
                  />
                  <el-button
                    type="danger"
                    size="small"
                    circle
                    @click="removeNewOption(optIndex)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <el-button type="primary" size="small" @click="addNewOption">
                  <el-icon><Plus /></el-icon>添加选项
                </el-button>
              </div>
            </el-form-item>
          </div>
          
          <el-form-item label="正确答案" prop="answer">
            <!-- 单选题 -->
            <el-select 
              v-if="newQuestion.type === 'single-choice'"
              v-model="newQuestion.answer"
              placeholder="请选择正确答案"
            >
              <el-option
                v-for="option in newQuestion.options"
                :key="option.value"
                :label="`${option.label}. ${option.content}`"
                :value="option.value"
              ></el-option>
            </el-select>
            
            <!-- 多选题 -->
            <el-select 
              v-else-if="newQuestion.type === 'multiple-choice'"
              v-model="newQuestion.answer"
              multiple
              placeholder="请选择正确答案"
            >
              <el-option
                v-for="option in newQuestion.options"
                :key="option.value"
                :label="`${option.label}. ${option.content}`"
                :value="option.value"
              ></el-option>
            </el-select>
            
            <!-- 判断题 -->
            <el-select 
              v-else-if="newQuestion.type === 'true-false'"
              v-model="newQuestion.answer"
              placeholder="请选择正确答案"
            >
              <el-option label="正确" value="true"></el-option>
              <el-option label="错误" value="false"></el-option>
            </el-select>
            
            <!-- 填空题和简答题 -->
            <el-input
              v-else
              v-model="newQuestion.answer"
              :placeholder="newQuestion.type === 'fill-in-blank' ? '请输入填空答案' : '请输入参考答案'"
            ></el-input>
          </el-form-item>
          
          <el-form-item label="题目解析" prop="explanation">
            <el-input
              type="textarea"
              v-model="newQuestion.explanation"
              :rows="3"
              placeholder="请输入题目解析"
            ></el-input>
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="addNewQuestion">添加</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { Edit, Delete, Plus } from "@element-plus/icons-vue";
import { teacherService } from "@/services/api";
import { ElLoading, ElMessage } from "element-plus";

const props = defineProps({
  examContent: {
    type: String,
    default: "",
  },
  chapterId: {
    type: String,
    default: "",
  },
  courseId: {
    type: String,
    default: "",
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  userAnswers: {
    type: Array,
    default: () => [],
  },
  questionIds: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(["delete-question", "update-question", "add-question"]);

const questions = ref([]);
const localUserAnswers = ref([]);
const submitted = ref(false);
const showEditDialog = ref(false);
const showAddDialog = ref(false);
const editingQuestionIndex = ref(-1);
const editingQuestion = ref({
  type: "",
  text: "",
  options: [],
  answer: "",
  explanation: ""
});

// 新题目表单
const newQuestion = ref({
  type: "single-choice",
  text: "",
  options: [
    { label: "A", content: "", value: "A" },
    { label: "B", content: "", value: "B" }
  ],
  answer: "",
  explanation: ""
});

const isReadonly = computed(() => props.readonly);

// 只读模式下优先用外部userAnswers
const displayUserAnswers = computed(() => {
  if (
    isReadonly.value &&
    Array.isArray(props.userAnswers) &&
    props.userAnswers.length > 0
  ) {
    return props.userAnswers;
  }
  return localUserAnswers.value;
});

// 解析考试内容
const parseExamContent = (content) => {
  if (!content) return [];
  
  const parsedQuestions = [];
  const questionBlocks = content.split("\n\n");
  
  questionBlocks.forEach((block) => {
    const lines = block.split("\n").filter(line => line.trim() !== "");
    if (lines.length < 3) return; // 跳过无效块
    
    // 解析题目行
    const questionLine = lines[0];
    const questionMatch = questionLine.match(/^(\d+)\.\s*\[(\w+)\]\s*(.*)$/);
    if (!questionMatch) return;
    
    const question = {
      type: questionMatch[2].toLowerCase().replace("_", "-"),
      text: questionMatch[3],
      options: [],
      answer: "",
      explanation: ""
    };
    
    // 解析选项
    for (let i = 1; i < lines.length; i++) {
      const line = lines[i];
      
      // 选项行 (A. xxx)
      if (/^[A-D]\.\s*.+$/.test(line)) {
        const optionMatch = line.match(/^([A-D])\.\s*(.*)$/);
        if (optionMatch) {
          question.options.push({
            label: optionMatch[1],
            content: optionMatch[2],
            value: optionMatch[1]
          });
        }
      }
      // 答案行
      else if (/^答案:\s*\{.*\}$/.test(line)) {
        const answerMatch = line.match(/^答案:\s*(\{.*\})$/);
        if (answerMatch) {
          try {
            const answerObj = JSON.parse(answerMatch[1]);
            if (answerObj.answer) {
              // 处理数组类型的答案（多选题）
              if (Array.isArray(answerObj.answer)) {
                question.answer = answerObj.answer.join(",");
              } 
              // 处理字符串类型的答案
              else if (typeof answerObj.answer === "string") {
                question.answer = answerObj.answer;
              }
              
              // 设置解析
              if (answerObj.explanation) {
                question.explanation = answerObj.explanation;
              }
            }
          } catch (e) {
            console.error("解析答案JSON失败:", e);
          }
        }
      }
      // 解析行
      else if (/^解析:\s*.+$/.test(line)) {
        const explanationMatch = line.match(/^解析:\s*(.*)$/);
        if (explanationMatch) {
          question.explanation = explanationMatch[1];
        }
      }
    }
    
    // 为多选题设置正确的选项类型
    if (question.type === "multiple-choice" && !question.options.length) {
      question.type = "multiple-choice";
    }
    
    // 为判断题添加默认选项
    if (question.type === "true-false" && !question.options.length) {
      question.options = [
        { label: "A", content: "正确", value: "true" },
        { label: "B", content: "错误", value: "false" }
      ];
    }
    
    parsedQuestions.push(question);
  });
  
  // 初始化用户答案数组
  localUserAnswers.value = parsedQuestions.map(q => 
    q.type === "multiple-choice" ? [] : ""
  );
  
  return parsedQuestions;
};

// 获取题目类型中文标签
const getQuestionTypeLabel = (type) => {
  const typeLabels = {
    "single-choice": "单选题",
    "multiple-choice": "多选题",
    "fill-in-blank": "填空题",
    "true-false": "判断题",
    "short-answer": "简答题",
  };
  return typeLabels[type] || type;
};

// 获取选项样式
const getOptionClass = (optionValue, correctAnswer, userAnswer) => {
  // 检查学生是否作答：undefined、null、空字符串、空数组都视为未作答
  if (
    userAnswer === undefined ||
    userAnswer === null ||
    (typeof userAnswer === "string" && userAnswer.trim() === "") ||
    (Array.isArray(userAnswer) && userAnswer.length === 0)
  ) {
    return "";
  }

  // 判断题和单选题直接比较
  if (typeof userAnswer === "string") {
    if (optionValue === correctAnswer) return "correct-option";
    if (optionValue === userAnswer && userAnswer !== correctAnswer)
      return "wrong-option";
  }
  // 多选题：多个答案
  else if (Array.isArray(userAnswer)) {
    if (correctAnswer.includes(optionValue)) {
      return userAnswer.includes(optionValue)
        ? "correct-option"
        : "missing-option";
    } else {
      return userAnswer.includes(optionValue) ? "wrong-option" : "";
    }
  }
  return "";
};

// 检查答案是否正确
const isCorrect = (question, userAnswer) => {
  // 检查学生是否作答：undefined、null、空字符串、空数组都视为未作答
  if (
    userAnswer === undefined ||
    userAnswer === null ||
    (typeof userAnswer === "string" && userAnswer.trim() === "") ||
    (Array.isArray(userAnswer) && userAnswer.length === 0)
  ) {
    return false;
  }

  const correctAnswer = question.answer;
  switch (question.type) {
    case "single-choice":
    case "fill-in-blank":
    case "true-false":
      return userAnswer === correctAnswer;

    case "multiple-choice":
      // 多选题：确保每个选项都正确（假设正确答案是逗号分隔的字符串，如"A,B"）
      const correctArray = correctAnswer.split(",").map((a) => a.trim());
      return Array.isArray(userAnswer)
        ? userAnswer.sort().toString() === correctArray.sort().toString()
        : false;

    case "short-answer":
      // 简答题暂时不判对错
      return false;

    default:
      return false;
  }
};

// 格式化用户答案
const formatUserAnswer = (userAnswer, type) => {
  // 检查学生是否作答：undefined、null、空字符串、空数组都视为未作答
  if (
    userAnswer === undefined ||
    userAnswer === null ||
    (typeof userAnswer === "string" && userAnswer.trim() === "") ||
    (Array.isArray(userAnswer) && userAnswer.length === 0)
  ) {
    return "未作答";
  }

  if (type === "multiple-choice" && Array.isArray(userAnswer)) {
    return userAnswer.join(", ");
  }

  if (type === "true-false") {
    if (userAnswer === "true") return "正确";
    else if (userAnswer === "false") return "错误";
    else return "未作答";
  }

  return userAnswer;
};

// 格式化正确答案
const formatAnswer = (answer, type) => {
  if (!answer) return "";
  
  // 确保 answer 是字符串类型
  const answerStr = String(answer);
  
  if (type === "multiple-choice") {
    return answerStr
      .split(",")
      .map(a => a.trim())
      .join(", ");
  }

  if (type === "true-false") {
    return answerStr === "true" ? "正确" : "错误";
  }
  
  return answerStr;
};


const editQuestion = (index) => {
  // 设置当前编辑的题目索引
  editingQuestionIndex.value = index;
  
  // 复制当前题目数据到编辑对象
  const question = questions.value[index];
  
  // 处理多选题的答案（如果是字符串，转换为数组）
  let answer = question.answer;
  if (question.type === "multiple-choice" && typeof answer === "string") {
    answer = answer.split(",").map(a => a.trim());
  }
  
  editingQuestion.value = {
    type: question.type,
    text: question.text,
    options: [...question.options],
    answer: answer,
    explanation: question.explanation
  };
  
  // 打开编辑对话框
  showEditDialog.value = true;
};

// 添加选项
const addOption = () => {
  const newOption = {
    label: String.fromCharCode(65 + editingQuestion.value.options.length),
    content: "",
    value: String.fromCharCode(65 + editingQuestion.value.options.length)
  };
  editingQuestion.value.options.push(newOption);
};

// 移除选项
const removeOption = (index) => {
  editingQuestion.value.options.splice(index, 1);
};

// 保存编辑后的题目
const saveEditedQuestion = async () => {
  // 获取当前编辑的题目索引
  const index = editingQuestionIndex.value;
  
  // 处理多选题答案（如果是数组，转换为逗号分隔的字符串）
  let finalAnswer = editingQuestion.value.answer;
  if (editingQuestion.value.type === "multiple-choice" && Array.isArray(finalAnswer)) {
    finalAnswer = finalAnswer.join(",");
  }
  
  // 获取题目ID（从父组件传递的questionIds中获取）
  const questionId = props.questionIds[index];
  
  // 验证是否获取到有效的题目ID
  if (!questionId) {
    ElMessage.error("无法更新题目：缺少题目ID");
    return;
  }
  
  // 显示加载状态
  const loadingInstance = ElLoading.service({
    lock: true,
    text: "正在更新题目...",
    background: "rgba(0, 0, 0, 0.7)",
  });
  
  try {
    // 构建API参数
    const questionData = {
      questionId: questionId,
      courseId: props.courseId ? parseInt(props.courseId) : 0,
      pointId: 0, // 知识点ID，需要根据实际情况设置
      questionType: mapQuestionType(editingQuestion.value.type), // 使用映射函数转换类型
      questionContent: editingQuestion.value.text,
      questionOptions: JSON.stringify(editingQuestion.value.options),
      correctAnswer: finalAnswer,
      explanation: editingQuestion.value.explanation,
      difficultyLevel: "medium", // 难度等级，需要根据实际情况设置
      scorePoints: 5, // 题目分值，需要根据实际情况设置
      createdBy: 1 // 创建者ID，需要根据实际情况设置
    };
    
    // 调用API更新题目
    const response = await teacherService.updateQuestion(questionData);
    
    if (response.data && response.data.success) {
      // 更新本地题目数据
      questions.value[index] = {
        ...questions.value[index],
        text: editingQuestion.value.text,
        options: [...editingQuestion.value.options],
        answer: finalAnswer,
        explanation: editingQuestion.value.explanation
      };
      
      // 发出更新事件，通知父组件
      emit("update-question", index, questions.value[index]);
      
      // 关闭对话框
      showEditDialog.value = false;
      
      ElMessage.success("题目更新成功");
    } else {
      ElMessage.error(response.data?.message || "更新题目失败");
    }
  } catch (error) {
    console.error("更新题目失败:", error);
    ElMessage.error(`更新题目失败: ${error.message || "请检查网络连接"}`);
  } finally {
    // 关闭加载状态
    loadingInstance.close();
  }
};

// 添加新选项
const addNewOption = () => {
  const newLabel = String.fromCharCode(65 + newQuestion.value.options.length);
  const newOption = {
    label: newLabel,
    content: "",
    value: newLabel
  };
  newQuestion.value.options.push(newOption);
};

// 移除新选项
const removeNewOption = (index) => {
  newQuestion.value.options.splice(index, 1);
};

// 题目类型映射函数
const mapQuestionType = (frontendType) => {
  const typeMap = {
    "single-choice": "single_choice",
    "multiple-choice": "multiple_choice",
    "fill-in-blank": "fill_blank",
    "true-false": "true_false",
    "short-answer": "short_answer"
  };
  
  return typeMap[frontendType] || frontendType;
};

const addNewQuestion = async () => {
  // 验证题目内容
  if (!newQuestion.value.text.trim()) {
    ElMessage.warning("请输入题目内容");
    return;
  }
  
  // 验证选择题选项
  if (["single-choice", "multiple-choice"].includes(newQuestion.value.type)) {
    // 检查是否有选项内容为空
    const emptyOption = newQuestion.value.options.find(
      opt => !opt.content.trim()
    );
    
    if (emptyOption) {
      ElMessage.warning("请填写所有选项内容");
      return;
    }
    
    // 验证答案是否为空
    if (!newQuestion.value.answer || 
        (Array.isArray(newQuestion.value.answer) && newQuestion.value.answer.length === 0)) {
      ElMessage.warning("请选择正确答案");
      return;
    }
  }
  
  // 验证判断题答案
  if (newQuestion.value.type === "true-false" && !newQuestion.value.answer) {
    ElMessage.warning("请选择正确答案");
    return;
  }
  
  // 验证填空题/简答题答案
  if (["fill-in-blank", "short-answer"].includes(newQuestion.value.type) && 
      !newQuestion.value.answer.trim()) {
    ElMessage.warning("请输入正确答案");
    return;
  }
  
  // 处理多选题答案（如果是数组，转换为逗号分隔的字符串）
  let finalAnswer = newQuestion.value.answer;
  if (newQuestion.value.type === "multiple-choice" && Array.isArray(finalAnswer)) {
    finalAnswer = finalAnswer.join(",");
  }
  
  // 创建新题目对象
  const newQuestionObj = {
    type: newQuestion.value.type,
    text: newQuestion.value.text,
    options: [...newQuestion.value.options],
    answer: finalAnswer,
    explanation: newQuestion.value.explanation
  };
  
  // 显示加载状态
  const loadingInstance = ElLoading.service({
    lock: true,
    text: "正在保存题目...",
    background: "rgba(0, 0, 0, 0.7)",
  });
  
  try {
    // 构建API参数
    const questionBank = {
      courseId: props.courseId ? parseInt(props.courseId) : 0,
      pointId: 0, // 知识点ID，
      questionType: mapQuestionType(newQuestion.value.type), 
      questionContent: newQuestion.value.text,
      questionOptions: JSON.stringify(newQuestion.value.options),
      correctAnswer: finalAnswer,
      explanation: newQuestion.value.explanation,
      difficultyLevel: "medium", // 难度等级，
      scorePoints: 5, // 题目分值，
      createdBy: 1 // 创建者ID，
    };
    
    const params = {
      questionBank: questionBank,
      chapterId: props.chapterId ? parseInt(props.chapterId) : 0
    };
    
    // 调用API添加题目
    const response = await teacherService.addChapterQuestion(params);
    
    if (response.data && response.data.success) {
      
      // 添加到题目列表
      questions.value.push(newQuestionObj);
      
      // 初始化用户答案
      localUserAnswers.value.push(
        newQuestion.value.type === "multiple-choice" ? [] : ""
      );

      // 关闭对话框
      showAddDialog.value = false;
      
      // 重置表单
      resetNewQuestionForm();
      
      ElMessage.success("题目添加成功");
    } else {
      ElMessage.error(response.data?.message || "添加题目失败");
    }
  } catch (error) {
    console.error("添加题目失败:", error);
    ElMessage.error(`添加题目失败: ${error.message || "请检查网络连接"}`);
  } finally {
    // 关闭加载状态
    loadingInstance.close();
  }
};

// 重置新题目表单
const resetNewQuestionForm = () => {
  newQuestion.value = {
    type: "single-choice",
    text: "",
    options: [
      { label: "A", content: "", value: "A" },
      { label: "B", content: "", value: "B" }
    ],
    answer: "",
    explanation: ""
  };
};

const deleteQuestion = async (index) => {
  // 确认删除
  if (confirm("确定要删除这道题目吗？")) {
    // 获取要删除的题目ID
    const questionId = props.questionIds[index];
    
    // 获取章节ID
    const chapterId = props.chapterId;
    
    // 检查是否有有效的题目ID和章节ID
    if (!questionId || !chapterId) {
      ElMessage.error("无法删除题目：缺少必要的题目ID或章节ID");
      return;
    }
    
    // 显示加载状态
    const loadingInstance = ElLoading.service({
      lock: true,
      text: "正在删除题目...",
      background: "rgba(0, 0, 0, 0.7)",
    });
    
    try {
      // 调用API删除题目，使用POST请求并将参数放在请求体中
      const response = await teacherService.deleteChapter({
        chapterId: chapterId,
        questionId: questionId
      });
      
      // 检查API响应
      if (response.data && response.data.success) {
        // 从本地问题列表中移除
        questions.value.splice(index, 1);
        
        // 从用户答案列表中移除
        localUserAnswers.value.splice(index, 1);
        
        // 从题目ID列表中移除
        const updatedQuestionIds = [...props.questionIds];
        updatedQuestionIds.splice(index, 1);
        
        // 发出删除事件，通知父组件
        emit("delete-question", index, questionId, updatedQuestionIds);
        
        ElMessage.success("题目删除成功");
      } else {
        ElMessage.error(response.data?.message || "删除题目失败");
      }
    } catch (error) {
      console.error("删除题目失败:", error);
      ElMessage.error(`删除题目失败: ${error.message || "请检查网络连接"}`);
    } finally {
      // 关闭加载状态
      loadingInstance.close();
    }
  }
};

// 提交试卷
const submitExam = () => {
  submitted.value = true;
  // 这里可以添加提交试卷的逻辑
};

// 监听examContent变化以解析题目
watch(
  () => props.examContent,
  (newContent) => {
    if (newContent) {
      const parsed = parseExamContent(newContent);
      questions.value = parsed;
    } else {
      questions.value = [];
    }
  },
  { immediate: true }
);

onMounted(() => {
  console.log("Component mounted", props.examContent);
  console.log("ids",props.questionIds)
  console.log("chapter",props.chapterId)
})
</script>

<style scoped>
.exam-paper-container {
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.no-questions {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 16px;
}

.question-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 25px;
  position: relative;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding-right: 100px;
  position: relative;
}

.question-number {
  font-weight: bold;
  margin-right: 8px;
  font-size: 16px;
}

.question-type {
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 10px;
}

.question-text {
  font-size: 16px;
  line-height: 1.5;
}

/* 操作按钮组样式 */
.question-actions {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  gap: 8px;
}

.edit-question-btn, .delete-question-btn {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.edit-question-btn {
  background: #409eff;
  color: white;
  border: none;
}

.edit-question-btn:hover {
  background: #79bbff;
  transform: scale(1.05);
}

.delete-question-btn {
  background: #ff4d4f;
  color: white;
  border: none;
}

.delete-question-btn:hover {
  background: #ff7875;
  transform: scale(1.05);
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f9f9f9;
  transition: all 0.2s;
}

.option-item:hover {
  background: #f0f0f0;
}

.option-item.selected {
  background: #e6f7ff;
  border-left: 3px solid #1890ff;
}

.option-item input {
  margin-right: 10px;
}

.fill-in-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.short-answer-textarea {
  width: 100%;
  min-height: 100px;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
}

.answer-section {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px dashed #eee;
}

.your-answer {
  font-weight: 500;
  margin-bottom: 8px;
}

.your-answer.correct {
  color: #52c41a;
}

.your-answer.wrong {
  color: #ff4d4f;
}

.correct-answer {
  color: #1890ff;
  margin-bottom: 8px;
}

.explanation {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.exam-actions {
  margin-top: 30px;
  text-align: center;
}

.submit-btn {
  background: #1890ff;
  color: white;
  border: none;
  padding: 10px 25px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:hover {
  background: #40a9ff;
}

.submit-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

/* 添加题目按钮样式 */
.add-question-btn {
  margin-top: 20px;
  margin-bottom: 30px;
  text-align: center;
}

/* 编辑表单样式 */
.edit-question-form {
  padding: 15px;
}

.add-question-form {
  padding: 15px;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

/* 正确/错误选项样式 */
.correct-option {
  background-color: #f6ffed;
  border-left: 3px solid #52c41a;
}

.wrong-option {
  background-color: #fff1f0;
  border-left: 3px solid #ff4d4f;
}

.missing-option {
  background-color: #fffbe6;
  border-left: 3px solid #faad14;
}
</style>