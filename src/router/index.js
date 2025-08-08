import { createRouter, createWebHistory } from 'vue-router'
import TeacherView from '../views/TeacherView.vue'
import StudentView from '../views/StudentView.vue'
import ErrorAnalysisView from '../views/student/ErrorAnalysisView.vue';
import ChatView from "@/views/chatView.vue";
import AdminView from '../views/AdminView.vue';
import UserManagement from '../views/admin/UserManagement.vue';
import ResourceManagement from '../views/admin/ResourceManagement.vue';
import Dashboard from '../views/admin/Dashboard.vue';
import KnowledgePointManagementView from '../views/student/KnowledgePointManagementView.vue';
import CourseSelectionView from '../views/student/CourseSelectionView.vue'
import LoginDynamicView from '@/views/Login/LoginDynamicView.vue'
import CommunityStudent from '../views/student/CommunityStudent.vue'
import CommunityTeacher from '../views/teacher/CommunityTeacher.vue'
import StudentChat from '../views/teacher/StudentChat.vue'
import ChatBackground from '@/components/student/chat/ChatBackground.vue';
import TabChat from '@/components/student/chat/TabChat.vue';
import CenterBackground from '@/components/student/center/CenterBackground.vue';
import Center from '@/views/Center.vue';
import TeacherChat from "@/views/student/TeacherChat.vue";
import TestView from "@/views/teacher/teacherCourse/CourseCenter.vue";
import ExamReview from "@/views/admin/ExamReview.vue";
import Test from "@/views/Test.vue"
// import { useAuthStore } from '@/store/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/chat',
      name: 'chat',
      component: TestView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginDynamicView,
    },
    {
      path: '/teacher',
      name: 'teacher',
      component: TeacherView,
      redirect: '/teacher/course-design',
      children: [
        {
          path: 'course-design',
          name: 'TeacherCourseDesign',
          component: () => import('@/views/teacher/CourseDesignSection.vue'),
        },
        {
          path: 'analysis',
          name: 'TeacherAnalysis',
          component: () => import('@/views/teacher/AnalysisSection.vue'),
        },
        {
          path: 'student-management',
          name: 'TeacherStudentManagement',
          component: () => import('@/views/teacher/StudentManagement.vue'),
        },
        {
          path: 'course-center/:courseId',
          name: 'TeacherCourseCenter',
          component: () => import('@/views/teacher/teacherCourse/CourseCenter.vue'),
          props: true
        },
        {
          path: 'community',
          name: 'TeacherCommunity',
          component: CommunityTeacher
        },
        {
          path: 'student-chat',
          name: 'TeacherStudentChat',
          component: StudentChat
        },
        {
          path: 'teacher-user-center',
          name: 'teacher-user-center',
          component: Center
        }
      ],
    },

    {
      path: '/student',
      name: 'student',
      component: StudentView,
      redirect: '/student/course-selection',
      children: [
        {
          path: 'community-student',
          name: 'CommunityStudent',
          component: CommunityStudent
        },
        {
          path: 'error-analysis',
          name: 'ErrorAnalysis',
          component: ErrorAnalysisView
        },
        {
          path: 'knowledgeManagement',
          name: 'KnowledgePointManagement',
          component: KnowledgePointManagementView
        },
        {
          path: 'course-center/:id',
          name: 'CourseCenter',
          component: () => import('@/views/student/knowledge-base/CourseCenter.vue'),
          props: true
        },
        {
          path: 'teacher-chat',
          name: 'TeacherChat',
          component: TeacherChat
        },
        {
          path: 'course-selection',
          name: 'CourseSelection',
          component: CourseSelectionView
        },
        {
          path: 'student-user-center',
          name: 'student-user-center',
          component: Center
        }
      ]
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminView,
      redirect: '/admin/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: Dashboard
        },
        {
          path: 'user-management',
          name: 'user-management',
          component: UserManagement
        },
        {
          path: 'resource-management',
          name: 'resource-management',
          component: ResourceManagement
        },
        {
          path: 'exam-review',
          name: 'exam-review',
          component: ExamReview
        },
        {
          path: 'community-admin',
          name: 'community-admin',
          component: () => import('@/views/admin/CommunityAdmin.vue')
        },
        {
          path: 'admin-user-center',
          name: 'admin-user-center',
          component: Center
        }
      ]
    },
    {
      path: "/test",
      name: "test",
      component: Test
    },
  ]
})

// router.beforeEach((to, from, next) => {
//   const authStore = useAuthStore();
//   const isAuthenticated = authStore.token; // 检查是否存在token
//
//   // 假设需要认证的路由
//   const authRequiredRoutes = ['/student', '/teacher'];
//
//   if (authRequiredRoutes.includes(to.path) && !isAuthenticated) {
//     next('/login'); // 如果未认证且访问需要认证的路由，则重定向到登录页
//   } else {
//     next(); // 否则正常跳转
//   }
// });

export default router
