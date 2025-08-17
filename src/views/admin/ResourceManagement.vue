<template>
  <div class="resource-management">
    <div class="toolbar">
      <el-select v-model="selectedSubject" placeholder="按学科筛选" @change="filterResources" style="margin-left: 10px;">
        <el-option label="全部学科" value=""></el-option>
        <el-option v-for="subject in subjects" :key="subject" :label="subject" :value="subject"></el-option>
      </el-select>
    </div>

    <!-- 数据统计信息 -->
    <div class="data-info">
      <span>共找到 {{ totalCount }} 条资源记录</span>
    </div>

    <el-table :data="paginatedResources" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="subject" label="学科" width="180" />
      <el-table-column prop="teacher" label="教师" width="180" />
      <el-table-column prop="createdAt" label="创建日期" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag
              :type="getStatusTagType(scope.row.status)"
              size="small"
          >
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="scope">
          <!-- 查看按钮 -->
          <el-button
              size="small"
              type="primary"
              @click="handleView(scope.row)"
          >
            查看
          </el-button>
          <!-- 审核通过按钮 -->
          <el-button
              size="small"
              type="success"
              @click="handleApprove(scope.row)"
              :disabled="scope.row.status === 'approved'"
          >
            审核通过
          </el-button>
          <!-- 审核退回按钮 -->
          <el-button
              size="small"
              type="danger"
              @click="handleReject(scope.row)"
              :disabled="scope.row.status === 'rejected'"
          >
            审核退回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <div class="pagination-container">
      <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[15, 20, 50, 100]"
          :total="totalCount"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          background
      />
    </div>

    <!-- 简化的资源详情查看对话框 -->
    <el-dialog
        v-model="viewDialogVisible"
        :title="selectedResource ? selectedResource.title : '资源详情'"
        width="60%"
    >
      <div v-if="selectedResource" class="resource-detail">
        <el-descriptions column="1" border>
          <!-- 只保留标题、老师和内容（描述） -->
          <el-descriptions-item label="标题">{{ selectedResource.title }}</el-descriptions-item>
          <el-descriptions-item label="教师">{{ selectedResource.teacher }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="1">
            <p class="resource-content">{{ selectedResource.description || '无内容' }}</p>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessageBox, ElMessage, ElDescriptions, ElDescriptionsItem, ElTag } from 'element-plus';
import {adminService, teacherService} from '@/services/api';

const subjects = ref([]);
const selectedSubject = ref('');

// 分页相关状态
const currentPage = ref(1);
const pageSize = ref(15);
const loading = ref(false);

const resources = ref([]);
// 查看详情相关状态
const viewDialogVisible = ref(false);
const selectedResource = ref(null);

// 获取课程列表用于学科筛选
const getCourses = async () => {
  try {
    const response = await adminService.getAllCourses();
    console.log('获取课程列表成功', response)

    if (response.data && response.data.success && response.data.data) {
      // 从课程数据中提取课程名称作为学科选项
      const courseNames = response.data.data.map(course => course.courseName);
      subjects.value = courseNames;
    }
  } catch (error) {
    console.error('获取课程列表失败:', error);
    // 如果获取失败，使用默认学科列表
    subjects.value = ['数学', '物理', '英语', '语文', '化学', '生物', '历史', '地理', '政治'];
  }
};

const getResource = async () => {
  loading.value = true;
  try {
    // 2.1 课程列表
    const { data: courseRes } = await adminService.getAllCourses();
    if (!courseRes || courseRes.code !== 0) throw new Error(courseRes?.msg || '课程列表获取失败');
    const courseList = courseRes.data;

    // 2.2 逐个课程拉教案
    const plansAll = [];
    await Promise.all(
        courseList.map(async (c) => {
          try {
            const { data: planRes } = await teacherService.getLessonPlanByCourseId(c.courseId);
            if (planRes?.code === 0 && Array.isArray(planRes.data)) {
              planRes.data.forEach((p) => {
                if (p.auditStatus === 'draft') return;

                let st = 'pending';
                if (p.auditStatus === 'approved') st = 'approved';
                else if (p.auditStatus === 'rejected') st = 'rejected';
                else if (p.auditStatus === 'draft') st = 'draft';

                plansAll.push({
                  id: p.planId,
                  title: p.planTitle,
                  subject: c.courseName,
                  teacher: c.teacher?.user?.realName || '未知教师',
                  createdAt: p.createdAt?.split('T')[0] || '',
                  description: p.planContent,          // 教案正文
                  status: st
                });
              });
            }
          } catch (e) {
            console.warn(`课程 ${c.courseId} 教案拉取失败`, e);
          }
        })
    );

    resources.value = plansAll;
    currentPage.value = 1;
  } catch (e) {
    console.error('获取资源失败:', e);
    resources.value = [];
  } finally {
    loading.value = false;
  }
};

// 过滤后的资源
const filteredResources = computed(() => {
  if (!selectedSubject.value) {
    return resources.value;
  }
  return resources.value.filter(r => r.subject === selectedSubject.value);
});

// 分页后的资源
const paginatedResources = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize.value;
  const endIndex = startIndex + pageSize.value;
  return filteredResources.value.slice(startIndex, endIndex);
});

// 总数量
const totalCount = computed(() => {
  return filteredResources.value.length;
});

// 分页大小改变处理
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  currentPage.value = 1; // 重置到第一页
};

// 当前页改变处理
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage;
};

// 监听筛选条件变化，重置分页
watch(selectedSubject, () => {
  currentPage.value = 1;
});

const filterResources = () => {
  // 筛选后自动重置到第一页
  currentPage.value = 1;
};

// 查看资源详情（只展示标题、老师和内容）
const handleView = (resource) => {
  selectedResource.value = { ...resource }; // 复制资源对象
  viewDialogVisible.value = true;
};

// 审核通过处理
const handleApprove = async (resource) => {
  try {
    await ElMessageBox.confirm(
        `确定要审核通过资源 "${resource.title}" 吗?`,
        '审核确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        }
    );

    // 显示加载状态
    const loadingMessage = ElMessage({
      message: '正在处理审核...',
      type: 'info',
      duration: 0,
      icon: 'el-icon-loading'
    });

    // 调用审核通过API
    const response = await teacherService.updateLessonPlanStatus(resource.id, 'approved');

    loadingMessage.close();

    if (response.data && response.data.code === 0) {
      // 更新本地资源状态
      const index = resources.value.findIndex(r => r.id === resource.id);
      if (index !== -1) {
        resources.value[index].status = 'approved';
      }
      ElMessage.success('审核通过成功');
    } else {
      throw new Error(response.data?.msg || '审核通过失败');
    }
  } catch (error) {
    if (error !== 'cancel') { // 排除用户取消的情况
      console.error('审核通过失败:', error);
      ElMessage.error(error.message || '审核通过操作失败');
    }
  }
};

// 审核退回处理（无需理由）
const handleReject = async (resource) => {
  try {
    await ElMessageBox.confirm(
        `确定要审核退回资源 "${resource.title}" 吗?`,
        '审核确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    );

    // 显示加载状态
    const loadingMessage = ElMessage({
      message: '正在处理审核...',
      type: 'info',
      duration: 0,
      icon: 'el-icon-loading'
    });

    // 调用审核退回API（无需传递理由）
    const response = await teacherService.updateLessonPlanStatus(resource.id, 'rejected');

    loadingMessage.close();

    if (response.data && response.data.code === 0) {
      // 更新本地资源状态
      const index = resources.value.findIndex(r => r.id === resource.id);
      if (index !== -1) {
        resources.value[index].status = 'rejected';
      }
      ElMessage.success('审核退回成功');
    } else {
      throw new Error(response.data?.msg || '审核退回失败');
    }
  } catch (error) {
    if (error !== 'cancel') { // 排除用户取消的情况
      console.error('审核退回失败:', error);
      ElMessage.error(error.message || '审核退回操作失败');
    }
  }
};

// 状态文本映射
const getStatusText = (status) => {
  const statusMap = {
    'draft': '草稿',
    'pending': '待审核',
    'approved': '已通过',
    'rejected': '已退回'
  };
  return statusMap[status] || '未知';
};

// 状态标签样式映射
const getStatusTagType = (status) => {
  const typeMap = {
    'draft': 'info',
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  };
  return typeMap[status] || 'info';
};

onMounted(async() => {
  // 先获取课程列表，再获取资源数据
  await getCourses();
  await getResource();
})
</script>

<style scoped>
.resource-management {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.data-info {
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 操作按钮样式调整 */
:deep(.el-button--primary) {
  margin-right: 8px;
}

:deep(.el-button--success) {
  margin-right: 8px;
}

/* 详情对话框样式 */
.resource-detail {
  padding: 10px 0;
}

:deep(.el-descriptions__cell) {
  padding: 12px 16px;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
}

/* 内容样式优化 */
.resource-content {
  white-space: pre-wrap; /* 保留换行和空格 */
  line-height: 1.6;
  margin: 0;
  padding: 8px 0;
}
</style>
