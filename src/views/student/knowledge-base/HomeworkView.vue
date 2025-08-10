<template>
  <div class="homework-view">
    <h2>课程作业内容</h2>
    <div v-if="doingHomework">
      <button
          class="back-button"
          @click="backToList"
          style="margin-bottom: 18px"
      >
        ← 返回作业列表
      </button>
      <h3>{{ currentHomeworkTitle }}</h3>
      <ExamPaper :examContent="currentHomeworkContent" />
    </div>
    <div v-else>
      <div class="homework-list">
        <div
            class="homework-item"
            v-for="homework in homeworks"
            :key="homework.id"
        >
          <h3>{{ homework.title }}</h3>
<!--          <p>题目数量: {{ homework.questions.length }}</p>-->
          <button class="start-button" @click.stop="startHomework(homework)">
            开始做题
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, watch } from "vue";
import ExamPaper from "@/components/ExamPaper.vue";
import { studentService } from "@/services/api";

const props = defineProps({
  courseId: {
    type: Number,
    required: true,
  },
});

const homeworks = ref([]);

const selectedHomework = ref(null);
const userAnswers = ref({});
const doingHomework = ref(false);
const currentHomeworkContent = ref("");
const currentHomeworkTitle = ref("");

//获取作业列表
const fetchAllWork = async () => {
  // console.log("coures", props.courseId)
  try {
    const chapter = ref([]);
    const responseChapter = await studentService.getChapterInfo(props.courseId);
    console.log("chapter", responseChapter.data);
    if (responseChapter.data) {
      chapter.value = responseChapter.data.data.map((item) => ({
        id: item.chapterId,
      }));
    }
    let number = 1;
    for (var i = 0; i < chapter.value.length; i++) {
      const response = await studentService.getAllWork(
          chapter.value[i].id,
          props.courseId
      );
      console.log("response", response.data);
      if (response.data.data) {
        const homework = {
          id: `${props.courseId}-${chapter.value[i].id}-${number}`,
          title: `第${number}次作业`,
          chapterId: chapter.value[i].id, // 添加章节关联ID
          questions: [],
        };
        // console.log("homework", response.data)
        homework.questions = response.data.data.map((item) => {
          let parsedOptions = [];
          try {
            parsedOptions = JSON.parse(item.questionOptions);
          } catch (e) {
            console.warn("选项解析失败:", item.questionOptions);
          }

          return {
            id: item.questionId,
            type: item.questionType, // like "single_choice"
            text: item.questionContent,
            options: parsedOptions, // 正确结构
            answer: item.correctAnswer.replace(/"/g, ""), // 去掉引号
          };
        });

        // console.log("homework", homework.value);

        if (homework.questions.length) {
          number++;
          homeworks.value.push(homework);
        }
      }
    }
    console.log("onw", homeworks.value);
  } catch (e) {
    console.log("error", e);
  }
};

function toExamPaperContent(questions) {
  const typeMap = {
    single_choice: "single-choice",
    multiple_choice: "multiple-choice",
    fill_blank: "fill-in-blank",
    short_answer: "short-answer",
    true_false: "true-false",
  };

  return questions
      .map((q, idx) => {
        const typeTag = typeMap[q.type] || "unknown-type";

        // options: [{ label: "A", content: "xxx" }]
        const options = Array.isArray(q.options) ? q.options : [];

        const opts = options
            .map((opt) => `${opt.label}. ${opt.content}`)
            .join("\n");

        // 处理答案
        let answerText = "";
        if (q.type === "multiple_choice") {
          // 多选答案为字符串数组或逗号分隔的字符串
          if (Array.isArray(q.answer)) {
            answerText = q.answer.join(",");
          } else {
            answerText = q.answer;
          }
        } else {
          answerText = q.answer;
        }

        return `${idx + 1}. [${typeTag}] ${
            q.text
        }\n${opts}\n答案: ${answerText}`;
      })
      .join("\n\n");
}
//选择作业项
const selectHomework = (homework) => {
  selectedHomework.value = homework;
  userAnswers.value = {}; // Reset answers when selecting new homework
};

//开始作业
const startHomework = (homework) => {
  currentHomeworkContent.value = toExamPaperContent(homework.questions);
  currentHomeworkTitle.value = homework.title;
  doingHomework.value = true;
};

//回到列表
const backToList = () => {
  doingHomework.value = false;
};

//提交作业
const submitHomework = () => {
  let correctCount = 0;
  selectedHomework.value.questions.forEach((question, index) => {
    if (userAnswers.value[index] === question.answer) {
      correctCount++;
    }
  });
  alert(
      `您答对了 ${correctCount} 题，共 ${selectedHomework.value.questions.length} 题。`
  );
  // 这里可以添加提交答案到后端的逻辑
};

watch(
    () => props.courseId,
    (newCourseId) => {
      if (newCourseId) {
        fetchAllWork();
      }
    },
    { immediate: true }
);
</script>

<style scoped>
.homework-view {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.homework-list {
  display: grid;
  gap: 20px;
}

.homework-item {
  background-color: #f8fafc; /* @bg-light */
  border: 1px solid #e2e8f0; /* @border-light */
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;

  &:hover {
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1); /* @shadow-sm */
    transform: translateY(-2px);
  }

  h3 {
    margin: 0 0 5px 0;
    font-size: 18px;
    color: #1e293b; /* @text-primary */
  }

  p {
    margin: 0;
    font-size: 14px;
    color: #64748b; /* @text-secondary */
  }
}

.start-button {
  background-color: #2563eb; /* @primary */
  color: white;
  padding: 8px 15px;
  border-radius: 5px;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s ease;

  &:hover {
    background-color: #1d4ed8; /* @primary-dark */
  }
}

.homework-detail {
  h3 {
    font-size: 24px;
    color: #1e293b;
    margin-bottom: 20px;
  }
}

.question-item {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;

  .question-text {
    font-size: 16px;
    color: #1e293b;
    margin-bottom: 10px;
  }

  .options label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    color: #64748b;
    cursor: pointer;

    input[type="radio"] {
      margin-right: 8px;
    }
  }
}

.submit-button,
.back-button {
  background-color: #2563eb;
  color: white;
  padding: 10px 20px;
  border-radius: 5px;
  border: none;
  cursor: pointer;
  transition: background-color 0.3s ease;
  margin-top: 20px;
  margin-right: 10px;

  &:hover {
    background-color: #1d4ed8;
  }
}

.back-button {
  background-color: #64748b; /* Secondary color for back button */

  &:hover {
    background-color: #475569;
  }
}
</style>
