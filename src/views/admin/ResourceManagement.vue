<template>
  <div class="resource-management">
    <div class="toolbar">

      <el-select v-model="selectedSubject" placeholder="按学科筛选" @change="filterResources" style="margin-left: 10px;">
        <el-option label="全部学科" value=""></el-option>
        <el-option v-for="subject in subjects" :key="subject" :label="subject" :value="subject"></el-option>
      </el-select>
      <el-button type="primary" @click="exportAllResources" style="margin: 10px;">导出全部</el-button>
      <el-button type="primary" @click="addResource">新增课件</el-button>
    </div>
    <el-table :data="filteredResources" stripe style="width: 100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="subject" label="学科" width="180" />
      <el-table-column prop="teacher" label="教师" width="180" />
      <el-table-column prop="createdAt" label="创建日期" />
      <el-table-column label="操作" width="240">
        <template #default="scope">
          <el-button size="small" @click="editResource(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteResource(scope.row)">删除</el-button>
          <el-button size="small" @click="exportSingleResource(scope.row)">导出</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title"></el-input>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="form.subject" placeholder="请选择学科">
            <el-option v-for="subject in subjects" :key="subject" :label="subject" :value="subject"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="教师">
          <el-input v-model="form.teacher"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelForm">取消</el-button>
          <el-button type="primary" @click="saveResource">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';

const subjects = ref(['数学', '物理', '英语', '语文', '化学', '生物', '历史', '地理', '政治']);
const selectedSubject = ref('');

const resources = ref([
  { id: 1, title: '函数与导数课件', subject: '数学', teacher: '李老师', createdAt: '2023-03-15' },
  { id: 2, title: '力学基础练习', subject: '物理', teacher: '王老师', createdAt: '2023-03-20' },
  { id: 3, title: '时态语法总结', subject: '英语', teacher: '赵老师', createdAt: '2023-03-25' },
  { id: 4, title: '解析几何初步', subject: '数学', teacher: '李老师', createdAt: '2023-04-01' },
  { id: 5, title: '元素周期表', subject: '化学', teacher: '钱老师', createdAt: '2023-04-05' },
  { id: 6, title: '细胞结构', subject: '生物', teacher: '孙老师', createdAt: '2023-04-10' },
]);

const filteredResources = computed(() => {
  if (!selectedSubject.value) {
    return resources.value;
  }
  return resources.value.filter(r => r.subject === selectedSubject.value);
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
  // The computed property will automatically update the table
};

const addResource = () => {
  dialogTitle.value = '新增课件';
  form.value = { id: null, title: '', subject: '', teacher: '' };
  dialogVisible.value = true;
};

const editResource = (resource) => {
  dialogTitle.value = '编辑课件';
  form.value = { ...resource };
  dialogVisible.value = true;
};

const saveResource = () => {
  if (form.value.id) {
    // Edit
    const index = resources.value.findIndex(r => r.id === form.value.id);
    if (index !== -1) {
      resources.value[index] = { ...form.value };
    }
  } else {
    // Add
    const newResource = {
      ...form.value,
      id: Date.now(), // simple id generation
      createdAt: new Date().toISOString().split('T')[0],
    };
    resources.value.push(newResource);
  }
  dialogVisible.value = false;
  ElMessage.success(dialogTitle.value + '成功');
};

const cancelForm = () => {
  dialogVisible.value = false;
};

const deleteResource = (resource) => {
  ElMessageBox.confirm(`确定要删除课件 “${resource.title}” 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    resources.value = resources.value.filter(r => r.id !== resource.id);
    ElMessage.success('删除成功');
  }).catch(() => {
    // catch cancel
  });
};

const exportAllResources = () => {
  console.log('Exporting all filtered resources:', filteredResources.value);
  // Here you would implement the actual export logic (e.g., to CSV or Excel)
  ElMessage.success('所有筛选资源已导出');
};

const exportSingleResource = (resource) => {
  console.log('Exporting single resource:', resource);
  // Here you would implement the actual export logic for a single resource
  ElMessage.success(`课件 “${resource.title}” 已导出`);
};
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
</style>