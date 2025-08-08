<template>
  <div class="admin-dashboard">
    <CustomNavbar
        :items="navbarItems"
        @item-click="handleNavbarItemClick"
    />

    <div class="main-content">
<!--      <CommonHeader :page-title="currentPageTitle" user-name="管理员" :user-avatar="adminAvatar" user-type="管理员"/>-->
      <head-bar />
      <div class="content-area">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, watch} from 'vue';
import {useRouter, useRoute} from 'vue-router';
import adminAvatar from '@/assets/admin-avatar.png';
import CustomNavbar from '@/components/common/Sider.vue';
import CommonHeader from '@/components/common/CommonHeader.vue';
import HeadBar from '@/components/common/HeadBar.vue';

const router = useRouter();
const route = useRoute();

// 适配新导航栏组件的数据结构
const navbarItems = [
  {id: 'dashboard', label: '大屏概览', icon: 'tv-outline', route: '/admin/dashboard'},
  {id: 'user-management', label: '用户管理', icon: 'people-outline', route: '/admin/user-management'},
  {id: 'resource-management', label: '教案资源管理', icon: 'library-outline', route: '/admin/resource-management'},
  {id: 'exam-review', label: '试卷审核与发布', icon: 'document-text-outline', route: '/admin/exam-review'},
  {id: 'community-admin', label: '社区交流中心', icon: 'people-outline', route: '/admin/community-admin'}
];

const activeMenu = ref('');

const currentPageTitle = computed(() => {
  const findTitle = (items, route) => {
    for (const item of items) {
      if (item.route === route) {
        return item.label;
      }
    }
    return '';
  };
  return findTitle(navbarItems, route.path);
});

const handleNavbarItemClick = (item) => {
  if (item.route) {
    router.push(item.route);
  }
};

watch(
    () => route.path,
    (newPath) => {
      const findActiveMenu = (items, path) => {
        for (const item of items) {
          if (item.route === path) {
            return item.id;
          }
        }
        return '';
      };
      activeMenu.value = findActiveMenu(navbarItems, newPath);
    },
    {immediate: true}
);

</script>

<style scoped>
.admin-dashboard {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background-color: #f5f7fa;
}

.main-content {
  margin-left: 5em;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-area {
  flex-grow: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 确保内容区域在小屏幕上正确显示 */
@media only screen and (max-width: 1024px) {
  .main-content {
    margin-left: 16em;
  }

  .admin-dashboard:hover .main-content {
    margin-left: 16em;
  }
}

@media only screen and (max-width: 756px) {
  .main-content {
    margin-left: 16em;
  }

  .admin-dashboard:hover .main-content {
    margin-left: 16em;
  }
}
</style>