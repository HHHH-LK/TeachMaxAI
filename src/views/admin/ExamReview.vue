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
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import ExamAttempt from '@/components/ExamAttempt.vue';
import { adminService, studentService } from '@/services/api';

const exams = ref([]);
const search = ref('');
const pageSize = 8;
const currentPage = ref(1);
const dialogVisible = ref(false);
const dialogTitle = ref('');
const dialogMessage = ref('');
const examViewVisible = ref(false);
const currentExamPaper = ref(null);
let dialogAction = null;
let dialogRow = null;

const filteredExams = computed(() => {
  const keyword = search.value.trim().toLowerCase();
  if (!keyword) return exams.value;

  return exams.value.filter(
      exam =>
          (exam.name || '').toLowerCase().includes(keyword) ||
          (exam.creator || '').toLowerCase().includes(keyword)
  );
});

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
    updateExamStatus(dialogRow.id, 'completed')
        .then(() => {
          dialogRow.status = '已审核';
          ElMessage.success('审核通过！');
        })
        .catch(error => {
          console.error('审核失败:', error);
          ElMessage.error('审核操作失败');
        });
  } else if (dialogAction === 'publish') {
    updateExamStatus(dialogRow.id, 'scheduled')
        .then(() => {
          dialogRow.status = '已发布';
          ElMessage.success('发布成功！');
        })
        .catch(error => {
          console.error('发布失败:', error);
          ElMessage.error('发布操作失败');
        });
  }
  dialogVisible.value = false;
}

// 修复3：添加状态更新API函数
async function updateExamStatus(examId, status) {
  try {
    const response = await adminService.updateExamStatus({
      examId: examId,
      status: status
    });

    if (response.data && response.data.code === 0) {
      return response.data;
    }

    throw new Error(response.data?.msg || '状态更新失败');
  } catch (error) {
    console.error(`更新试卷状态失败 (examId: ${examId}, status: ${status}):`, error);
    throw error;
  }
}

const examPapers = ref({});

function openExamView(row) {
  currentExamPaper.value = examPapers.value[row.id] || null;
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

const getAllPapers = async () => {
  try {
    console.log('开始获取试卷数据...');

    // 1. 获取所有课程数据
    console.log('获取课程数据...');
    const courseResponse = await studentService.getCourses();
    console.log('课程响应:', courseResponse);

    if (!courseResponse.data || courseResponse.data.code !== 0) {
      const errorMsg = courseResponse.data?.msg || '获取课程失败';
      console.error(errorMsg);
      throw new Error(errorMsg);
    }

    // 2. 创建课程映射
    console.log('创建课程映射...');
    const courseMap = {};
    courseResponse.data.data.forEach(course => {
      const teacherName = course.teacher?.user?.realName || '未知教师';
      courseMap[course.courseId] = {
        courseName: course.courseName,
        teacherName: teacherName
      };
    });

    // 3. 获取试卷数据
    console.log('获取试卷数据...');
    const courseIds = Object.keys(courseMap);

    const paperPromises = courseIds.map(courseId =>
        adminService.getAllPaper(courseId)
            .then(response => {
              if (response.data && response.data.code === 0 && Array.isArray(response.data.data)) {
                return response.data.data;
              }
              console.warn(`课程 ${courseId} 的试卷获取失败:`, response.data?.message);
              return [];
            })
            .catch(error => {
              console.error(`获取课程 ${courseId} 的试卷失败:`, error);
              return [];
            })
    );

    const papersResults = await Promise.all(paperPromises);
    const allPapers = papersResults.flat();
    console.log('原始试卷数据:', allPapers);

    // 4. 处理试卷数据
    const processedPapers = allPapers
        .filter(paper => paper?.status !== 'draft')
        .map(paper => {
          if (!paper || typeof paper !== 'object') {
            console.warn('无效的试卷数据:', paper);
            return null;
          }

          const courseInfo = courseMap[paper.courseId] || {
            courseName: '未知课程',
            teacherName: '未知教师'
          };

          const statusMap = {
            'draft': '待审核',
            'completed': '已结束',
            'scheduled': '已发布'
          };

          const status = paper.status || 'unknown';
          const statusText = statusMap[status] || '未知';

          const formatDate = (dateStr) => {
            if (!dateStr) return '';
            try {
              return dateStr.split('T')[0];
            } catch (e) {
              return dateStr;
            }
          };

          return {
            id: paper.examId || 0,
            name: paper.title || '未命名试卷',
            creator: courseInfo.teacherName,
            createdAt: formatDate(paper.createdAt),
            expectedPublish: formatDate(paper.examDate),
            status: statusText,
            courseName: courseInfo.courseName
          };
        })
        .filter(paper => paper !== null);

    console.log('处理后的试卷数据（已跳过草稿状态）:', processedPapers);

    // 5. 更新exams ref
    exams.value = processedPapers;

    return processedPapers;

  } catch (error) {
    console.error('获取试卷数据失败:', error);

    const backupData = [
      { id: 1, name: '数学期中试卷', creator: '张发在', createdAt: '2024-04-01',
        expectedPublish: '2024-05-01', status: '待审核' },
      { id: 2, name: '英语月考试卷', creator: '李京东', createdAt: '2024-04-05',
        expectedPublish: '2024-05-03', status: '已审核' },
      { id: 3, name: '物理模拟卷', creator: '王暖是', createdAt: '2024-03-28',
        expectedPublish: '2024-04-20', status: '已发布' },
      { id: 4, name: '化学期末卷', creator: '赵星河', createdAt: '2024-04-10',
        expectedPublish: '2024-05-10', status: '待审核' },
      { id: 5, name: '生物竞赛卷', creator: '钱三幅', createdAt: '2024-04-12',
        expectedPublish: '2024-05-12', status: '已审核' },
      { id: 6, name: 'Java期末考试', creator: '张冬梅', createdAt: '2025-07-12',
        expectedPublish: '2025-07-12', status: '已发布' },
    ];

    exams.value = backupData.filter(item => item.status !== 'draft');

    return exams.value;
  }
};
const getPaperQues = async () => {
  try {
    console.log('开始获取试卷详情...');

    if (!exams.value || exams.value.length === 0) {
      console.warn('exams.value为空，无法获取试卷详情');
      return;
    }

    const examIds = exams.value.map(exam => exam.id);
    const papers = [];

    // 串行处理每份试卷
    for (const examId of examIds) {
      try {
        console.log(`正在获取试卷 ${examId} 的题目...`);

        const examIdNum = Number(examId);
        if (isNaN(examIdNum)) {
          throw new Error(`无效的试卷ID: ${examId}`);
        }


        const response = await adminService.getQuestion(examIdNum);

        if (!response.data || response.data.code !== 0) {
          throw new Error(response.data?.msg || `获取试卷 ${examId} 题目失败`);
        }

        const questionsData = response.data.data;
        if (!Array.isArray(questionsData)) {
          throw new Error(`试卷 ${examId} 题目数据格式无效`);
        }

        // 转换数据结构
        const questions = questionsData.map(q => {
          let options = [];
          try {
            if (q.questionOptions) {

              const parsedOptions = JSON.parse(q.questionOptions);
              options = parsedOptions.map(opt => ({
                label: opt.label || '',
                value: opt.value || opt.label || '',
                text: opt.content || opt.text || ''
              }));
            }
          } catch (error) {
            console.error(`解析试卷 ${examId} 选项失败:`, error);
          }

          const typeMap = {
            'single_choice': 'single',
            'multiple_choice': 'multiple',
            'true_false': 'judge',
            'short_answer': 'short',
            'fill_blank': 'blank'
          };

          return {
            id: `q${q.questionId}`,
            title: q.questionContent,
            type: typeMap[q.questionType] || q.questionType,
            options: options,
            score: q.scorePoints || 0
          };
        });

        // 使用exams.value中的试卷名称
        const examInfo = exams.value.find(e => e.id === examId);
        papers.push({
          examId: examId,
          title: examInfo?.name || '未命名试卷',
          questions: questions
        });

        // 存储到examPapers以便预览
        examPapers.value[examId] = {
          title: examInfo?.name || '未命名试卷',
          questions: questions
        };

        console.log(`试卷 ${examId} 题目获取成功`);

      } catch (error) {
        console.error(`处理试卷 ${examId} 时出错:`, error);

        // 尝试使用后备数据
        if (examPapers.value[examId]) {
          papers.push({
            examId: examId,
            title: examPapers.value[examId].title,
            questions: examPapers.value[examId].questions
          });
          console.log(`使用后备数据 for 试卷 ${examId}`);
        } else {
          const examInfo = exams.value.find(e => e.id === examId);
          papers.push({
            examId: examId,
            title: examInfo?.name || `试卷 ${examId} 详情获取失败`,
            questions: []
          });
          console.warn(`无法获取试卷 ${examId} 且无后备数据`);
        }
      }
    }

    // 更新exams数据添加paper属性
    exams.value = exams.value.map(exam => {
      const paper = papers.find(p => p.examId === exam.id);
      return {
        ...exam,
        paper: paper || {
          title: `试卷 ${exam.id} 详情获取失败`,
          questions: []
        }
      };
    });

    console.log('更新带试卷详情的exams:', exams.value);

  } catch (error) {
    console.error('处理试卷详情时发生全局错误:', error);

    exams.value = exams.value.map(exam => ({
      ...exam,
      paper: {
        title: `试卷 ${exam.id} 详情获取失败`,
        questions: []
      }
    }));
  }
};

onMounted(async () => {
  await getAllPapers();
  await getPaperQues();
});
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