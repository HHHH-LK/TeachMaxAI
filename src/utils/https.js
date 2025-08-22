import axios from 'axios';
import { useAuthStore } from "@/store/authStore.js";
import {ElMessage} from "element-plus";

const apiClient = axios.create({
  baseURL: 'http://localhost:8108',
  timeout: 3200000,
//   headers: {
//     'Content-Type': 'application/json'
//   }
});

// 请求拦截器：除了登录和注册外，都带上 token
apiClient.interceptors.request.use(config => {
  const authStore = useAuthStore();
  const token = authStore.getToken;

  // 判断当前请求是否是登录或注册接口（根据实际 URL 路径修改）
  const isPublicRoute = ['/login', '/register'].includes(config.url);

  // if (token && !isPublicRoute) {
  //   config.headers['Authorization'] = `Bearer ${token}`;
  // }

  if (token && !isPublicRoute) {
    config.headers['token'] = token;
    // delete config.headers['Authorization'];
  } else {
    console.error('token!')
  }

  return config;
}, error => {
  ElMessage.error('token error!')
  return Promise.reject(error);
});

// 响应拦截器
apiClient.interceptors.response.use(
    // 成功响应直接返回数据
    response => response,

    // 错误响应处理（聚焦401、403等状态码）
    error => {
      const authStore = useAuthStore();
      const status = error.response?.status;

      switch (status) {
        case 401:
          ElMessage.error('身份验证失败，请重新登录');
          authStore.clearToken();
          router.push('/login');
          break;
        case 403:
          // ElMessage.error('没有操作权限');
          console.error('没有操作权限')
          break;
        case 500:
          // ElMessage.error('服务器内部错误，请稍后重试');
          console.error('服务器内部错误，请稍后重试')
          break;
        default:
          // 其他状态码统一提示
          // ElMessage.error(`请求失败 [${status || '未知错误'}]`);
          console.error(`请求失败 [${status || '未知错误'}]`)
      }

      // 抛出错误供业务层处理
      return Promise.reject(error);
    }
);

export default apiClient;