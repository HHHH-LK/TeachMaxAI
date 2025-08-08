<template>
  <el-dropdown @command="handleCommand" class="user-dropdown" trigger="click">
    <div class="dropdown-trigger">
      <el-avatar :size="40" :src="avatarUrl" class="user-avatar" />
      <span class="user-info">
        <span class="user-name">{{ username }}</span>
        <span class="user-type">{{ userType }}</span>
      </span>
      <i class="el-icon-arrow-down"></i>
    </div>

    <template #dropdown>
      <el-dropdown-menu class="custom-dropdown-menu">
        <el-dropdown-item command="userCenter" class="dropdown-item">
          <i class="el-icon-user"></i>个人中心
        </el-dropdown-item>
        <el-dropdown-item command="logout" class="dropdown-item">
          <i class="el-icon-switch-button"></i>退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { authService, teacherService } from "@/services/api";
import { useAuthStore } from "@/store/authStore";
import { ElMessage } from "element-plus";

// 用户数据
const username = ref("student001");
const userType = ref("学生");
const avatarUrl = ref("https://randomuser.me/api/portraits/wosmen/41.jpg");

// 获取教师信息的函数
const fetchTeacherInfo = async (userId) => {
  try {
    const response = await teacherService.getTeacherInfo(userId);
    if (response.data && response.data.success) {
      const teacherData = response.data.data;
      username.value = teacherData.real_name || teacherData.username || "zhangdm";
      userType.value = "教师";
      avatarUrl.value = "https://tse2-mm.cn.bing.net/th/id/OIP-C.q_kfyYa8Hui3SiLYa-oqagHaGQ?w=221&h=187&c=7&r=0&o=5&dpr=1.5&pid=1.7";
    }
  } catch (error) {
    console.error('获取教师信息失败:', error);
    // 如果API调用失败，使用默认数据
    username.value = "zhangdm";
    userType.value = "教师";
    avatarUrl.value = "https://tse2-mm.cn.bing.net/th/id/OIP-C.q_kfyYa8Hui3SiLYa-oqagHaGQ?w=221&h=187&c=7&r=0&o=5&dpr=1.5&pid=1.7";
  }
};

const router = useRouter();
const authStore = useAuthStore();

const setName = async () => {
  const path = window.location.pathname;
  const currentUser = authStore.getUser;
  
  // 更精确的路由判断逻辑
  if (path.startsWith('/teacher')) {
    userType.value = "教师";
    if (currentUser && currentUser.id) {
      // 尝试获取真实教师信息
      await fetchTeacherInfo(currentUser.id);
    } else {
      // 使用默认数据
      username.value = "zhangdm";
      avatarUrl.value = "https://tse2-mm.cn.bing.net/th/id/OIP-C.q_kfyYa8Hui3SiLYa-oqagHaGQ?w=221&h=187&c=7&r=0&o=5&dpr=1.5&pid=1.7";
    }
  } else if (path.startsWith('/student')) {
    userType.value = "学生";
    username.value = "zhangxm";
    avatarUrl.value = "https://tse3-mm.cn.bing.net/th/id/OIP-C.MyVTP6gOD1WSIDQ8CIV1qAHaHa?w=167&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3";
  } else if (path.startsWith('/admin')) {
    userType.value = "管理员";
    username.value = "wangfk";
    avatarUrl.value = "https://tse4-mm.cn.bing.net/th/id/OIP-C._NeFXOQo9CFwgDT20CPNZgHaHa?w=160&h=180&c=7&r=0&o=5&dpr=1.5&pid=1.7";
  } else {
    // 默认情况
    userType.value = "学生";
    username.value = "zhangxm";
    avatarUrl.value = "https://tse3-mm.cn.bing.net/th/id/OIP-C.MyVTP6gOD1WSIDQ8CIV1qAHaHa?w=167&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3";
  }
}


// 处理下拉菜单命令
const handleCommand = (command) => {
  console.log(userType.value)
  switch (command) {
    case "userCenter":
      if(userType.value == "教师") router.push("/teacher/teacher-user-center");
      else if(userType.value == "学生") router.push("/student/student-user-center");
      else router.push("/admin/admin-user-center");
      break;
    case "logout":
      logout();
      break;
  }
};

// 退出登录逻辑
const logout = async () => {
  try {
    // 从 store 中获取用户信息
    const user = authStore.getUser;
    const userId = user?.id;
    console.log("用户ID:", userId);
    
    if (userId) {
      // 调用登出 API
      await authService.logout(userId);
      ElMessage.success("退出登录成功");
    }
    
    // 跳转到登录页面
    router.push("/login");
    console.log("用户退出登录");

    // 清除本地存储的用户信息和token
    authStore.clearToken();
  } catch (error) {
    console.error("退出登录失败:", error);
    ElMessage.error("退出登录失败，请重试");

    router.push("/login");
    // 即使API调用失败，也要清除本地存储并跳转
    authStore.clearToken();
  }
};

// 监听路由变化，重新设置用户信息
watch(() => router.currentRoute.value.path, async () => {
  await setName();
}, { immediate: false });

onMounted(async () => {
  await setName();
})
</script>

<style scoped lang="scss">
$primary-color: #3a8ffe;
$secondary-color: #235ee7;
$text-color: #2c3e50;
$light-bg: #f9fbfd;

.user-dropdown {
  height: 100px;
  width: 100%;
  //padding: 11px;
  z-index: 100;
  position: relative;


  .dropdown-trigger {
    // margin-right: 0;

    display: flex;
    align-items: center;
    cursor: pointer;
    height: 100%;
    position: absolute;
    right: 0;
    transition: all 0.3s ease;

    border-radius: 10px;

    &:hover {
      background: rgba($primary-color, 0.08);

      .user-avatar {
        transform: scale(1.05);
      }
    }

    .user-avatar {
      transition: all 0.3s ease;
      border: 2px solid white;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }

    .user-info {
      display: flex;
      flex-direction: column;
      margin: 0 10px;

      .user-name {
        font-weight: 600;
        font-size: 15px;
        color: $text-color;
      }

      .user-type {
        font-size: 12px;
        color: #7b8a9a;
        background: $light-bg;
        border-radius: 4px;
        padding: 2px 6px;
        margin-top: 3px;
        align-self: flex-start;
      }
    }

    .el-icon-arrow-down {
      color: $text-color;
      margin-left: 5px;
      font-size: 14px;
      transition: transform 0.3s;
    }
  }

  &:hover .el-icon-arrow-down {
    transform: translateY(2px);
  }
}

/* 与用户中心组件一致的弹出菜单样式 */
:deep(.custom-dropdown-menu) {
  border-radius: 12px !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15) !important;
  border: none !important;
  padding: 10px;
  width: 220px;
  background: white !important;

  .dropdown-item {
    padding: 0 15px;
    height: 46px;
    line-height: 46px;
    border-radius: 10px;
    margin: 6px 0;
    font-size: 15px;
    color: $text-color;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;

    &:hover {
      background: rgba($primary-color, 0.1) !important;
      color: $primary-color !important;
      transform: translateX(5px);
    }

    i {
      margin-right: 10px;
      font-size: 18px;
      width: 20px;
    }
  }

  .el-dropdown-menu__item:not(.is-disabled):focus {
    background: transparent !important;
  }

  .popper__arrow {
    display: none !important;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-dropdown .user-info {
    display: none;
  }

  .dropdown-trigger {
    padding: 0;
  }
}
</style>
