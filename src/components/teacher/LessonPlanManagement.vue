<template>
  <div class="lesson-plan-management-container">
         <!-- 头部操作区域 -->
     <div class="header-actions">
       <div class="left-section">
         <h2 class="page-title">教案管理</h2>
         <p class="page-description">管理您的课程教案，包括查看、编辑和删除教案</p>
       </div>
             <div class="right-section">
         <el-button @click="refreshLessonPlans">
           <el-icon><Refresh /></el-icon>
           刷新
         </el-button>
       </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-filter-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-input
            v-model="searchQuery"
            placeholder="搜索教案名称..."
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
         <el-col :span="4">
           <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="handleFilter">
           <el-option label="全部" value="" />
           <el-option label="草稿" value="draft" />
           <el-option label="待审核" value="pending" />
           <el-option label="审核通过" value="approved" />
           <el-option label="审核拒绝" value="rejected" />
         </el-select>
         </el-col>
      </el-row>
    </div>

         <!-- 教案列表 -->
     <div class="lesson-plans-list">
       <!-- 加载状态 -->
       <div v-if="loading" class="loading-state">
         <el-skeleton :rows="3" animated />
         <el-skeleton :rows="3" animated />
         <el-skeleton :rows="3" animated />
       </div>

       <!-- 错误状态 -->
       <div v-else-if="hasError" class="error-state">
         <el-result
           icon="error"
           title="加载失败"
           :sub-title="errorMessage"
         >
           <template #extra>
             <el-button type="primary" @click="loadLessonPlans">重试</el-button>
           </template>
         </el-result>
       </div>

       <!-- 教案卡片列表 -->
       <el-row v-else :gutter="20">
         <el-col
           v-for="lessonPlan in paginatedLessonPlans"
           :key="lessonPlan.id"
           :span="8"
           class="lesson-plan-col"
         >
          <el-card class="lesson-plan-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <div class="lesson-plan-title">
                  <h3>{{ lessonPlan.title }}</h3>
                  <el-tag
                    :type="getStatusType(lessonPlan.status)"
                    size="small"
                  >
                    {{ getStatusText(lessonPlan.status) }}
                  </el-tag>
                </div>
                <div class="lesson-plan-actions">
                  <el-dropdown @command="handleAction">
                    <el-button type="text" size="small">
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item :command="{ action: 'view', id: lessonPlan.id }">
                          <el-icon><View /></el-icon>查看
                        </el-dropdown-item>
                        <el-dropdown-item :command="{ action: 'edit', id: lessonPlan.id }" v-if="['draft', 'pending', 'approved'].includes(lessonPlan.status)">
                          <el-icon><Edit /></el-icon>编辑
                        </el-dropdown-item>
                        <el-dropdown-item :command="{ action: 'publish', id: lessonPlan.id }" v-if="lessonPlan.status === 'draft'">
                          <el-icon><Upload /></el-icon>发布
                        </el-dropdown-item>
                        <el-dropdown-item :command="{ action: 'delete', id: lessonPlan.id }" divided>
                          <el-icon><Delete /></el-icon>删除
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </template>

            <div class="lesson-plan-content">
              <div class="lesson-plan-info">
                <div class="meta-info">
                  <div class="meta-item">
                    <el-icon><User /></el-icon>
                    <span>{{ lessonPlan.teacherName }}</span>
                  </div>
                </div>
                <div class="creation-info">
                  <span class="created-time">创建时间：{{ formatDate(lessonPlan.createdAt) }}</span>
                  <span class="updated-time" v-if="lessonPlan.updatedAt">
                    更新时间：{{ formatDate(lessonPlan.updatedAt) }}
                  </span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

             <!-- 空状态 -->
       <div v-if="!loading && totalFilteredItems === 0" class="empty-state">
         <el-empty description="暂无教案数据">
         </el-empty>
       </div>
    </div>

         <!-- 分页 -->
     <div class="pagination-section" v-if="!loading && totalFilteredItems > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[6, 12, 24]"
        :total="totalFilteredItems"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

         <!-- 编辑教案对话框 -->
     <el-dialog
       v-model="showEditDialog"
       title="编辑教案"
       width="60%"
       @close="resetForm"
     >
       <el-form :model="lessonPlanForm" :rules="formRules" ref="lessonPlanFormRef" label-width="120px">
         <el-row :gutter="20">
           <el-col :span="12">
             <el-form-item label="教案标题" prop="title">
               <el-input v-model="lessonPlanForm.title" placeholder="请输入教案标题" />
             </el-form-item>
           </el-col>
         </el-row>

         <el-row :gutter="12">
           <!-- 左侧编辑区 -->
           <el-col :span="12">
             <el-form-item label="教案内容" prop="content">
               <el-input v-model="lessonPlanForm.content"
                         type="textarea"
                         :rows="20"
                         placeholder="支持 Markdown 语法"/>
             </el-form-item>
           </el-col>

           <!-- 右侧实时预览 -->
           <el-col :span="12">
             <div class="preview-panel">
               <h4 style="margin-top: 0;">实时预览</h4>
               <div class="markdown-body scroll-preview" v-html="editPreviewHtml"></div>
             </div>
           </el-col>
         </el-row>
       </el-form>

       <template #footer>
         <el-button @click="showEditDialog=false">取消</el-button>
         <el-button type="primary" @click="saveLessonPlan" :loading="saving">更新</el-button>
       </template>
     </el-dialog>

    <!-- 查看教案对话框 -->
    <el-dialog
      v-model="showViewDialog"
      title="教案详情"
      width="70%"
    >
      <div class="lesson-plan-detail" v-if="currentLessonPlan">
        <div class="detail-header">
          <h2>{{ currentLessonPlan.title }}</h2>
          <div class="detail-meta">
            <el-tag :type="getStatusType(currentLessonPlan.status)" size="large">
              {{ getStatusText(currentLessonPlan.status) }}
            </el-tag>
            <span class="meta-text">创建时间：{{ formatDate(currentLessonPlan.createdAt) }}</span>
            <span class="meta-text" v-if="currentLessonPlan.updatedAt">
              更新时间：{{ formatDate(currentLessonPlan.updatedAt) }}
            </span>
          </div>
        </div>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">{{ getStatusText(currentLessonPlan.status) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-content">
           <h3>详细内容</h3>
          <div class="markdown-body" v-html="renderedContent"></div>
         </div>
      </div>

      <template #footer>
        <el-button @click="showViewDialog=false">关闭</el-button>
        <el-button type="primary"
                   v-if="['draft','pending','approved'].includes(currentLessonPlan.status)"
                   @click="editCurrentLessonPlan">编辑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherService } from '@/services/api.js'
import renderMarkdown from '@/utils/markdown.js';
import {
  Refresh,
  Search,
  MoreFilled,
  View,
  Edit,
  Upload,
  Delete,
  User,
  Check,
  Close
} from '@element-plus/icons-vue'

// 定义 props
const props = defineProps({
  courseId: {
    type: [String, Number],
    required: true
  }
})

// 响应式数据
const showEditDialog = ref(false)
const showViewDialog = ref(false)
const saving = ref(false)
const loading = ref(false)
const hasError = ref(false)
const errorMessage = ref('')
const currentPage = ref(1)
const pageSize = ref(6)
const searchQuery = ref('')
const filterStatus = ref('')

const renderedContent = computed(() => renderMarkdown(currentLessonPlan.value?.content || ''));
const editPreviewHtml = computed(() => renderMarkdown(lessonPlanForm.value.content || ''));

// 教案表单
const lessonPlanForm = ref({
  title: '',
  className: '',
  content: ''
})

// 当前查看的教案
const currentLessonPlan = ref(null)

// 教案列表
const lessonPlans = ref([])

// 表单验证规则
const formRules = {
  title: [{ required: true, message: '请输入教案标题', trigger: 'blur' }],
  className: [{ required: true, message: '请输入授课班级', trigger: 'blur' }],
  content: [{ required: true, message: '请输入教案内容', trigger: 'blur' }]
}

// 表单引用
const lessonPlanFormRef = ref()

// 计算属性
const filteredLessonPlans = computed(() => {
  let filtered = lessonPlans.value

  // 搜索过滤
  if (searchQuery.value) {
    filtered = filtered.filter(plan =>
      plan.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      plan.description.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }

  // 状态过滤
  if (filterStatus.value) {
    filtered = filtered.filter(plan => plan.status === filterStatus.value)
  }

  return filtered
})

// 分页后的教案列表
const paginatedLessonPlans = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value
  const endIndex = startIndex + pageSize.value
  return filteredLessonPlans.value.slice(startIndex, endIndex)
})

// 更新总数为过滤后的数据长度
const totalFilteredItems = computed(() => filteredLessonPlans.value.length)

// 方法
// 状态映射函数
const mapAuditStatus = (auditStatus) => {
  if (!auditStatus) return 'draft'
  const statusMap = {
    'DRAFT': 'draft',
    'PENDING': 'pending',
    'APPROVED': 'approved',
    'REJECTED': 'rejected'
  }
  return statusMap[auditStatus.toUpperCase()] || 'draft'
}

const loadLessonPlans = async () => {
  loading.value = true
  hasError.value = false
  errorMessage.value = ''

  try {
    console.log('正在获取课程ID为', props.courseId, '的教案...')
    const response = await teacherService.getLessonPlanByCourseId(props.courseId)
    console.log('API响应:', response)

    if (response.data.success && response.data) {
             // 将API返回的数据映射到组件使用的数据结构
      lessonPlans.value = response.data.data.map(item => ({
        id: item.planId,
        title: item.planTitle,
        className: item.courseName || '未知课程',
        content: item.planContent || '暂无内容',
        status: mapAuditStatus(item.auditStatus),
        createdAt: item.createdAt,
        updatedAt: item.updatedAt,
        teacherName: item.teacherName || '张冬梅',
        courseId: item.courseId,
        chapterId: item.chapterId,
        teacherId: item.teacherId,
        originalData: item
      }))
      console.log('映射后的教案数据:', lessonPlans.value)
    } else {
      lessonPlans.value = []
      if (response.message) {
        ElMessage.warning(response.message)
      }
    }
  } catch (error) {
    console.error('获取教案列表失败:', error)
    hasError.value = true
    errorMessage.value = '获取教案列表失败，请稍后重试'
    lessonPlans.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleFilter = () => {
  currentPage.value = 1
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

const refreshLessonPlans = async () => {
  await loadLessonPlans()
  ElMessage.success('教案列表已刷新')
}

const handleAction = (command) => {
  const { action, id } = command
  const lessonPlan = lessonPlans.value.find(plan => plan.id === id)

  switch (action) {
    case 'view':
      currentLessonPlan.value = lessonPlan
      showViewDialog.value = true
      break
    case 'edit':
      editLessonPlan(lessonPlan)
      break
    case 'publish':
      publishLessonPlan(id)
      break
    case 'delete':
      deleteLessonPlan(id)
      break
  }
}

const editLessonPlan = (lessonPlan) => {
  lessonPlanForm.value = {
    id: lessonPlan.id,
    title: lessonPlan.title,
    className: lessonPlan.className,
    content: lessonPlan.content,
    courseId: lessonPlan.courseId,
    chapterId: lessonPlan.chapterId,
    teacherId: lessonPlan.teacherId
  };
  showEditDialog.value = true;
};

const editCurrentLessonPlan = () => {
  showViewDialog.value = false
  editLessonPlan(currentLessonPlan.value)
}

const publishLessonPlan = async (id) => {
  try {
    await ElMessageBox.confirm('确定要发布这个教案吗？发布后将进入审核流程', '确认发布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    try {
      // 调用API更新教案状态为待审核
      await teacherService.updateLessonPlanToPending(id)

      // 更新本地数据
      const plan = lessonPlans.value.find(p => p.id === id)
      if (plan) {
        plan.status = 'pending'
        plan.updatedAt = new Date().toISOString()
      }

      ElMessage.success('教案已提交审核')
    } catch (error) {
      console.error('发布教案失败:', error)
      ElMessage.error('发布教案失败，请稍后重试')
    }
  } catch {
    // 用户取消
  }
}

const deleteLessonPlan = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个教案吗？删除后无法恢复！', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    try {
      // 调用API删除教案
      await teacherService.deleteLessonPlan(id)

      // 更新本地数据
      const index = lessonPlans.value.findIndex(p => p.id === id)
      if (index > -1) {
        lessonPlans.value.splice(index, 1)
      }

      ElMessage.success('教案删除成功')
    } catch (error) {
      console.error('删除教案失败:', error)
      ElMessage.error('删除教案失败，请稍后重试')
    }
  } catch {
    // 用户取消
  }
}

const saveLessonPlan = async () => {
  try {
    await lessonPlanFormRef.value.validate();
    saving.value = true;

    const currentPlan = lessonPlans.value.find(p => p.id === lessonPlanForm.value.id);
    if (!currentPlan) {
      ElMessage.error('未找到对应的教案');
      return;
    }

    const payload = {
      ...currentPlan.originalData, // 保留所有原始字段
      planTitle: lessonPlanForm.value.title,
      planContent: lessonPlanForm.value.content,
      courseName: lessonPlanForm.value.className,
      updatedAt: new Date().toISOString()
    };

    await teacherService.updateLessonPlan(payload);
    console.log(payload)

    // 更新本地数据
    Object.assign(currentPlan, {
      title: payload.planTitle,
      content: payload.planContent,
      className: payload.courseName,
      updatedAt: payload.updatedAt
    });

    ElMessage.success('教案更新成功');
    showEditDialog.value = false;
    resetForm();
  } catch (error) {
    console.error('更新教案失败:', error);
    ElMessage.error('更新教案失败，请稍后重试');
  } finally {
    saving.value = false;
  }
};

const resetForm = () => {
  lessonPlanForm.value = {
    id: null,
    title: '',
    className: '',
    content: '',
    courseId: null,
    chapterId: null,
    teacherId: null
  };
  lessonPlanFormRef.value?.resetFields();
};

const getStatusType = (status) => {
  const statusMap = {
    draft: 'info',
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    draft: '草稿',
    pending: '待审核',
    approved: '审核通过',
    rejected: '审核拒绝'
  }
  return statusMap[status] || '未知'
}

const getTypeText = (type) => {
  const typeMap = {
    theory: '理论课',
    lab: '实验课',
    seminar: '研讨课',
    mixed: '混合课'
  }
  return typeMap[type] || '未知'
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 生命周期
onMounted(async () => {
  await loadLessonPlans()
})
</script>

<style scoped lang="less">
.lesson-plan-management-container {
  padding: 20px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.left-section {
  flex: 1;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.page-description {
  color: #6b7280;
  margin: 0;
  font-size: 14px;
}

.debug-info {
  color: #9ca3af;
  margin: 4px 0 0 0;
  font-size: 12px;
  font-family: monospace;
}

.right-section {
  display: flex;
  gap: 12px;
}

.search-filter-section {
  margin-bottom: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.lesson-plans-list {
  margin-bottom: 24px;
}

.loading-state {
  padding: 40px 20px;
  text-align: center;

  .el-skeleton {
    margin-bottom: 20px;
  }
}

.error-state {
  padding: 40px 20px;
  text-align: center;
}

.lesson-plan-col {
  margin-bottom: 20px;
}

.lesson-plan-card {
  height: 100%;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.lesson-plan-title {
  flex: 1;

  h3 {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
  }
}

.lesson-plan-actions {
  flex-shrink: 0;
}

.lesson-plan-content {
  .description {
    color: #6b7280;
    margin-bottom: 16px;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.meta-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 13px;

  .el-icon {
    font-size: 14px;
  }
}

.creation-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;

  .created-time,
  .updated-time {
    display: block;
  }
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.lesson-plan-detail {
  .detail-header {
    margin-bottom: 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e5e7eb;

    h2 {
      margin: 0 0 16px 0;
      color: #1f2937;
    }
  }

  .detail-meta {
    display: flex;
    align-items: center;
    gap: 16px;

    .meta-text {
      color: #6b7280;
      font-size: 14px;
    }
  }

  .detail-content {
    h3 {
      color: #1f2937;
      margin: 24px 0 12px 0;
      font-size: 16px;
      font-weight: 600;
    }

    p {
      color: #374151;
      line-height: 1.6;
      margin-bottom: 16px;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.markdown-body {
  line-height: 1.7;
}
.markdown-body h1,
.markdown-body h2,
.markdown-body h3 {
  margin-top: 1.2em;
  margin-bottom: 0.6em;
}
.markdown-body pre {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}
.markdown-table {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}
.markdown-table th,
.markdown-table td {
  border: 1px solid #dcdfe6;
  padding: 6px 10px;
}
.preview-panel {
  height: 100%;
  background: #fafafa;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.scroll-preview {
  height: 500px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  padding: 8px;
}
</style>
