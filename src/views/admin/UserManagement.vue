<template>
  <div class="user-management">
    <div class="toolbar">
      <el-radio-group v-model="filterRole">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="管理员">管理员</el-radio-button>
        <el-radio-button label="教师">教师</el-radio-button>
        <el-radio-button label="学生">学生</el-radio-button>
      </el-radio-group>
      <el-button type="primary" @click="addUser">新增用户</el-button>
    </div>
    <el-table :data="filteredUsers" stripe style="width: 100%">
      <el-table-column prop="name" label="姓名" width="180" />
      <el-table-column prop="role" label="角色" width="180" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="createdAt" label="创建日期" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="editUser(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteUser(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="管理员" value="管理员"></el-option>
            <el-option label="教师" value="教师"></el-option>
            <el-option label="学生" value="学生"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelForm">取消</el-button>
          <el-button type="primary" @click="saveUser">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';

const users = ref([
  { id: 1, name: '王科飞', role: '管理员', email: 'wang@example.com', createdAt: '2023-01-15' },
  { id: 2, name: '张冬梅', role: '教师', email: 'zhang@example.com', createdAt: '2023-02-20' },
  { id: 3, name: '陈清泉', role: '教师', email: 'chenqing@example.com', createdAt: '2023-02-22'},
  { id: 4, name: '李芭比', role: '教师', email: 'liba@example.com', createdAt: '2023-04-11'},
  { id: 5, name: '李贵阳', role: '学生', email: 'ligui@example.com', createdAt: '2023-03-10' },
  { id: 6, name: '胡海涛', role: '学生', email: 'huhai@example.com', createdAt: '2023-04-01' },
  { id: 7, name: '陈小明', role: '学生', email: 'xiaoming@example.com', createdAt: '2023-03-20'},
  { id: 8, name: '刘小红', role: '学生', email: 'xiaohong@example.com', createdAt: '2023-02-11'},
  { id: 9, name: '张小华', role: '学生', email: 'xiaohua@example.com', createdAt: '2023-03-21'},
  { id: 10, name: '孙小亮', role: '学生', email: 'xiaoliang@example.com', createdAt: '2023-02-22'},
  { id: 11, name: '周小雯', role: '学生', email: 'xiaowen@example.com', createdAt: '2023-04-02'},
  { id: 12, name: '冯小琴', role: '学生', email: 'xiaoqin@example.com', createdAt: '2023-03-14'},
  { id: 13, name: '韩小慧', role: '学生', email: 'xiaohui@example.com', createdAt: '2023-02-19'},
  { id: 14, name: '吴小强', role: '学生', email: 'xiaoqiang@example.com', createdAt: '2023-03-11'},
]);

const filterRole = ref('');

const filteredUsers = computed(() => {
  if (!filterRole.value) {
    return users.value;
  }
  return users.value.filter(user => user.role === filterRole.value);
});

const dialogVisible = ref(false);
const dialogTitle = ref('');
const form = ref({
  id: null,
  name: '',
  role: '',
  email: '',
});

const addUser = () => {
  dialogTitle.value = '新增用户';
  form.value = { id: null, name: '', role: '', email: '' };
  dialogVisible.value = true;
};

const editUser = (user) => {
  dialogTitle.value = '编辑用户';
  form.value = { ...user };
  dialogVisible.value = true;
};

const saveUser = () => {
  if (form.value.id) {
    // Edit
    const index = users.value.findIndex(u => u.id === form.value.id);
    if (index !== -1) {
      users.value[index] = { ...form.value };
    }
  } else {
    // Add
    const newUser = {
      ...form.value,
      id: Date.now(), // simple id generation
      createdAt: new Date().toISOString().split('T')[0],
    };
    users.value.push(newUser);
  }
  dialogVisible.value = false;
  ElMessage.success(dialogTitle.value + '成功');
};

const cancelForm = () => {
  dialogVisible.value = false;
}

const deleteUser = (user) => {
  ElMessageBox.confirm(`确定要删除用户 “${user.name}” 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    users.value = users.value.filter(u => u.id !== user.id);
    ElMessage.success('删除成功');
  }).catch(() => {
    // catch cancel
  });
};
</script>

<style scoped>
.user-management {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  margin-bottom: 16px;
  justify-content: space-between;
}
</style>