<template>
  <div class="error-collection-container">
    <div class="header">
      <h2>错题集</h2>
      <button class="back-button" @click="$emit('back')">
        <i class="icon-back"></i> 返回
      </button>
    </div>
    
    <div class="filter-section">
      <!-- <div class="filter-group">
        <label>考试分类：</label>
        <select v-model="selectedExamCategory">
          <option value="all">全部</option>
          <option value="ai_generated">AI生成</option>
          <option value="homework">作业</option>
          <option value="exam">考试</option>
        </select>
      </div> -->

      <div class="filter-group">
        <label>错题类型：</label>
        <select v-model="selectedErrorType">
          <option value="all">全部</option>
          <option value="single_choice">单选</option>
          <option value="multiple_choice">多选</option>
          <option value="true_false">判断</option>
          <option value="fill_in_blank">填空</option>
          <option value="short_answer">简答</option>
        </select>
      </div>
      
      <div class="filter-group">
        <label>章节：</label>
        <select v-model="selectedChapter">
          <option value="all">全部章节</option>
          <option v-for="chapter in chapters" :value="chapter.id">{{ chapter.name }}</option>
        </select>
      </div>
    </div>
    
    <div class="error-list">
      <div v-for="(error, index) in filteredErrors" :key="index" class="error-item">
        <div class="error-header">
          <span class="error-type" :class="error.type">{{ getErrorTypeName(error.type) }}</span>
          <!-- <span class="exam-category" v-if="error.examCategory">{{ getExamCategoryName(error.examCategory) }}</span> -->
          <!-- <span class="error-chapter">{{ getChapterName(error.chapterId) }}</span> -->
        </div>
        
        <div class="question-content">
          <h3>题目：</h3>
          <p>{{ error.question }}</p>
        </div>
        
        <div class="answer-section">
          <div class="user-answer">
            <h4>你的答案：</h4>
            <p>{{ error.userAnswer }}</p>
          </div>
          <div class="correct-answer">
            <h4>正确答案：</h4>
            <p>{{ error.correctAnswer }}</p>
          </div>
        </div>
        
        <div class="explanation">
          <h4>解析：</h4>
          <p>{{ error.explanation }}</p>
        </div>
        
        <div class="knowledge-point">
          <h4>相关知识点：</h4>
          <p>{{ error.knowledgePoint }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, defineProps } from 'vue';
import { studentService } from "@/services/api";

const props = defineProps({
  courseId: {
    type: Number,
    required: true,
  },
});

const loading = ref(true);
const error = ref(null);

// 模拟数据 - 实际应用中应从后端获取
const errors = ref([
  {
    id: 1,
    type: 'single_choice',
    question: '以下哪个选项是Java语言的特点？',
    userAnswer: 'C++兼容性',
    correctAnswer: '跨平台性',
    explanation: 'Java语言的一个主要特点是其跨平台性，通过JVM实现。',
    knowledgePoint: 'Java语言基础',
    chapterId: 1,
    date: '2023-05-10',
    examCategory: 'ai_generated'
  },
  {
    id: 2,
    type: 'multiple_choice',
    question: '以下哪些是关系型数据库？',
    userAnswer: 'MongoDB, Redis',
    correctAnswer: 'MySQL, PostgreSQL, Oracle',
    explanation: 'MySQL, PostgreSQL, Oracle是常见的关系型数据库，MongoDB和Redis是非关系型数据库。',
    knowledgePoint: '数据库基础',
    chapterId: 2,
    date: '2023-05-15',
    examCategory: 'homework'
  },
  {
    id: 3,
    type: 'true_false',
    question: '所有编程语言都支持面向对象编程。',
    userAnswer: '对',
    correctAnswer: '错',
    explanation: '并非所有编程语言都支持面向对象编程，例如C语言主要支持面向过程编程。',
    knowledgePoint: '编程范式',
    chapterId: 3,
    date: '2023-05-20',
    examCategory: 'exam'
  },
  {
    id: 4,
    type: 'fill_in_blank',
    question: 'HTTP协议的默认端口是____。',
    userAnswer: '8080',
    correctAnswer: '80',
    explanation: 'HTTP协议的默认端口是80。',
    knowledgePoint: '网络协议',
    chapterId: 1,
    date: '2023-05-22',
    examCategory: 'ai_generated'
  },
  {
    id: 5,
    type: 'short_answer',
    question: '请简述TCP和UDP的区别。',
    userAnswer: 'TCP快，UDP慢。',
    correctAnswer: 'TCP是面向连接的、可靠的、基于字节流的传输层协议；UDP是无连接的、不可靠的、基于数据报的传输层协议。',
    explanation: 'TCP提供可靠的数据传输服务，有拥塞控制和流量控制；UDP则提供简单的、不可靠的数据报服务，开销小，速度快。',
    knowledgePoint: '网络协议',
    chapterId: 2,
    date: '2023-05-25',
    examCategory: 'homework'
  }
]);

const chapters = ref([
]);

const selectedErrorType = ref('all');
// const selectedChapter = ref('all');
// const selectedExamCategory = ref('all');

const fetchAllError = async() =>{
  console.log(props.courseId)
  try{
    loading.value = true;
    error.value = null;
    const response = await studentService.getAllErrors(props.courseId);
    if(response.data){
      errors.value = response.data.data.map(item => ({
        id: item.answerId,
        type: item.questionType,
        question: item.questionContent,
        userAnswer: item.studentAnswer,
        correctAnswer: item.correctAnswer,
        explanation: item.explanation,
        knowledgePoint: item.pointName,
        chapterId: item.chapterId,
        // date: 
      }))
    }
  
  }catch(e){
    console.log("fetch error", e);
  }
} 


//查询筛选
const filteredErrors = computed(() => {
  return errors.value.filter(error => {
    const typeMatch = selectedErrorType.value === 'all' || error.type === selectedErrorType.value;
    // const chapterMatch = selectedChapter.value === 'all' || error.chapterId === selectedChapter.value;
    // const examCategoryMatch = selectedExamCategory.value === 'all' || error.examCategory === selectedExamCategory.value;
    return typeMatch ;
  });
});

//获取错误题目类型
const getErrorTypeName = (type) => {
  const typeNames = {
    single_choice: '单选',
    multiple_choice: '多选',
    true_false: '判断',
    fill_blank: '填空',
    short_answer: '简答'
  };
  return typeNames[type] || type;
};

// //获取章节名
// const getChapterName = (id) => {
//   const chapter = chapters.value.find(c => c.id === id);
//   return chapter ? chapter.name : '未知章节';
// };

//获取题目来源（ai/测试/考试）
// const getExamCategoryName = (category) => {
//   const categoryNames = {
//     ai_generated: 'AI生成',
//     homework: '作业',
//     exam: '考试'
//   };
//   return categoryNames[category] || category;
// };

watch(
  () => props.courseId,
  (newCourseId) => {
    if (newCourseId) {
      fetchAllError();
    }
  },
  { immediate: true }
); // 立即执行一次
</script>

<style lang="less" scoped>
.error-collection-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.filter-section {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;

  label {
    font-weight: 500;
  }

  select {
    padding: 8px 12px;
    border: 1px solid #ddd;
    border-radius: 4px;
    background-color: white;
  }
}

.error-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.error-item {
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 6px;
  background-color: #fff;
  transition: all 0.2s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
}

.error-header {
  display: flex;
  align-items: center;
  gap: 10px; /* Add some space between tags */
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}

.error-type {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  color: white;

  &.single_choice { background-color: #ef4444; } /* 单选 */
  &.multiple_choice { background-color: #f59e0b; } /* 多选 */
  &.true_false { background-color: #10b981; } /* 判断 */
  &.fill_in_blank { background-color: #3b82f6; } /* 填空 */
  &.short_answer { background-color: #8b5cf6; } /* 简答 */
}

// .exam-category {
//   padding: 4px 8px;
//   border-radius: 4px;
//   font-size: 12px;
//   font-weight: bold;
//   color: white;
//   background-color: #6b7280; /* 灰色背景，用于考试分类 */
// }

.error-chapter {
  color: #64748b;
  font-size: 14px;
}

.question-content {
  margin-bottom: 15px;

  h3 {
    margin-bottom: 8px;
    font-size: 16px;
    color: #1e293b;
  }

  p {
    color: #334155;
    line-height: 1.6;
  }
}

.answer-section {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;

  > div {
    flex: 1;
    padding: 12px;
    border-radius: 6px;
  }
}

.user-answer {
  background-color: #fee2e2;

  h4 {
    color: #b91c1c;
  }

  p {
    color: #7f1d1d;
  }
}

.correct-answer {
  background-color: #dcfce7;

  h4 {
    color: #166534;
  }

  p {
    color: #14532d;
  }
}

.explanation, .knowledge-point {
  margin-bottom: 15px;

  h4 {
    margin-bottom: 8px;
    color: #1e293b;
  }

  p {
    color: #334155;
    line-height: 1.6;
  }
}

.back-button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: #f8f9fa;
  }
}
</style>