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
// apiClient.interceptors.response.use(
//     response => {
//       // 成功响应处理
//       if (response.data.code === 200) {
//         return response.data;
//       }
//       // 业务逻辑错误处理
//       return Promise.reject({
//         isBizError: true,
//         message: response.data.message || '业务逻辑错误',
//         code: response.data.code,
//         response
//       });
//     },
//     error => {
//       // 统一错误处理
//       const errorInfo = {
//         isNetworkError: !error.response,
//         isTimeout: error.code === 'ECONNABORTED',
//         status: error.response?.status,
//         message: ''
//       };
//
//       if (error.response) {
//         switch (error.response.status) {
//           case 401:
//             errorInfo.message = '身份验证失败，请重新登录';
//             router.push('/login');
//             break;
//           case 403:
//             errorInfo.message = '没有操作权限';
//             break;
//           case 500:
//             errorInfo.message = '服务器内部错误';
//             break;
//           default:
//             errorInfo.message = `请求失败 [${error.response.status}]`;
//         }
//       } else if (error.isTimeout) {
//         errorInfo.message = '请求超时，请检查网络连接';
//       } else {
//         errorInfo.message = '网络连接异常，请检查网络';
//       }
//
//       // 返回统一错误格式
//       return Promise.reject({
//         ...errorInfo,
//         rawError: error,
//         timestamp: new Date().toISOString()
//       });
//     }
//   );

export default apiClient;