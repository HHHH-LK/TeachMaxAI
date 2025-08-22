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
import ExamReview from "@/views/admin/ExamReview.vue";
import CourseAssignment from "@/views/admin/CourseAssignment.vue";
import Test from "@/views/tower/Fight.vue"
import Game from "@/views/student/game/Game.vue"
import TowerTransition from "@/views/student/game/animation/tower.vue"
import { useAuthStore } from '@/store/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
      meta: { requiresAuth: false } // 不需要登录
    },
    {
      path: '/login',
      name: 'login',
      component: LoginDynamicView,
      meta: { requiresAuth: false } // 登录页不需要登录
    },
    {
      path: '/teacher',
      name: 'teacher',
      component: TeacherView,
      redirect: '/teacher/course-design',
      meta: { requiresAuth: true }, // 教师路由组需要登录
      children: [
        {
          path: 'course-design',
          name: 'TeacherCourseDesign',
          component: () => import('@/views/teacher/CourseDesignSection.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'analysis',
          name: 'TeacherAnalysis',
          component: () => import('@/views/teacher/AnalysisSection.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'student-management',
          name: 'TeacherStudentManagement',
          component: () => import('@/views/teacher/StudentManagement.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'course-center/:courseId',
          name: 'TeacherCourseCenter',
          component: () => import('@/views/teacher/teacherCourse/CourseCenter.vue'),
          props: true,
          meta: { requiresAuth: true }
        },
        {
          path: 'community',
          name: 'TeacherCommunity',
          component: CommunityTeacher,
          meta: { requiresAuth: true }
        },
        {
          path: 'student-chat',
          name: 'TeacherStudentChat',
          component: StudentChat,
          meta: { requiresAuth: true }
        },
        {
          path: 'teacher-user-center',
          name: 'teacher-user-center',
          component: Center,
          meta: { requiresAuth: true }
        }
      ],
    },

    {
      path: '/student',
      name: 'student',
      component: StudentView,
      redirect: '/student/course-selection',
      meta: { requiresAuth: true }, // 学生路由组需要登录
      children: [
        {
          path: 'community-student',
          name: 'CommunityStudent',
          component: CommunityStudent,
          meta: { requiresAuth: true }
        },
        {
          path: 'error-analysis',
          name: 'ErrorAnalysis',
          component: ErrorAnalysisView,
          meta: { requiresAuth: true }
        },
        {
          path: 'knowledgeManagement',
          name: 'KnowledgePointManagement',
          component: KnowledgePointManagementView,
          meta: { requiresAuth: true }
        },
        {
          path: 'course-center/:id',
          name: 'CourseCenter',
          component: () => import('@/views/student/knowledge-base/CourseCenter.vue'),
          props: true,
          meta: { requiresAuth: true }
        },
        {
          path: 'teacher-chat',
          name: 'TeacherChat',
          component: TeacherChat,
          meta: { requiresAuth: true }
        },
        {
          path: 'course-selection',
          name: 'CourseSelection',
          component: CourseSelectionView,
          meta: { requiresAuth: true }
        },
        {
          path: 'student-user-center',
          name: 'student-user-center',
          component: Center,
          meta: { requiresAuth: true }
        },
        {
          path: 'game-entry',
          name: 'GameEntry',
          component: () => import('@/views/student/game/animation/Entry.vue'),
          meta: { requiresAuth: true }
        }
      ]
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminView,
      redirect: '/admin/dashboard',
      meta: { requiresAuth: true }, // 管理员路由组需要登录
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: Dashboard,
          meta: { requiresAuth: true }
        },
        {
          path: 'user-management',
          name: 'user-management',
          component: UserManagement,
          meta: { requiresAuth: true }
        },
        {
          path: 'resource-management',
          name: 'resource-management',
          component: ResourceManagement,
          meta: { requiresAuth: true }
        },
        {
          path: 'exam-review',
          name: 'exam-review',
          component: ExamReview,
          meta: { requiresAuth: true }
        },
        {
          path: 'community-admin',
          name: 'community-admin',
          component: () => import('@/views/admin/CommunityAdmin.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'course-assignment',
          name: 'course-assignment',
          component: CourseAssignment,
          meta: { requiresAuth: true }
        },
        {
          path: 'admin-user-center',
          name: 'admin-user-center',
          component: Center,
          meta: { requiresAuth: true }
        }
      ]
    },
    {
      path: "/test",
      name: "test",
      component:  Test,
      meta: { requiresAuth: true } // 需要登录
    },
    {
      path: '/student/game',
      name: 'StudentGame',
      component: () => import('@/views/student/game/Game.vue'),
      meta: { requiresAuth: true } // 需要登录
    },
    {
      path: '/game',
      name: 'game',
      component: Game,
      meta: { requiresAuth: true } // 需要登录
    },
    {
      path: '/tower-transition',
      name: 'TowerTransition',
      component: TowerTransition,
      props: true,
      meta: { requiresAuth: true } // 需要登录
    },
    {
      path: '/chat',
      name: 'chat',
      component: ChatView,
      meta: { requiresAuth: true } // 需要登录
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const isAuthenticated = !!authStore.token; // 确保是布尔值（有token则为true）

  // 判断当前路由是否需要登录
  if (to.meta.requiresAuth) {
    // 需要登录：检查是否已登录
    if (isAuthenticated) {
      next(); // 已登录，正常跳转
    } else {
      // 未登录，跳转到登录页，并记录目标路径（登录后可跳转回来）
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      });
    }
  } else {
    // 不需要登录的路由（如登录页），直接放行
    next();
  }
});

export default router
