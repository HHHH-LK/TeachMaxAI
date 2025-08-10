<template>
  <div class="resource-view">
    <h2>课程资源内容</h2>
    <div class="resource-list">
      <div class="resource-item" v-for="resource in resources" :key="resource.id">
        <div class="resource-icon">
          <i :class="resource.icon"></i>
        </div>
        <div class="resource-details">
          <h3>{{ resource.title }}</h3>
          <p>{{ resource.description }}</p>
        </div>
        <div class="resource-actions">
          <button v-if="resource.type === 'pdf'" @click="previewResource(resource)" class="preview-button">预览</button>
          <a :href="resource.link" target="_blank" class="download-link">下载</a>
        </div>
      </div>
    </div>
  </div>

  <!-- 预览模态框 -->
  <div v-if="showPreviewModal" class="preview-modal-overlay">
    <div class="preview-modal-content">
      <div class="preview-modal-header">
        <h3>资源预览</h3>
        <button @click="showPreviewModal = false" class="preview-modal-close">&times;</button>
      </div>
      <div class="preview-modal-body">
        <iframe :src="previewUrl" frameborder="0"></iframe>
      </div>
    </div>
  </div>
</template>

<script setup>
import { studentService } from '@/services/api';
// import { c } from 'vite/dist/node/moduleRunnerTransport.d-DJ_mE5sf';
import { ref, watch, defineProps } from 'vue';
const props = defineProps({
  courseId: {
    type: Number,
    required: true,
  },
});

const resources = ref([]);

const showPreviewModal = ref(false);
const previewUrl = ref('');
const loading = ref(false);
const error = ref(null);

//获取课程资源
const fetchResource = async() =>{
  try{
    loading.value = true;
    error.value = null;
    const chapters = ref([]);
    const responseChapter = await studentService.getChapterInfo(props.courseId);
  
    if(responseChapter.data){
      chapters.value = responseChapter.data.data.map(item => ({
        id: item.chapterId
      }));
    }
    for(var i = 0; i < chapters.value.length; i++){
      const response = await studentService.getAllResources(chapters.value[i].id, props.courseId)
      // console.log("data", response.data.data.courseList)
      const resourcesTemp = response.data.data.courseList.map(item => ({
        id: item.materialId,
        title: item.materialTitle,
        description: item. materialDescription,
        icon: 'icon-file-pdf',
        link: "https://www.w3cschool.cn/java/java-book.html",
        type: "pdf"
      }));
      resources.value.push(...resourcesTemp)
    }
    // console.log("resource", resources.value)
  }catch(e){
    console.log("获取失败")
  }
}

const previewResource = (resource) => {
  if (resource.type === 'pdf') {
    previewUrl.value = resource.link;
    showPreviewModal.value = true;
  } else {
    alert('目前只支持PDF文件预览。');
  }
};

watch(
  () => props.courseId,
  (newCourseId) => {
    fetchResource()
  },
  { immediate: true }
); 
</script>

<style scoped>
.resource-view {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.resource-list {
  display: grid;
  gap: 20px;
}

.resource-item {
  display: flex;
  justify-content: space-between; /* 将内容和按钮推向两端 */
  align-items: center;
  background-color: #f8fafc; /* @bg-light */
  border: 1px solid #e2e8f0; /* @border-light */
  border-radius: 8px;
  padding: 15px;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1); /* @shadow-sm */
    transform: translateY(-2px);
  }
}

.resource-icon {
  font-size: 30px;
  color: #2563eb; /* @primary */
  margin-right: 15px;
}

.resource-details {
  /* flex-grow: 1; */ /* 移除 flex-grow */
  margin-right: auto; /* 将其推到左边 */

  h3 {
    margin: 0 0 5px 0;
    font-size: 18px;
    color: #1e293b; /* @text-primary */
  }

  p {
    margin: 0;
    font-size: 14px;
    color: #64748b; /* @text-secondary */
  }
}

.download-link,
.preview-button {
  display: inline-block;
  background-color: #2563eb; /* @primary */
  color: white;
  padding: 8px 15px;
  border-radius: 5px;
  text-decoration: none;
  font-size: 14px;
  transition: background-color 0.3s ease;
  margin-left: 10px; /* Add some space between buttons */
  border: none;
  cursor: pointer;

  &:hover {
    background-color: #1d4ed8; /* @primary-dark */
  }
}

/* 预览模态框样式 */
.preview-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.preview-modal-content {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  width: 80%;
  height: 80%;
  display: flex;
  flex-direction: column;
}

.preview-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.preview-modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
}

.preview-modal-body {
  flex-grow: 1;
}

.preview-modal-body iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 示例图标样式 */
.icon-file-pdf {
  background-image: url('data:image/svg+xml;charset=UTF-8,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"%3E%3Cpath d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM16 18H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/%3E%3C/svg%3E');
}

.icon-file-word {
  background-image: url('data:image/svg+xml;charset=UTF-8,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"%3E%3Cpath d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM16 18H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/%3E%3C/svg%3E');
}

.icon-file-ppt {
  background-image: url('data:image/svg+xml;charset=UTF-8,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"%3E%3Cpath d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM16 18H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/%3E%3C/svg%3E');
}
</style>