<template>
  <div class="student-management">
    <div class="page-header">
      <h2>学生信息管理</h2>
      <!-- <p>查看和管理学生信息及考试情况</p> -->
    </div>

    <!-- 课程选择 -->
    <div class="class-selector">
      <el-card class="filter-card">
        <template #header>
          <div class="card-header">
            <span>课程筛选</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-select
                v-model="selectedClass"
                placeholder="请选择课程"
                @change="handleClassChange"
                style="width: 100%"
            >
              <el-option
                  v-for="classItem in classList"
                  :key="classItem.id"
                  :label="classItem.name"
                  :value="classItem.id"
              />
            </el-select>
          </el-col>
          <el-col :span="8">
            <el-button type="primary" @click="refreshData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 学生信息表格 -->
    <div class="student-table" v-if="selectedClass">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>{{ getSelectedClassName() }} - 学生信息</span>
          </div>
        </template>

        <el-table
            :data="studentList"
            style="width: 100%"
            v-loading="loading"
            stripe
        >
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="name" label="姓名" width="100" />
          <!-- <el-table-column prop="gender" label="性别" width="80" /> -->
          <el-table-column prop="phone" label="联系电话" width="150" />
          <el-table-column prop="email" label="邮箱" width="200" />
          <!-- <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
                {{ scope.row.status === 'active' ? '在读' : '休学' }}
              </el-tag>
            </template>
          </el-table-column> -->
          <!-- <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewStudentDetail(scope.row)">
                查看详情
              </el-button>
              <el-button size="small" type="primary" @click="viewExamHistory(scope.row)">
                考试记录
              </el-button>
            </template>
          </el-table-column> -->
        </el-table>
      </el-card>
    </div>

    <!-- 考试情况统计 -->
    <!-- <div class="exam-statistics" v-if="selectedClass">
      <el-row :gutter="20" class="statistics-row">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ examStats.totalStudents }}</div>
              <div class="stat-label">班级总人数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ examStats.avgScore }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ examStats.passRate }}%</div>
              <div class="stat-label">及格率</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-number">{{ examStats.excellentRate }}%</div>
              <div class="stat-label">优秀率</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div> -->

    <!-- 学生详情对话框 -->
    <!-- <el-dialog
      v-model="studentDetailVisible"
      title="学生详细信息"
      width="600px"
    >
      <div v-if="selectedStudent" class="student-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学号">{{ selectedStudent.studentId }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ selectedStudent.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ selectedStudent.gender }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ selectedStudent.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selectedStudent.email }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedStudent.status === 'active' ? 'success' : 'danger'">
              {{ selectedStudent.status === 'active' ? '在读' : '休学' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="入学时间">{{ selectedStudent.enrollmentDate }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ getSelectedClassName() }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog> -->

    <!-- 考试记录对话框 -->
    <!-- <el-dialog
      v-model="examHistoryVisible"
      title="考试记录"
      width="800px"
    >
      <div v-if="selectedStudent" class="exam-history">
        <h4>{{ selectedStudent.name }} 的考试记录</h4>
        <el-table :data="examHistory" style="width: 100%">
          <el-table-column prop="examName" label="考试名称" />
          <el-table-column prop="examDate" label="考试日期" width="120" />
          <el-table-column prop="score" label="成绩" width="80" />
          <el-table-column prop="totalScore" label="总分" width="80" />
          <el-table-column prop="rank" label="排名" width="80" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getExamStatusType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog> -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { teacherService } from '@/services/api.js'

// 响应式数据
const selectedClass = ref('')
const loading = ref(false)
const studentList = ref([])
const classList = ref([])
const examStats = ref({
  totalStudents: 0,
  avgScore: 0,
  passRate: 0,
  excellentRate: 0
})

// 对话框控制
const studentDetailVisible = ref(false)
const examHistoryVisible = ref(false)
const selectedStudent = ref(null)
const examHistory = ref([])
const teacherId = "1"


const fetchCourseList = async() =>{
  const response = await teacherService.getAllCourse(teacherId);
  classList.value = response.data.data.map(item => ({
    id: item.courseId,
    name: item.courseName
  }))
  // console.log("class", classList);
}

const fetchStudent = async() => {
  console.log("select", selectedClass.value)
}

// 模拟数据（作为降级方案）
const mockClassList = [
  { id: 'class1', name: '计算机科学与技术1班' },
  { id: 'class2', name: '软件工程1班' },
  { id: 'class3', name: '人工智能1班' },
  { id: 'class4', name: '数据科学1班' }
]

//模拟学生数据
const mockStudentData = {
  class1: [
    { studentId: '2023001', name: '张三', gender: '男', phone: '13800138001', email: 'zhangsan@example.com', status: 'active', enrollmentDate: '2021-09-01' },
    { studentId: '2021002', name: '李四', gender: '女', phone: '13800138002', email: 'lisi@example.com', status: 'active', enrollmentDate: '2021-09-01' },
    { studentId: '2021003', name: '王五', gender: '男', phone: '13800138003', email: 'wangwu@example.com', status: 'active', enrollmentDate: '2021-09-01' }
  ],
  class2: [
    { studentId: '2022001', name: '赵六', gender: '女', phone: '13800138004', email: 'zhaoliu@example.com', status: 'active', enrollmentDate: '2022-09-01' },
    { studentId: '2022002', name: '钱七', gender: '男', phone: '13800138005', email: 'qianqi@example.com', status: 'active', enrollmentDate: '2022-09-01' }
  ]
}

//模拟考试记录数据
const mockExamHistory = [
  { examName: '期中考试', examDate: '2024-03-15', score: 85, totalScore: 100, rank: 5, status: '已通过' },
  { examName: '期末考试', examDate: '2024-06-20', score: 92, totalScore: 100, rank: 3, status: '已通过' },
  { examName: '实验考核', examDate: '2024-05-10', score: 88, totalScore: 100, rank: 4, status: '已通过' }
]

// 方法
const handleClassChange = async () => {
  if (selectedClass.value) {
    await Promise.all([loadStudentData()])
  }
}

//  获取学生数据
const loadStudentData = async () => {
  loading.value = true
  try {
    const response = await teacherService.getAllStudent(selectedClass.value)
    if(response.data){
      studentList.value = response.data.data.map(item => ({
        studentId: item.studentNumber,
        name: item.realName,
        phone: item.phone,
        email: item.email
      }))
    }
  } catch (error) {
    console.error('获取学生数据失败:', error)
    ElMessage.error('获取学生数据失败')
    // 降级到模拟数据
    studentList.value = mockStudentData[selectedClass.value] || []
  } finally {
    loading.value = false
  }
}


//获取考试统计
// const loadExamStats = async () => {
//   try {
//     const response = await teacherService.getClassExamStats(selectedClass.value)
//     if (response.success) {
//       examStats.value = response.data
//     } else {
//       ElMessage.error('获取考试统计失败')
//       // 降级到模拟数据
//       const students = mockStudentData[selectedClass.value] || []
//       examStats.value = {
//         totalStudents: students.length,
//         avgScore: 87.5,
//         passRate: 95.2,
//         excellentRate: 23.8
//       }
//     }
//   } catch (error) {
//     console.error('获取考试统计失败:', error)
//     ElMessage.error('获取考试统计失败')
//     // 降级到模拟数据
//     const students = mockStudentData[selectedClass.value] || []
//     examStats.value = {
//       totalStudents: students.length,
//       avgScore: 87.5,
//       passRate: 95.2,
//       excellentRate: 23.8
//     }
//   }
// }

//刷新数据
const refreshData = async () => {
  if (selectedClass.value) {
    await Promise.all([loadStudentData(), loadExamStats()])
    ElMessage.success('数据已刷新')
  } else {
    ElMessage.warning('请先选择班级')
  }
}

const getSelectedClassName = () => {
  const classItem = classList.value.find(item => item.id === selectedClass.value)
  return classItem ? classItem.name : ''
}

const viewStudentDetail = (student) => {
  selectedStudent.value = student
  studentDetailVisible.value = true
}

const viewExamHistory = async (student) => {
  selectedStudent.value = student
  try {
    const response = await teacherService.getStudentExamHistory(student.studentId)
    if (response.success) {
      examHistory.value = response.data
    } else {
      ElMessage.error('获取考试记录失败')
      // 降级到模拟数据
      examHistory.value = mockExamHistory
    }
  } catch (error) {
    console.error('获取考试记录失败:', error)
    ElMessage.error('获取考试记录失败')
    // 降级到模拟数据
    examHistory.value = mockExamHistory
  }
  examHistoryVisible.value = true
}

const getExamStatusType = (status) => {
  switch (status) {
    case '已通过': return 'success'
    case '未通过': return 'danger'
    case '待考试': return 'warning'
    default: return 'info'
  }
}



// 生命周期
onMounted(() => {
  fetchCourseList();
})
</script>

<style scoped>
.student-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.class-selector {
  margin-bottom: 24px;
}

.filter-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.student-table {
  margin-bottom: 24px;
}

.statistics-row {
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;
}

.stat-content {
  padding: 20px;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.student-detail {
  padding: 20px 0;
}

.exam-history h4 {
  margin-bottom: 16px;
  color: #303133;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .student-management {
    padding: 10px;
  }

  .statistics-row .el-col {
    margin-bottom: 16px;
  }
}
</style> 