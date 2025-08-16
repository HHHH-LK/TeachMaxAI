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
          <el-table-column prop="phone" label="联系电话" width="150" />
          <el-table-column prop="email" label="邮箱" width="200" />
          <el-table-column prop="courseName" label="所属课程" width="200" />
        </el-table>
      </el-card>
    </div>
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

const teacherId = "1"

const fetchCourseList = async() =>{
  const response = await teacherService.getAllCourse(teacherId);
  classList.value = response.data.data.map(item => ({
    id: item.courseId,
    name: item.courseName
  }))
  // console.log("class", classList);
}

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
    console.log("response", response)
    if(response.data){
      studentList.value = response.data.data.map(item => ({
        studentId: item.studentNumber,
        name: item.realName,
        phone: item.phone,
        email: item.email,
        courseName: item.courseName
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