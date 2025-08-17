<template>
  <div class="teaching-resources-container">
    <el-card class="resource-card">
      <template #header>
        <div class="card-header">
          <span>教学资源管理</span>
          <div>
            <el-button @click="fetchResource" :loading="loading" style="margin-right: 10px;">刷新</el-button>
            <el-button type="primary" @click="showUploadDialog = true">上传教学资源</el-button>
          </div>
        </div>
      </template>

      <el-table :data="resources" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="资源名称" width="180" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="description" label="描述" width="200" />
        <el-table-column prop="difficultyLevel" label="难度等级" width="100">
          <template #default="scope">
            <el-tag
              :type="getDifficultyTagType(scope.row.difficultyLevel)"
              size="small"
            >
              {{ getDifficultyLabel(scope.row.difficultyLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="estimatedTime" label="预计时长(分钟)" width="120" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="previewResource(scope.row)">预览</el-button>
            <el-button size="small" type="danger" @click="deleteResource(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传教学资源"
      width="50%"
      @close="resetUploadForm"
    >
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-width="120px">
        <el-form-item label="选择章节" prop="chapterId">
          <el-select v-model="uploadForm.chapterId" placeholder="请选择章节" style="width: 100%">
            <el-option
              v-for="chapter in chapters"
              :key="chapter.id"
              :label="chapter.name"
              :value="chapter.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资源名称" prop="materialTitle">
          <el-input v-model="uploadForm.materialTitle" placeholder="请输入资源名称" />
        </el-form-item>

        <el-form-item label="资源描述" prop="materialDescription">
          <el-input
            v-model="uploadForm.materialDescription"
            type="textarea"
            :rows="3"
            placeholder="请输入资源描述"
          />
        </el-form-item>

        <el-form-item label="预计学习时长" prop="estimatedTime">
          <el-input-number
            v-model="uploadForm.estimatedTime"
            :min="1"
            :max="480"
            placeholder="请输入预计学习时长(分钟)"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="难度等级" prop="difficultyLevel">
          <el-select v-model="uploadForm.difficultyLevel" placeholder="请选择难度等级" style="width: 100%">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>

        <el-form-item label="资源链接" prop="resourceUrl">
          <el-input
            v-model="uploadForm.resourceUrl"
            placeholder="请输入资源链接或存储路径"
            type="url"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUploadDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmUpload" :loading="uploading">确认上传</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog v-model="showPreviewDialog" title="资源预览" width="80%">
      <div class="preview-container">
        <iframe
          v-if="currentPreviewResource"
          :src="getPreviewUrl(currentPreviewResource)"
          frameborder="0"
          class="preview-iframe"
        ></iframe>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import { studentService, teacherService } from "@/services/api.js";
import {getCurrentUserId} from "@/utils/userUtils.js";

const route = useRoute();
const courseId = ref(route.params.courseId);

// 章节信息
const chapters = ref([]);

// 上传表单数据
const uploadForm = ref({
  chapterId: '',
  materialTitle: '',
  materialDescription: '',
  estimatedTime: 30,
  difficultyLevel: 'medium',
  resourceUrl: ''
});

// 上传表单验证规则
const uploadRules = {
  chapterId: [
    { required: true, message: '请选择章节', trigger: 'change' }
  ],
  materialTitle: [
    { required: true, message: '请输入资源名称', trigger: 'blur' },
    { min: 1, max: 100, message: '资源名称长度在1到100个字符', trigger: 'blur' }
  ],
  materialDescription: [
    { required: true, message: '请输入资源描述', trigger: 'blur' },
    { min: 1, max: 500, message: '资源描述长度在1到500个字符', trigger: 'blur' }
  ],
  estimatedTime: [
    { required: true, message: '请输入预计学习时长', trigger: 'blur' },
    { type: 'number', min: 1, max: 480, message: '预计学习时长在1到480分钟之间', trigger: 'blur' }
  ],
  difficultyLevel: [
    { required: true, message: '请选择难度等级', trigger: 'change' }
  ],
  resourceUrl: [
    { required: true, message: '请输入资源链接或存储路径', trigger: 'blur' },
    { type: 'url', message: '请输入有效的URL地址', trigger: 'blur' }
  ]
};

const uploadFormRef = ref(null);
const uploading = ref(false);

const fetchResource = async() =>{
  try{
    loading.value = true;
    const responseChapter = await studentService.getChapterInfo(courseId.value);

    if(responseChapter.data && responseChapter.data.success){
      chapters.value = responseChapter.data.data.map(item => ({
        id: item.chapterId,
        name: item.chapterName || `第${item.chapterId}章`
      }));
    }

    // 清空现有资源
    resources.value = [];

    for(var i = 0; i < chapters.value.length; i++){
      const response = await teacherService.getMaterialByCourseId(courseId.value)
      console.log("章节下的资源:", response.data)

      if(response.data && response.data.success && response.data.data){
        const resourcesTemp = response.data.data.map(item => ({
          id: item.materialId,
          name: item.materialTitle,
          type: item.materialTypeCn || item.materialType,
          description: item.materialDescription,
          difficultyLevel: item.difficultyLevel || 'medium',
          estimatedTime: item.estimatedTime || 30,
          uploadTime: item.createdAt,
          url: item.externalResourceUrl
        }));
        resources.value.push(...resourcesTemp)
      }
    }
    console.log("获取到的资源:", resources.value)
  } catch(e) {
    console.log("获取失败:", e)
    ElMessage.error('获取教学资源失败')
  } finally {
    loading.value = false;
  }
}

const resources = ref([]);
const loading = ref(false);

const showUploadDialog = ref(false);
const showPreviewDialog = ref(false);
const currentPreviewResource = ref(null);

const confirmUpload = async () => {
  try {
    // 验证表单
    const valid = await uploadFormRef.value.validate();
    if (!valid) {
      return;
    }

    if (!uploadForm.value.resourceUrl) {
      ElMessage.warning('请输入资源链接');
      return;
    }

    uploading.value = true;

    const userId = getCurrentUserId() || 1;

    // 构建完整的上传数据（包含所有必要参数）
    const uploadData = {
      courseId: courseId.value, // 从路由参数获取的课程ID
      chapterId: uploadForm.value.chapterId,
      resourceUrl: uploadForm.value.resourceUrl,
      materialTitle: uploadForm.value.materialTitle,
      materialDescription: uploadForm.value.materialDescription,
      estimatedTime: uploadForm.value.estimatedTime,
      userId: userId, // 补充用户ID
      difficultyLevel: uploadForm.value.difficultyLevel
    };

    // 调用真实的创建资源API
    const response = await teacherService.createMaterial(uploadData);

    // 处理API响应（根据后端实际响应格式调整）
    if (response.data && response.data.success) {
      ElMessage.success('资源上传成功');
      // 关闭对话框并重置表单
      showUploadDialog.value = false;
      resetUploadForm();
      // 重新获取资源列表，刷新表格
      fetchResource();
    } else {
      ElMessage.error(response.data?.message || '上传失败，请重试');
    }

  } catch (error) {
    console.error('上传失败:', error);
    // 处理网络错误或API异常
    ElMessage.error('网络错误，上传失败');
  } finally {
    uploading.value = false;
  }
};

const previewResource = (resource) => {
  currentPreviewResource.value = resource;
  showPreviewDialog.value = true;
};



const deleteResource = async (resource) => {
  try {
    // 弹出确认对话框
    await ElMessageBox.confirm(
        `确定要删除资源 "${resource.name}" 吗？此操作不可恢复。`,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
    );

    // 调用删除API
    const response = await teacherService.deleteMaterial(resource.id);

    if (response.data && response.data.success) {
      // 从本地资源列表中移除
      resources.value = resources.value.filter(r => r.id !== resource.id);
      ElMessage.success('资源删除成功');
    } else {
      throw new Error(response.data?.message || '删除失败');
    }
  } catch (error) {
    // 用户取消删除或出现错误
    if (error !== 'cancel') {
      console.error('删除资源失败:', error);
      ElMessage.error(error.message || '删除资源失败');
    }
  }
};

const getPreviewUrl = (resource) => {
  // 实际项目中这里应该返回资源的预览URL
  return resource.url;
};

// 获取难度等级标签类型
const getDifficultyTagType = (difficulty) => {
  switch (difficulty) {
    case 'easy':
      return 'success';
    case 'medium':
      return 'warning';
    case 'hard':
      return 'danger';
    default:
      return 'info';
  }
};

// 获取难度等级标签文本
const getDifficultyLabel = (difficulty) => {
  switch (difficulty) {
    case 'easy':
      return '简单';
    case 'medium':
      return '中等';
    case 'hard':
      return '困难';
    default:
      return '未知';
  }
};

// 重置上传表单
const resetUploadForm = () => {
  uploadForm.value = {
    chapterId: '',
    materialTitle: '',
    materialDescription: '',
    estimatedTime: 30,
    difficultyLevel: 'medium',
    resourceUrl: ''
  };
  if (uploadFormRef.value) {
    uploadFormRef.value.resetFields();
  }
};

// 监听路由参数变化
watch(
  () => route.params.courseId,
  (newCourseId) => {
    if (newCourseId && newCourseId !== courseId.value) {
      courseId.value = newCourseId;
      fetchResource();
    }
  }
);

// 组件挂载时获取资源数据
onMounted(() => {
  fetchResource();
});
</script>

<style scoped>
.teaching-resources-container {
  padding: 20px;
}

.resource-card {
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-container {
  width: 100%;
  height: 70vh;
}

.preview-iframe {
  width: 100%;
  height: 100%;
}

/* 上传表单样式 */
.upload-demo {
  width: 100%;
}

.el-upload__tip {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

/* 表单验证样式 */
.el-form-item.is-error .el-input__inner {
  border-color: #f56c6c;
}

.el-form-item.is-error .el-textarea__inner {
  border-color: #f56c6c;
}

/* 难度等级标签样式 */
.difficulty-tag {
  margin-left: 8px;
}

.difficulty-easy {
  background-color: #67c23a;
  color: white;
}

.difficulty-medium {
  background-color: #e6a23c;
  color: white;
}

.difficulty-hard {
  background-color: #f56c6c;
  color: white;
}
</style>