<template>
  <div class="teaching-resources-container">
    <el-card class="resource-card">
      <template #header>
        <div class="card-header">
          <span>教学资源管理</span>
          <div>
            <el-button @click="fetchResource" :loading="loading" style="margin-right: 10px;">刷新</el-button>
            <el-button type="primary" @click="showUploadDialog = true">上传PPT</el-button>
          </div>
        </div>
      </template>

      <el-table :data="resources" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="资源名称" width="180" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="description" label="描述" width="200" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="previewResource(scope.row)">预览</el-button>
            <el-button size="small" type="primary" @click="publishToStudents(scope.row)">发布</el-button>
            <el-button size="small" type="danger" @click="deleteResource(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传教学资源" width="30%">
      <el-upload
        class="upload-demo"
        drag
        action="#"
        multiple
        :on-change="handleUploadChange"
        :auto-upload="false"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持上传PPT/PDF/Word文件，大小不超过50MB
          </div>
        </template>
      </el-upload>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showUploadDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmUpload">确认上传</el-button>
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
import { UploadFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { studentService, teacherService } from "@/services/api.js";

const route = useRoute();
const courseId = ref(route.params.courseId);

// const res = teacherService.assessment.getAllMaterialsByChapterAndCourse('2', courseId.value)
// console.log(res)

const fetchResource = async() =>{
  try{
    loading.value = true;
    const chapters = [];
    const responseChapter = await studentService.getChapterInfo(courseId.value);

    if(responseChapter.data && responseChapter.data.success){
      chapters.push(...responseChapter.data.data.map(item => ({
        id: item.chapterId
      })));
    }
    
    // 清空现有资源
    resources.value = [];
    
    for(var i = 0; i < chapters.length; i++){
      const response = await studentService.getAllResources(chapters[i].id, courseId.value)
      
      if(response.data && response.data.success && response.data.data.courseList){
        const resourcesTemp = response.data.data.courseList.map(item => ({
          id: item.materialId,
          name: item.materialTitle,
          type: item.materialTypeCn || item.materialType,
          description: item.materialDescription,
          uploadTime: new Date().toLocaleString(), // 由于API没有返回时间，使用当前时间
          url: `https://www.w3cschool.cn/java/java-book.html` // 临时链接
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
const uploadFile = ref(null);

const handleUploadChange = (file) => {
  uploadFile.value = file;
};

const confirmUpload = () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择要上传的文件');
    return;
  }

  // 模拟上传过程
  const newResource = {
    id: resources.value.length + 1,
    name: uploadFile.value.name,
    type: uploadFile.value.name.split('.').pop().toUpperCase(),
    description: '新上传的教学资源',
    uploadTime: new Date().toLocaleString(),
    url: `/resources/${uploadFile.value.name}`
  };

  resources.value.push(newResource);
  showUploadDialog.value = false;
  ElMessage.success('上传成功');
};

const previewResource = (resource) => {
  currentPreviewResource.value = resource;
  showPreviewDialog.value = true;
};

const publishToStudents = (resource) => {
  // 这里调用API发布资源给学生
  ElMessage.success(`已发布资源: ${resource.name}`);
};

const deleteResource = (resource) => {
  // 这里调用API删除资源
  resources.value = resources.value.filter(r => r.id !== resource.id);
  ElMessage.success('删除成功');
};

const getPreviewUrl = (resource) => {
  // 实际项目中这里应该返回资源的预览URL
  return resource.url;
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
</style>