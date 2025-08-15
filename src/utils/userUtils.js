import { useAuthStore } from "@/store/authStore";

/**
 * 获取当前登录用户信息
 * @returns {Object} 用户信息对象
 */
export function getCurrentUser() {
  const authStore = useAuthStore();
  const user = authStore.getUser;
  
  if (user) {
    return {
      id: user.id || user.userId, // 兼容不同的用户ID字段
      name: user.name || user.userName || user.username || "未知用户",
      avatar: user.avatar || user.userAvatar || "https://tse3-mm.cn.bing.net/th/id/OIP-C.MyVTP6gOD1WSIDQ8CIV1qAHaHa?w=167&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3"
    };
  }
  
  // 如果没有用户信息，返回默认值
  return {
    id: 16,
    name: "未知用户",
    avatar: "https://tse3-mm.cn.bing.net/th/id/OIP-C.MyVTP6gOD1WSIDQ8CIV1qAHaHa?w=167&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3"
  };
}

/**
 * 检查用户是否已登录
 * @returns {boolean} 是否已登录
 */
export function isUserLoggedIn() {
  const authStore = useAuthStore();
  return !!authStore.getToken && !!authStore.getUser;
}

/**
 * 获取用户ID
 * @returns {number} 用户ID
 */
export function getCurrentUserId() {
  const user = getCurrentUser();
  return user.id;
}

/**
 * 获取用户名
 * @returns {string} 用户名
 */
export function getCurrentUserName() {
  const user = getCurrentUser();
  return user.name;
}

/**
 * 获取用户头像
 * @returns {string} 用户头像URL
 */
export function getCurrentUserAvatar() {
  const user = getCurrentUser();
  return user.avatar;
} 