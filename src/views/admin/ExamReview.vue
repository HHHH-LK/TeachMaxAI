<template>
  <div class="exam-review-bg">
    <div class="banner">
      <div class="banner-content">
        <h1>试卷审核与发布</h1>
        <p>高效管理试卷审核流程，保障考试质量与时效</p>
      </div>
    </div>
    <div class="exam-review-card">
      <div class="header-bar">
        <el-input v-model="search" placeholder="搜索试卷名称/创建人" clearable class="search-input" @input="filterExams" />
      </div>
      <el-table :data="pagedExams" stripe border class="custom-table" :header-cell-style="headerStyle">
        <el-table-column prop="id" label="试卷ID" width="100" align="center" />
        <el-table-column prop="name" label="试卷名称" min-width="180" align="center" />
        <el-table-column prop="creator" label="创建人" width="120" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center" />
        <el-table-column prop="expectedPublish" label="预计发布时间" width="200" align="center">
          <template #default="scope">
            <span class="publish-date">{{ scope.row.expectedPublish || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <!-- 新增：查看试卷操作列 -->
        <el-table-column label="查看试卷" width="120" align="center">
          <template #default="scope">
            <el-button size="small" @click="openExamView(scope.row)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="scope">
            <el-button v-if="scope.row.status === '待审核'" type="primary" size="small" @click="confirmReview(scope.row)">审核通过</el-button>
            <el-button v-if="scope.row.status === '已审核'" type="success" size="small" @click="confirmPublish(scope.row)">发布</el-button>
            <el-tag v-if="scope.row.status === '已发布'" type="success">已发布</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          background
          layout="prev, pager, next, jumper"
          :total="filteredExams.length"
          :page-size="pageSize"
          v-model:current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px" center>
        <span>{{ dialogMessage }}</span>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleDialogConfirm">确定</el-button>
        </template>
      </el-dialog>
      <!-- 新增：试卷查看弹窗 -->
      <el-dialog v-model="examViewVisible" title="试卷预览" width="900px" center :close-on-click-modal="false">
        <ExamAttempt v-if="currentExamPaper" :paper="currentExamPaper" :readonly="true" />
        <template #footer>
          <el-button @click="examViewVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import ExamAttempt from '@/components/ExamAttempt.vue';

const exams = ref([
  { id: 1, name: '数学期中试卷', creator: '张发在', createdAt: '2024-04-01 10:00', expectedPublish: '2024-05-01 09:00', status: '待审核' },
  { id: 2, name: '英语月考试卷', creator: '李京东', createdAt: '2024-04-05 14:30', expectedPublish: '2024-05-03 14:00', status: '已审核' },
  { id: 3, name: '物理模拟卷', creator: '王暖是', createdAt: '2024-03-28 09:20', expectedPublish: '2024-04-20 09:00', status: '已发布' },
  { id: 4, name: '化学期末卷', creator: '赵星河', createdAt: '2024-04-10 16:10', expectedPublish: '2024-05-10 10:00', status: '待审核' },
  { id: 5, name: '生物竞赛卷', creator: '钱三幅', createdAt: '2024-04-12 11:45', expectedPublish: '2024-05-12 08:30', status: '已审核' },
  { id: 6, name: 'Java期末考试', creator: '张冬梅', createdAt: '2025-07-12 11:45', expectedPublish: '2025-07-12 09:00', status: '已发布' },
  { id: 6, name: 'Java期末考试（A）', creator: '张冬梅', createdAt: '2025-07-14 11:45', expectedPublish: '2025-07-14 09:00', status: '待审核' },
]);

const search = ref('');
const filteredExams = ref([...exams.value]);

function filterExams() {
  const keyword = search.value.trim();
  if (!keyword) {
    filteredExams.value = [...exams.value];
  } else {
    filteredExams.value = exams.value.filter(
      exam => exam.name.includes(keyword) || exam.creator.includes(keyword)
    );
  }
  currentPage.value = 1;
}

const pageSize = 8;
const currentPage = ref(1);
const pagedExams = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredExams.value.slice(start, start + pageSize);
});
function handlePageChange(page) {
  currentPage.value = page;
}

function statusTagType(status) {
  switch (status) {
    case '待审核': return 'warning';
    case '已审核': return 'info';
    case '已发布': return 'success';
    default: return '';
  }
}

const dialogVisible = ref(false);
const dialogTitle = ref('');
dialogTitle.value = '';
const dialogMessage = ref('');
let dialogAction = null;
let dialogRow = null;

function confirmReview(row) {
  dialogTitle.value = '审核确认';
  dialogMessage.value = `确定要通过【${row.name}】的审核吗？`;
  dialogVisible.value = true;
  dialogAction = 'review';
  dialogRow = row;
}
function confirmPublish(row) {
  dialogTitle.value = '发布确认';
  dialogMessage.value = `确定要发布【${row.name}】吗？`;
  dialogVisible.value = true;
  dialogAction = 'publish';
  dialogRow = row;
}
function handleDialogConfirm() {
  if (dialogAction === 'review') {
    dialogRow.status = '已审核';
    ElMessage.success('审核通过！');
  } else if (dialogAction === 'publish') {
    dialogRow.status = '已发布';
    ElMessage.success('发布成功！');
  }
  dialogVisible.value = false;
  filterExams();
}

const examPapers = {
  1: {
    title: '数学期中试卷',
    questions: [
      {
        id: 'q1',
        title: '下列哪个是质数？',
        type: 'single',
        options: [
          { label: 'A', value: 'A' },
          { label: 'B', value: 'B' },
          { label: 'C', value: 'C' },
          { label: 'D', value: 'D' }
        ],
        score: 5
      },
      {
        id: 'q2',
        title: '选择所有偶数',
        type: 'multiple',
        options: [
          { label: 'A', value: 'A' },
          { label: 'B', value: 'B' },
          { label: 'C', value: 'C' },
          { label: 'D', value: 'D' }
        ],
        score: 6
      },
      {
        id: 'q3',
        title: '1+1=2，对吗？',
        type: 'judge',
        score: 2
      },
      {
        id: 'q4',
        title: '请简述牛顿第一定律。',
        type: 'short',
        score: 10
      }
    ]
  },
  2: {
    title: '英语月考试卷',
    questions: [
      {
        id: 'q1',
        title: 'Which is a fruit?',
        type: 'single',
        options: [
          { label: 'A', value: 'A' },
          { label: 'B', value: 'B' },
          { label: 'C', value: 'C' },
          { label: 'D', value: 'D' }
        ],
        score: 5
      }
    ]
  },
  6: {
    "title": "2025上学期《Java程序设计》期末测验",
    "questions": [
      {
        "id": "q1",
        "title": "Java程序的三大特性是什么？请按顺序写出名称。",
        "type": "short",
        "score": 6
      },
      {
        "id": "q2",
        "title": "抽象类和接口的区别是？请简要说明。",
        "type": "short",
        "score": 6
      },
      {
        "id": "q3",
        "title": "用于实现多态性的Java机制是____。",
        "type": "blank",
        "score": 3
      },
      {
        "id": "q4",
        "title": "下列哪项不是Java关键字？",
        "type": "single",
        "score": 4,
        "options": [
          {"value": "A", "label": "const"},
          {"value": "B", "label": "interface"},
          {"value": "C", "label": "null"}
        ]
      },
      {
        "id": "q5",
        "title": "请选择所有访问修饰符。",
        "type": "multiple",
        "score": 4,
        "options": [
          {"value": "A", "label": "public"},
          {"value": "B", "label": "private"},
          {"value": "C", "label": "synchronized"}
        ]
      },
      {
        "id": "q6",
        "title": "Java中实现线程安全的集合类是____。",
        "type": "blank",
        "score": 3
      },
      {
        "id": "q7",
        "title": "this关键字的作用？",
        "type": "short",
        "score": 6
      },
      {
        "id": "q8",
        "title": "下列哪项不是基本数据类型？",
        "type": "single",
        "score": 4,
        "options": [
          {"value": "A", "label": "int"},
          {"value": "B", "label": "String"},
          {"value": "C", "label": "char"}
        ]
      },
      {
        "id": "q9",
        "title": "以下哪些属于运行时异常？",
        "type": "multiple",
        "score": 4,
        "options": [
          {"value": "A", "label": "NullPointerException"},
          {"value": "B", "label": "FileNotFoundException"},
          {"value": "C", "label": "IOException"},
          {"value": "D", "label": "ArrayIndexOutOfBoundsException"}
        ]
      },
      {
        "id": "q10",
        "title": "JVM的作用是？",
        "type": "single",
        "score": 4,
        "options": [
          {"value": "A", "label": "编译Java代码"},
          {"value": "B", "label": "执行字节码文件"},
          {"value": "C", "label": "调试程序"}
        ]
      },
      {
        "id": "q11",
        "title": "反射机制主要应用于？",
        "type": "single",
        "score": 4,
        "options": [
          {"value": "A", "label": "动态加载类"},
          {"value": "B", "label": "内存管理"},
          {"value": "C", "label": "异常处理"}
        ]
      },
      {
        "id": "q12",
        "title": "try-with-resources语法需实现____接口。",
        "type": "blank",
        "score": 3
      },
      {
        "id": "q13",
        "title": "Math.round(11.5)的返回值是____。",
        "type": "blank",
        "score": 3
      },
      {
        "id": "q14",
        "title": "Java中数组是对象。",
        "type": "judge",
        "score": 3
      },
      {
        "id": "q15",
        "title": "简述static关键字的四种应用场景。",
        "type": "short",
        "score": 6
      },
      {
        "id": "q16",
        "title": "Java中用于定义接口的关键字是？",
        "type": "single",
        "score": 4,
        "options": [
          {"value": "A", "label": "class"},
          {"value": "B", "label": "interface"},
          {"value": "C", "label": "abstract"},
          {"value": "D", "label": "extends"}
        ]
      },
      {
        "id": "q17",
        "title": "以下哪些是Java集合框架中的接口？",
        "type": "multiple",
        "score": 4,
        "options": [
          {"value": "A", "label": "List"},
          {"value": "B", "label": "Map"},
          {"value": "C", "label": "Set"},
          {"value": "D", "label": "Array"}
        ]
      },
      {
        "id": "q18",
        "title": "Java中HashMap允许null键和null值。",
        "type": "judge",
        "score": 4
      },
      {
        "id": "q19",
        "title": "Java中final关键字可以修饰哪些元素？",
        "type": "multiple",
        "score": 4,
        "options": [
          {"value": "A", "label": "类"},
          {"value": "B", "label": "方法"},
          {"value": "C", "label": "变量"},
          {"value": "D", "label": "接口"}
        ]
      },
      {
        "id": "q20",
        "title": "Java中String类是可变类。",
        "type": "judge",
        "score": 3
      },
      {
        "id": "q21",
        "title": "Java中实现多线程的方式有哪几种？",
        "type": "short",
        "score": 6
      },
      {
        "id": "q22",
        "title": "Java中用于创建对象的关键字是____。",
        "type": "blank",
        "score": 3
      },
      {
        "id": "q23",
        "title": "Java中super关键字的作用是____。",
        "type": "blank",
        "score": 3
      },
      {
        "id": "q24",
        "title": "Java中Thread类的start()方法用于启动线程。",
        "type": "judge",
        "score": 2
      },
      {
        "id": "q25",
        "title": "Java中异常处理机制使用哪些关键字？",
        "type": "multiple",
        "score": 4,
        "options": [
          {"value": "A", "label": "try"},
          {"value": "B", "label": "catch"},
          {"value": "C", "label": "finally"},
          {"value": "D", "label": "throw"}
        ]
      }
    ]
  }
};
const examViewVisible = ref(false);
const currentExamPaper = ref(null);
function openExamView(row) {
  // 取模拟数据，真实项目可用row.id请求后端
  currentExamPaper.value = examPapers[row.id] || null;
  examViewVisible.value = true;
}

function headerStyle() {
  return {
    background: 'linear-gradient(90deg, #4f8cff 0%, #235ee7 100%)',
    color: '#fff',
    fontWeight: 'bold',
    fontSize: '16px',
    letterSpacing: '1px',
  };
}
</script>

<style scoped>
.exam-review-bg {
  min-height: 100vh;
  background: linear-gradient(135deg, #eaf1ff 0%, #f7faff 100%);
  padding-bottom: 60px;
}
.banner {
  width: 100%;
  height: 180px;
  background: linear-gradient(90deg, #4f8cff 0%, #235ee7 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.10);
  border-radius: 0 0 32px 32px;
  margin-bottom: 36px;
}
.banner-content {
  text-align: center;
  color: #fff;
}
.banner-content h1 {
  font-size: 38px;
  font-weight: 900;
  margin-bottom: 10px;
  letter-spacing: 3px;
  text-shadow: 0 4px 16px rgba(35,94,231,0.18);
}
.banner-content p {
  font-size: 18px;
  opacity: 0.92;
  letter-spacing: 1px;
}
.exam-review-card {
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 12px 40px 0 rgba(31, 38, 135, 0.16);
  padding: 48px 48px 32px 48px;
  min-height: 700px;
  max-width: 1400px;
  margin: -80px auto 0 auto;
  position: relative;
  z-index: 2;
}
.header-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 24px;
}
.search-input {
  width: 320px;
  font-size: 16px;
}
.custom-table {
  border-radius: 18px;
  overflow: hidden;
  font-size: 16px;
  background: rgba(255,255,255,0.98);
  box-shadow: 0 2px 12px 0 rgba(35,94,231,0.06);
}
.el-table th {
  font-size: 17px;
  background: linear-gradient(90deg, #eaf1ff 0%, #f7faff 100%);
}
.el-tag {
  font-size: 15px;
  padding: 5px 18px;
  border-radius: 8px;
}
.publish-date {
  color: #235ee7;
  font-weight: 600;
  letter-spacing: 1px;
}
.el-button {
  min-width: 80px;
  font-size: 15px;
  border-radius: 8px;
}
.pagination-bar {
  margin-top: 32px;
  display: flex;
  justify-content: flex-end;
}
.el-dialog {
  border-radius: 18px;
}
@media (max-width: 900px) {
  .exam-review-card {
    padding: 24px 8px 16px 8px;
  }
  .banner {
    height: 120px;
  }
  .banner-content h1 {
    font-size: 26px;
  }
}
</style>