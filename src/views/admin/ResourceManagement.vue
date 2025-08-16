<template>
  <div class="resource-management">
    <div class="toolbar">
      <el-select v-model="selectedSubject" placeholder="按学科筛选" @change="filterResources" style="margin-left: 10px;">
        <el-option label="全部学科" value=""></el-option>
        <el-option v-for="subject in subjects" :key="subject" :label="subject" :value="subject"></el-option>
      </el-select>
      <el-button type="primary" @click="exportAllResources" style="margin: 10px;">导出全部</el-button>
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
      <el-table-column label="操作" width="240">
        <template #default="scope">
          <!-- <el-button size="small" @click="editResource(scope.row)">编辑</el-button> -->
          <el-button size="small" type="danger" @click="deleteResource(scope.row)">删除</el-button>
          <el-button size="small" @click="exportSingleResource(scope.row)">导出</el-button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { adminService, studentService } from '@/services/api';

const subjects = ref([]);
const selectedSubject = ref('');

// 分页相关状态
const currentPage = ref(1);
const pageSize = ref(15);
const loading = ref(false);

const resources = ref([]);

// 获取课程列表用于学科筛选
const getCourses = async () => {
  try {
    const response = await adminService.getAllCourses();
    
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

const getResource = async() => {
  try {
    loading.value = true;
    // 1. 获取资源数据
    const resourceResponse = await adminService.getResource();

    // 检查响应是否成功
    if (!resourceResponse.data || resourceResponse.data.code !== 0) {
      throw new Error(resourceResponse.data?.msg || '获取资源失败');
    }

    // 2. 获取所有课程数据
    const courseResponse = await studentService.getCourses();

    // 检查响应是否成功
    if (!courseResponse.data || courseResponse.data.code !== 0) {
      throw new Error(courseResponse.data?.msg || '获取课程失败');
    }

    // 3. 创建课程ID到课程信息的映射
    const courseMap = {};
    courseResponse.data.data.forEach(course => {
      courseMap[course.courseId] = {
        courseName: course.courseName,
        teacherName: course.teacher.user.realName || '未知教师'
      };
    });

    // 4. 处理资源数据，添加课程名称和教师信息
    const processedResources = resourceResponse.data.data.map(resource => {
      const courseInfo = courseMap[resource.courseId] || {
        courseName: '未知课程',
        teacherName: '未知教师'
      };

      return {
        id: resource.resourceId,
        title: resource.resourceTitle,
        subject: courseInfo.courseName,
        teacher: courseInfo.teacherName,
        createdAt: resource.createdAt.split('T')[0], // 提取日期部分
        description: resource.resourceDescription,
        type: resource.resourceType,
        file: {
          name: resource.fileName,
          url: resource.fileUrl,
          size: resource.fileSizeMb
        },
        tags: resource.tags,
        downloadCount: resource.downloadCount,
        isPublic: resource.isPublic
      };
    });

    // 5. 更新资源列表 - 这是关键步骤
    resources.value = processedResources;

    // 6. 重置分页到第一页
    currentPage.value = 1;

    return processedResources;

  } catch (error) {
    console.error('获取资源失败:', error);
    // 返回空数组或根据业务需求处理错误
    return [];
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

const dialogVisible = ref(false);
const dialogTitle = ref('');
const form = ref({
  id: null,
  title: '',
  subject: '',
  teacher: '',
});

const filterResources = () => {
  // 筛选后自动重置到第一页
  currentPage.value = 1;
};

// const addResource = () => {
//   dialogTitle.value = '新增课件';
//   form.value = { id: null, title: '', subject: '', teacher: '' };
//   dialogVisible.value = true;
// };

// const editResource = (resource) => {
//   dialogTitle.value = '编辑课件';
//   form.value = { ...resource };
//   dialogVisible.value = true;
// };


const cancelForm = () => {
  dialogVisible.value = false;
};

const deleteResource = (resource) => {
  ElMessageBox.confirm(`确定要删除课件 "${resource.title}" 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    center: true,
    customClass: 'delete-confirm-dialog'
  }).then(async () => {
    // 显示加载消息
    const loadingMessage = ElMessage({
      message: '正在删除资源...',
      type: 'info',
      duration: 0,
      icon: 'el-icon-loading', // 使用加载图标
    });

    try {
      const response = await adminService.deleteResource(resource.id);

      loadingMessage.close();

      if (response.data && response.data.code === 0) {
        resources.value = (resources.value || []).filter(r => r.id !== resource.id);
        ElMessage.success('删除成功');
        
        // 删除后检查当前页是否还有数据，如果没有则跳转到上一页
        if (paginatedResources.value.length === 0 && currentPage.value > 1) {
          currentPage.value--;
        }
      } else {
        throw new Error(response.data?.msg || '删除操作失败');
      }
    } catch (error) {
      // 关闭加载消息
      loadingMessage.close();
      console.error('删除资源失败:', error);

      let errorMsg = '删除失败';
      if (error.response) {
        errorMsg += ` (状态码: ${error.response.status})`;
      } else if (error.message) {
        errorMsg += `: ${error.message}`;
      }

      ElMessage.error(errorMsg);
    }
  }).catch(() => {
    // 用户取消操作
    ElMessage.info('已取消删除');
  });
};

const exportAllResources = () => {
  console.log('Exporting all filtered resources:', filteredResources.value);
  ElMessage.success('所有筛选资源已导出');
};

const exportSingleResource = (resource) => {
  console.log('Exporting single resource:', resource);
  ElMessage.success(`课件 "${resource.title}" 已导出`);
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
</style>