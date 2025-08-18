<template>
  <!-- 模板部分保持不变 -->
  <div class="user-center-container">
    <div class="background">
      <center-background />
    </div>

    <div class="profile-card" v-loading="loading" element-loading-text="加载中...">
      <div class="card-header">
        <div class="avatar-wrapper">
          <img
              :src="avatarUrl"
              class="avatar"
          />
          <div class="edit-overlay">
            <i class="el-icon-camera"></i>
          </div>
        </div>
        <div class="header-info">
          <h1>{{ form.real_name }}<span class="verification-icon">✓</span></h1>
          <div class="user-type">
            <span class="tag user-id">{{ form.user_id }}</span>
            <span class="tag">{{
                form.user_type === 'teacher' ? '老师' :
                    form.user_type === 'student' ? '学生' : '管理员'
              }}</span>
          </div>
        </div>
      </div>

      <div class="form-scroll-container">
        <div class="form-section">
          <div class="section-header">
            <i class="el-icon-user-solid"></i>
            <h3>个人信息</h3>
          </div>

          <el-form :model="form" class="profile-form">
            <div class="form-grid">
              <div class="form-group" v-for="field in dynamicFields" :key="field.key">
                <div class="form-label">
                  <i :class="field.icon"></i> {{ field.label }}
                </div>
                <el-input
                    v-model="form[field.key]"
                    :placeholder="'请输入' + field.label"
                    size="large"
                    clearable
                    :type="field.type || 'text'"
                    :disabled="field.disabled || !isEditing"
                >
                  <template #suffix v-if="field.locked">
                    <i class="el-icon-lock"></i>
                  </template>
                </el-input>
              </div>
            </div>
          </el-form>
        </div>
      </div>

      <div class="form-actions">
        <template v-if="!isEditing">
          <el-button
              type="primary"
              @click="enableEdit"
              class="update-btn"
              size="large"
          >
            <i class="el-icon-edit"></i> 更改
          </el-button>
        </template>

        <template v-else>
          <div class="edit-actions">
            <el-button
                type="primary"
                @click="centerDialogVisible = true"
                class="update-btn"
                size="large"
            >
              <i class="el-icon-check"></i> 保存更改
            </el-button>
            <el-button
                @click="cancelChanges"
                class="update-btn cancel"
                size="large"
            >
              <i class="el-icon-close"></i> 取消
            </el-button>
          </div>
        </template>
      </div>
    </div>

    <el-dialog
        v-model="centerDialogVisible"
        width="90%"
        max-width="500px"
        center
        class="confirm-dialog"
    >
      <div class="dialog-content">
        <div class="dialog-icon">
          <i class="el-icon-warning"></i>
        </div>
        <h3 class="dialog-title">确认修改个人信息</h3>
        <p class="dialog-message">您确定要保存对个人资料的修改吗？</p>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="centerDialogVisible = false" class="cancel-btn">
            取消
          </el-button>
          <el-button type="primary" @click="submitForm" class="confirm-btn">
            确认保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted } from "vue";
import CenterBackground from "@/components/student/center/CenterBackground.vue";
import { ElMessage } from "element-plus";
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/store/authStore';
import { teacherService, studentService, adminService } from '@/services/api';
import studentAvatar from '@/assets/student-avatar.png'
import teacherAvatar from '@/assets/teacher-avatar.png'
import adminAvatar from '@/assets/admin-avatar.png'

const route = useRoute();
const authStore = useAuthStore();
const centerDialogVisible = ref(false);
const isEditing = ref(false);
const loading = ref(false);

// 角色专属头像（差异化显示）
const avatarUrl = computed(() => {
  switch(userForm.value.user_type) {
    case 'student': return "https://tse3-mm.cn.bing.net/th/id/OIP-C.MyVTP6gOD1WSIDQ8CIV1qAHaHa?w=167&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3"; // 学生专属头像
    case 'teacher': return "https://tse2-mm.cn.bing.net/th/id/OIP-C.q_kfyYa8Hui3SiLYa-oqagHaGQ?w=221&h=187&c=7&r=0&o=5&dpr=1.5&pid=1.7"; // 教师专属头像
    case 'admin': return "https://tse4-mm.cn.bing.net/th/id/OIP-C._NeFXOQo9CFwgDT20CPNZgHaHa?w=160&h=180&c=7&r=0&o=5&dpr=1.5&pid=1.7"; // 管理员专属头像
    default: return 'https://randomuser.me/api/portraits/men/32.jpg';
  }
});

// 响应式用户表单状态（初始值为学生默认值）
const userForm = ref({
  user_id: "STD2023001", // 学生默认用户ID
  username: "zhangxm",
  real_name: "张小明", // 学生真实姓名
  email: "zhangxm@school.com",
  phone: "13812345678", // 学生手机号
  user_type: "student",
  // 学生字段
  student_number: "2023001001",
  grade: "大一",
  class_name: "计算机2301",
  student_id: 0, // 学生ID
  // 老师字段
  employee_id: "",
  department: "",
  // 管理员字段
  admin_id: "",
  manage_scope: "",
  admin_level: "",
  admin_department: "",
});

// 原始表单数据备份
const originalForm = ref({ ...userForm.value });

// 获取用户类型的函数
const getUserType = () => {
  const path = route.path;
  if (path.startsWith('/teacher')) return 'teacher';
  if (path.startsWith('/student')) return 'student';
  if (path.startsWith('/admin')) return 'admin';
  return 'student'; // 默认返回学生
};

// 设置默认用户数据的函数
const setDefaultUserData = (userType) => {
  switch (userType) {
    case 'teacher':
      userForm.value = {
        user_id: "TCH2023001",
        username: "zhangdm",
        real_name: "张冬梅",
        email: "zhangdm@school.com",
        phone: "13987654321",
        user_type: "teacher",
        employee_id: "T2023001",
        department: "计算机科学与技术",
        student_number: "",
        grade: "",
        class_name: "",
        admin_id: "",
        manage_scope: "",
      };
      break;
    case 'student':
      userForm.value = {
        user_id: "STD2023001",
        username: "zhangxm",
        real_name: "张小明",
        email: "zhangxm@school.com",
        phone: "13812345678",
        user_type: "student",
        student_number: "2023001001",
        grade: "大一",
        class_name: "计算机2301",
        student_id: 0, // 默认学生ID
        employee_id: "",
        department: "",
        admin_id: "",
        manage_scope: "",
      };
      break;
    case 'admin':
      userForm.value = {
        user_id: "ADM2023001",
        username: "wangfk",
        real_name: "王飞科",
        email: "wangfk@school.com",
        phone: "13788889999",
        user_type: "admin",
        admin_id: "A2023001",
        manage_scope: "用户管理、系统设置",
        admin_level: "super_admin",
        admin_department: "信息技术部",
        student_number: "",
        grade: "",
        class_name: "",
        employee_id: "",
        department: "",
      };
      break;
  }
  // 同步更新原始数据备份
  originalForm.value = { ...userForm.value };
};

// 初始化用户数据的函数
const initializeUserData = async () => {
  const userType = getUserType();
  const currentUser = authStore.getUser;
  
  // 先设置默认数据
  setDefaultUserData(userType);
  
  // 如果是教师且有用户信息，尝试获取真实数据
  if (userType === 'teacher' && currentUser && currentUser.id) {
    try {
      await fetchTeacherInfo(currentUser.id);
    } catch (error) {
      console.error('获取教师信息失败，使用默认数据:', error);
      // 如果API调用失败，保持默认数据不变
    }
  }
  
  // 如果是学生且有用户信息，尝试获取真实数据
  if (userType === 'student' && currentUser && currentUser.id) {
    try {
      await fetchStudentInfo(currentUser.id);
    } catch (error) {
      console.error('获取学生信息失败，使用默认数据:', error);
      // 如果API调用失败，保持默认数据不变
    }
  }
  
  // 如果是管理员，尝试获取真实数据
  if (userType === 'admin') {
    try {
      await fetchAdminInfo();
    } catch (error) {
      console.error('获取管理员信息失败，使用默认数据:', error);
      // 如果API调用失败，保持默认数据不变
    }
  }
};

// 监听路由变化
watch(() => route.path, () => {
  initializeUserData();
}, { immediate: false });

// 组件挂载时初始化数据
onMounted(() => {
  initializeUserData();
});

// 计算属性绑定表单
const form = computed({
  get: () => userForm.value,
  set: (value) => { userForm.value = value }
});

// 动态字段定义
const commonFields = [
  { key: "username", label: "用户名", icon: "el-icon-user" },
  { key: "real_name", label: "真实姓名", icon: "el-icon-s-custom" },
  { key: "email", label: "邮箱", icon: "el-icon-message", type: "email" },
  { key: "phone", label: "手机号", icon: "el-icon-phone", type: "tel" },
];

const studentFields = [
  { key: "student_number", label: "学号", icon: "el-icon-s-order" },
  { key: "grade", label: "年级", icon: "el-icon-s-marketing" },
  { key: "class_name", label: "班级", icon: "el-icon-school" },
];

const teacherFields = [
  { key: "employee_id", label: "职工号", icon: "el-icon-s-order" },
  { key: "department", label: "所属院系", icon: "el-icon-s-management" },
];

const adminFields = [
  { key: "admin_id", label: "管理员ID", icon: "el-icon-s-order" },
  { key: "manage_scope", label: "管理范围", icon: "el-icon-s-operation" },
];

const systemFields = [
  {
    key: "user_id",
    label: "用户ID",
    icon: "el-icon-s-check",
    locked: true,
    disabled: true,
  },
  {
    key: "user_type",
    label: "用户类型",
    icon: "el-icon-s-custom",
    locked: true,
    disabled: true,
  },
];

// 动态字段计算
const dynamicFields = computed(() => {
  const type = userForm.value.user_type;
  let typeFields = [];
  if (type === "student") {
    typeFields = studentFields;
  } else if (type === "teacher") {
    typeFields = teacherFields;
  } else if (type === "admin") {
    typeFields = adminFields;
  }

  return [
    ...typeFields,
    ...commonFields,
    ...systemFields,
  ];
});

// 编辑状态管理
const enableEdit = () => {
  isEditing.value = true;
};

const cancelChanges = () => {
  userForm.value = { ...originalForm.value };
  isEditing.value = false;
};

// 获取教师个人信息
const fetchTeacherInfo = async (userId) => {
  try {
    loading.value = true;
    console.log('正在获取教师信息，用户ID:', userId);
    const response = await teacherService.getTeacherInfo(userId);
    console.log('教师信息API响应:', response);
    
    if (response.data && response.data.success) {
      const teacherData = response.data.data;
      console.log('教师数据:', teacherData);
      
      // 更新教师表单数据
      userForm.value = {
        user_id: teacherData.userId?.toString() || "TCH2023001",
        username: teacherData.username || "zhangdm",
        real_name: teacherData.real_name || "张冬梅",
        email: teacherData.email || "zhangdm@school.com",
        phone: teacherData.phone || "13987654321",
        user_type: "teacher",
        employee_id: teacherData.employee_number || "T2023001",
        department: teacherData.department || "计算机科学与技术",
        // 其他角色字段置空
        student_number: "",
        grade: "",
        class_name: "",
        admin_id: "",
        manage_scope: "",
      };
      // 同步更新原始数据备份
      originalForm.value = { ...userForm.value };
      console.log('教师信息更新完成');
    } else {
      console.warn('API响应格式不正确:', response);
      ElMessage.warning('获取教师信息格式异常');
    }
  } catch (error) {
    console.error('获取教师信息失败:', error);
    ElMessage.error('获取教师信息失败，请重试');
  } finally {
    loading.value = false;
  }
};

// 获取学生个人信息
const fetchStudentInfo = async (userId) => {
  try {
    loading.value = true;
    console.log('正在获取学生信息，用户ID:', userId);
    
    // 第一步：通过userId获取学生基本信息
    const studentIdResponse = await studentService.getStudentIdByUserId(userId);
    console.log('学生ID信息API响应:', studentIdResponse);
    
    if (studentIdResponse.data && studentIdResponse.data.success) {
      const studentIdData = studentIdResponse.data.data;
      console.log('学生ID数据:', studentIdData);
      
      // 第二步：通过学号获取详细学生信息
      const studentDetailResponse = await studentService.getStudentByNumber(studentIdData.studentNumber);
      console.log('学生详细信息API响应:', studentDetailResponse);
      
      if (studentDetailResponse.data && studentDetailResponse.data.success) {
        const studentDetailData = studentDetailResponse.data.data;
        console.log('学生详细数据:', studentDetailData);
        
        // 更新学生表单数据
        userForm.value = {
          user_id: studentDetailData.userId?.toString() || studentIdData.userId?.toString() || "STD2023001",
          username: studentDetailData.user?.username || "zhangxm",
          real_name: studentDetailData.user?.realName || "张小明",
          email: studentDetailData.user?.email || "zhangxm@school.com",
          phone: studentDetailData.user?.phone || "13812345678",
          user_type: "student",
          student_number: studentDetailData.studentNumber || studentIdData.studentNumber || "2023001001",
          grade: studentDetailData.grade || studentIdData.grade || "大一",
          class_name: studentDetailData.className || studentIdData.className || "计算机2301",
          // 保存学生ID用于更新
          student_id: studentDetailData.studentId || studentIdData.studentId || 0,
          // 其他角色字段置空
          employee_id: "",
          department: "",
          admin_id: "",
          manage_scope: "",
        };
        
        // 同步更新原始数据备份
        originalForm.value = { ...userForm.value };
        console.log('学生信息更新完成');
      } else {
        // 如果获取详细信息失败，使用基本信息
        userForm.value = {
          user_id: studentIdData.userId?.toString() || "STD2023001",
          username: "zhangxm",
          real_name: "张小明",
          email: "zhangxm@school.com",
          phone: "13812345678",
          user_type: "student",
          student_number: studentIdData.studentNumber || "2023001001",
          grade: studentIdData.grade || "大一",
          class_name: studentIdData.className || "计算机2301",
          // 保存学生ID用于更新
          student_id: studentIdData.studentId || 0,
          // 其他角色字段置空
          employee_id: "",
          department: "",
          admin_id: "",
          manage_scope: "",
        };
        
        // 同步更新原始数据备份
        originalForm.value = { ...userForm.value };
        console.log('学生基本信息更新完成');
      }
    } else {
      // 如果API调用失败，使用默认数据
      userForm.value = {
        user_id: "STD2023001",
        username: "zhangxm",
        real_name: "张小明",
        email: "zhangxm@school.com",
        phone: "13812345678",
        user_type: "student",
        student_number: "2023001001",
        grade: "大一",
        class_name: "计算机2301",
        // 默认学生ID
        student_id: 0,
        // 其他角色字段置空
        employee_id: "",
        department: "",
        admin_id: "",
        manage_scope: "",
      };
      
      // 同步更新原始数据备份
      originalForm.value = { ...userForm.value };
      console.log('使用默认学生数据');
    }
  } catch (error) {
    console.error('获取学生信息失败:', error);
    ElMessage.error('获取学生信息失败，请重试');
    
    // 如果API调用失败，使用默认数据
    userForm.value = {
      user_id: "STD2023001",
      username: "zhangxm",
      real_name: "张小明",
      email: "zhangxm@school.com",
      phone: "13812345678",
      user_type: "student",
      student_number: "2023001001",
      grade: "大一",
      class_name: "计算机2301",
      // 默认学生ID
      student_id: 0,
      // 其他角色字段置空
      employee_id: "",
      department: "",
      admin_id: "",
      manage_scope: "",
    };
    
    // 同步更新原始数据备份
    originalForm.value = { ...userForm.value };
  } finally {
    loading.value = false;
  }
};

// 获取管理员个人信息
const fetchAdminInfo = async () => {
  try {
    loading.value = true;
    console.log('正在获取管理员信息');
    
    const response = await adminService.getAdminInfo();
    console.log('管理员信息API响应:', response);
    
    if (response.data && response.data.success) {
      const adminData = response.data.data;
      console.log('管理员数据:', adminData);
      
      // 更新管理员表单数据
      userForm.value = {
        user_id: adminData.userId?.toString() || "ADM2023001",
        username: adminData.user?.username || "admin",
        real_name: adminData.user?.realName || "飞科",
        email: adminData.user?.email || "admin@smartcampus.edu",
        phone: adminData.user?.phone || "13800000001",
        user_type: "admin",
        admin_id: adminData.admin?.adminNumber || "ADM001",
        manage_scope: adminData.admin?.position || "系统管理员",
        // 保存管理员详细信息用于更新
        admin_level: adminData.admin?.adminLevel || "super_admin",
        admin_department: adminData.admin?.department || "信息技术部",
        // 其他角色字段置空
        student_number: "",
        grade: "",
        class_name: "",
        employee_id: "",
        department: "",
      };
      
      // 同步更新原始数据备份
      originalForm.value = { ...userForm.value };
      console.log('管理员信息更新完成');
    } else {
      console.warn('API响应格式不正确:', response);
      ElMessage.warning('获取管理员信息格式异常');
    }
  } catch (error) {
    console.error('获取管理员信息失败:', error);
    ElMessage.error('获取管理员信息失败，请重试');
  } finally {
    loading.value = false;
  }
};

// 提交表单
const submitForm = async () => {
  try {
    loading.value = true;
    
    // 如果是学生角色，调用更新学生信息API
    if (userForm.value.user_type === 'student') {
      // 构建更新学生信息的请求数据
      const updateData = {
        studentId: userForm.value.student_id || 0,
        userId: parseInt(userForm.value.user_id) || 0,
        studentNumber: userForm.value.student_number,
        grade: userForm.value.grade,
        className: userForm.value.class_name,
        user: {
          userId: parseInt(userForm.value.user_id) || 0,
          username: userForm.value.username,
          realName: userForm.value.real_name,
          email: userForm.value.email,
          phone: userForm.value.phone,
          userType: "student",
          status: "active",
          createdAt: new Date().toISOString()
        }
      };
      
      console.log('准备更新学生信息:', updateData);
      
      // 调用更新学生信息API
      const response = await studentService.updateStudent(updateData);
      
      if (response.data && response.data.success) {
        ElMessage.success("学生信息更新成功！");
        // 更新成功后，重新获取最新信息
        if (authStore.getUser && authStore.getUser.id) {
          await fetchStudentInfo(authStore.getUser.id);
        }
      } else {
        ElMessage.error("更新失败：" + (response.data?.message || "未知错误"));
        return; // 更新失败，不关闭对话框
      }
    }
    
    // 如果是管理员角色，调用更新管理员信息API
    if (userForm.value.user_type === 'admin') {
      // 构建更新管理员信息的请求数据
      const updateData = {
        adminNumber: userForm.value.admin_id || "",
        adminName: userForm.value.real_name || "",
        adminLevel: userForm.value.admin_level || "",
        adminRole: userForm.value.manage_scope || "",
        adminEmail: userForm.value.email || "",
        adminPhone: userForm.value.phone || "",
        adminDepartment: userForm.value.admin_department || ""
      };
      
      console.log('准备更新管理员信息:', updateData);
      
      // 调用更新管理员信息API
      const response = await adminService.updateAdminInfo(updateData);
      
      if (response.data && response.data.success) {
        ElMessage.success("管理员信息更新成功！");
        // 更新成功后，重新获取最新信息
        await fetchAdminInfo();
      } else {
        ElMessage.error("更新失败：" + (response.data?.message || "未知错误"));
        return; // 更新失败，不关闭对话框
      }
    }
    
    // 关闭确认对话框
    centerDialogVisible.value = false;
    
    // 更新原始数据备份
    originalForm.value = { ...userForm.value };
    isEditing.value = false;
    
  } catch (error) {
    console.error('更新信息失败:', error);
    ElMessage.error("更新失败：" + (error.message || "网络错误"));
    return; // 更新失败，不关闭对话框
  } finally {
    loading.value = false;
  }
};
</script>
<style scoped lang="scss">
/* 样式部分保持不变 */
$primary-color: #b8d4f8;
$secondary-color: #6a7385;
$card-bg: #ffffff;
$form-bg: #f9fbfd;
$text-primary: #2c3e50;
$text-secondary: #6c757d;
$disabled-bg: #f8fafc;
$border-color: #e4e7ed;
$shadow-color: rgba(58, 143, 254, 0.15);
.form-actions {
  padding: 20px 30px 30px;
  border-top: 1px solid rgba(#e4e7ed, 0.5);
  text-align: center;

  .edit-actions {
    display: flex;
    justify-content: center;
    gap: 30px;
    flex-wrap: wrap;
  }

  .update-btn.cancel {
    background: white;
    border: 1px solid #e4e7ed;
    color: #6c757d;

    &:hover {
      background: #f5f7fa;
      transform: translateY(-2px);
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
    }
  }
}

.user-center-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
}

.background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.profile-card {
  background: $card-bg;
  border-radius: 20px;
  box-shadow: 0 15px 50px $shadow-color;
  overflow: hidden;
  position: relative;
  opacity: 0.85;
}

.card-header {
  background: linear-gradient(135deg, $primary-color 0%, $secondary-color 100%);
  padding: 25px 30px;
  display: flex;
  align-items: center;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 20px;
    background: linear-gradient(to bottom, rgba($secondary-color, 0.3), transparent);
  }
}

.avatar-wrapper {
  position: relative;
  margin-right: 25px;

  .avatar {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    border: 3px solid rgba(255, 255, 255, 0.5);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
    object-fit: cover;
  }

  .edit-overlay {
    position: absolute;
    bottom: 0;
    right: 0;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: white;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
    transition: all 0.3s;

    i {
      color: $primary-color;
      font-size: 18px;
    }

    &:hover {
      transform: scale(1.1);
      background: $primary-color;

      i {
        color: white;
      }
    }
  }
}

.header-info {
  color: white;

  h1 {
    font-size: 28px;
    font-weight: 700;
    margin-bottom: 10px;
    display: flex;
    align-items: center;

    .verification-icon {
      margin-left: 10px;
      width: 24px;
      height: 24px;
      background: white;
      border-radius: 50%;
      color: $primary-color;
      font-size: 16px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
  }

  .user-type {
    display: flex;
    gap: 10px;

    .tag {
      font-size: 14px;
      padding: 6px 15px;
      border-radius: 15px;
      background: rgba(255, 255, 255, 0.2);
      backdrop-filter: blur(5px);
      border: 1px solid rgba(255, 255, 255, 0.3);
    }

    .user-id {
      background: rgba(0, 0, 0, 0.2);
    }
  }
}

.form-scroll-container {
  max-height: 60vh;
  overflow-y: auto;
  padding: 25px 15px;
}

.form-section {
  background: $form-bg;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  border: 1px solid $border-color;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid lighten($border-color, 5%);

  h3 {
    font-size: 20px;
    color: $text-primary;
    font-weight: 600;
    margin: 0;
  }

  i {
    font-size: 24px;
    color: $primary-color;
    margin-right: 12px;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 22px;
}

.form-group {
  position: relative;

  .form-label {
    font-size: 15px;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 10px;
    display: flex;
    align-items: center;

    i {
      margin-right: 8px;
      font-size: 18px;
      color: $text-secondary;
    }
  }
}

:deep(.el-input) {
  .el-input__wrapper {
    border-radius: 12px;
    height: 50px;
    border: 1px solid $border-color;
    background: white;
    box-shadow: none !important;
    padding: 0 20px;
    transition: all 0.3s;

    &:hover {
      border-color: lighten($primary-color, 20%);
    }

    &.is-focus {
      border-color: $primary-color;
      box-shadow: 0 0 0 2px rgba($primary-color, 0.2) !important;
    }
  }

  .el-input__inner {
    font-size: 16px;
    color: $text-primary;
    height: 100%;
  }

  .el-input__suffix {
    right: 15px;
    color: $text-secondary;
    font-size: 18px;
  }

  &.is-disabled {
    .el-input__wrapper {
      background: $disabled-bg;
      border-color: lighten($border-color, 3%);
    }

    .el-input__inner {
      color: $text-secondary;
      -webkit-text-fill-color: $text-secondary;
    }
  }
}

.form-actions {
  padding: 20px 30px 30px;
  border-top: 1px solid rgba($border-color, 0.5);

  .update-btn {
    width: 100%;
    height: 56px;
    border-radius: 14px;
    font-size: 18px;
    font-weight: 500;
    background: linear-gradient(135deg, $primary-color 0%, $secondary-color 100%);
    border: none;
    box-shadow: 0 6px 20px rgba($primary-color, 0.3);
    transition: all 0.3s ease;

    i {
      font-size: 20px;
      margin-right: 10px;
    }

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 10px 25px rgba($primary-color, 0.4);
    }

    &:active {
      transform: translateY(1px);
      box-shadow: 0 4px 15px rgba($primary-color, 0.3);
    }
  }
}

/* 自定义滚动条样式 */
.form-scroll-container::-webkit-scrollbar {
  width: 8px;
}

.form-scroll-container::-webkit-scrollbar-track {
  background: rgba($border-color, 0.3);
  border-radius: 10px;
}

.form-scroll-container::-webkit-scrollbar-thumb {
  background: lighten($primary-color, 10%);
  border-radius: 10px;
}

.form-scroll-container::-webkit-scrollbar-thumb:hover {
  background: $primary-color;
}

:deep(.confirm-dialog) {
  border-radius: 20px;
  overflow: hidden;

  .el-dialog__header {
    padding: 0;
  }

  .el-dialog__body {
    padding: 0;
  }
}

.dialog-content {
  text-align: center;
  padding: 40px 30px 20px;

  .dialog-icon {
    font-size: 60px;
    margin-bottom: 20px;
    color: $primary-color;
  }

  .dialog-title {
    color: $text-primary;
    font-size: 24px;
    font-weight: 700;
    margin-bottom: 15px;
  }

  .dialog-message {
    color: $text-secondary;
    font-size: 16px;
    line-height: 1.6;
  }
}

.dialog-footer {
  display: flex;
  justify-content: center;
  padding: 20px 30px;
  gap: 25px;

  .el-button {
    height: 50px;
    width: 150px;
    font-size: 17px;
    font-weight: 500;
    border-radius: 12px;
    transition: all 0.3s;
  }

  .cancel-btn {
    background: white;
    border: 1px solid $border-color;
    color: $text-secondary;

    &:hover {
      background: #f5f7fa;
      transform: translateY(-2px);
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
    }
  }

  .confirm-btn {
    background: linear-gradient(135deg, $primary-color 0%, $secondary-color 100%);
    border: none;
    box-shadow: 0 4px 15px rgba($primary-color, 0.3);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba($primary-color, 0.4);
    }
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    text-align: center;
    padding: 25px 20px;

    .avatar-wrapper {
      margin-right: 0;
      margin-bottom: 20px;
    }
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    gap: 15px;

    .el-button {
      width: 130px;
    }
  }
}

@media (max-width: 480px) {
  .card-header {
    padding: 20px 15px;
  }

  .form-scroll-container {
    max-height: 50vh;
  }

  .form-section {
    padding: 15px;
  }

  .dialog-footer {
    flex-direction: column;
    align-items: center;
    gap: 15px;

    .el-button {
      width: 100%;
    }
  }
}
</style>