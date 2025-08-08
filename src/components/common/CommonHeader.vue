<template>
  <el-header class="header">
    <div class="page-title">{{ pageTitle }}</div>
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <span class="el-dropdown-link">
          <el-avatar :size="30" :src="userAvatar || ''">{{ !userAvatar ? userType : '' }}</el-avatar>
          <span class="user-type" v-if="userType">{{ userType }}</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="userCenter">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import { defineProps } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/authStore';


const props = defineProps({
  pageTitle: {
    type: String,
    required: true,
  },
  userName: {
    type: String,
    default: '用户',
  },
  userAvatar: {
    type: String,
    default: '',
  },
  userType: {
    type: String,
    default: ''
  }
});

const router = useRouter();
const authStore = useAuthStore();

const handleCommand = (command) => {
  if (command === 'userCenter') {
    router.push('/user-center');
  } else if (command === 'logout') {
    authStore.clearToken();
    router.push('/login');
  }
};
</script>

<style scoped>
.header {
  background-color: #fff;
  color: #333;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding: 0 20px;
}

.page-title {
  font-size: 20px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-type {
  font-size: 14px;
}
</style>