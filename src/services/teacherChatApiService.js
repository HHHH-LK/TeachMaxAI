import { useAuthStore } from '@/store/authStore.js';

/**
 * 教师聊天API服务
 * 统一管理教师端聊天相关的API调用
 */
class TeacherChatApiService {
  constructor() {
    this.baseURL = 'http://localhost:8108';
  }

  /**
   * 获取认证token
   * @returns {string} 认证token
   */
  getAuthToken() {
    const authStore = useAuthStore();
    return authStore.getToken;
  }

  /**
   * 教师向学生发送消息
   * @param {number} studentId - 学生ID（接收者）
   * @param {string} content - 消息内容
   * @param {string} messageType - 消息类型，默认为'text'
   * @param {string} fileUrl - 文件URL，可选
   * @param {number} conversationId - 会话ID，可选
   * @returns {Promise<Object>} API响应
   */
  async sendMessage(studentId, content, messageType = 'text', fileUrl = null, conversationId = null) {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      // 验证参数
      if (!studentId) {
        throw new Error('学生ID不能为空');
      }

      // 获取当前用户信息
      const authStore = useAuthStore();
      const currentUser = authStore.getUser;
      
      // 验证用户信息
      if (!currentUser || !currentUser.id) {
        throw new Error('用户信息不完整，请重新登录');
      }

      // 验证不能给自己发消息（考虑类型转换）
      const currentUserId = String(currentUser.id);
      const studentIdStr = String(studentId);
      
      console.log('用户ID比较:', {
        当前用户ID: currentUserId,
        学生ID: studentIdStr,
        是否相同: currentUserId === studentIdStr
      });
      
      if (currentUserId === studentIdStr) {
        throw new Error('不能给自己发送消息');
      }

      // 如果没有提供会话ID，使用学生用户ID作为会话ID
      if (!conversationId) {
        conversationId = Number(studentId);
        console.log('使用学生用户ID作为会话ID:', conversationId);
      }

      const requestData = {
        conversationId: conversationId,
        content: content,
        messageType: messageType,
        fileUrl: fileUrl,
        useId: studentId // 接收者ID（学生ID）
      };

      console.log('发送消息请求:', requestData);
      console.log('当前用户ID:', Number(currentUser.id));
      console.log('接收者用户ID (useId):', studentId);
      console.log('当前用户信息:', currentUser);
      console.log('当前用户ID:', currentUser?.id || currentUser?.userId);
      
      // 详细调试信息
      console.log('=== 详细调试信息 ===');
      console.log('请求数据详情:', {
        conversationId: conversationId,
        content: content,
        messageType: messageType,
        fileUrl: fileUrl,
        useId: studentId
      });
      console.log('用户信息详情:', {
        id: currentUser?.id,
        id类型: typeof currentUser?.id,
        username: currentUser?.username,
        role: currentUser?.role
      });
      
      // 添加更多调试信息
      console.log('=== 请求调试信息 ===');
      console.log('请求URL:', `${this.baseURL}/studentteacher/chat/sentChatMemory`);
      console.log('请求方法:', 'POST');
      console.log('请求头:', {
        'Content-Type': 'application/json',
        'token': token ? '存在' : '不存在'
      });
      console.log('请求体JSON:', JSON.stringify(requestData, null, 2));

      const response = await fetch(`${this.baseURL}/studentteacher/chat/sentChatMemory`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'token': token
        },
        body: JSON.stringify(requestData)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      console.log('发送消息响应:', result);
      return result;
    } catch (error) {
      console.error('发送消息失败:', error);
      throw error;
    }
  }
}

// 创建单例实例
const teacherChatApiService = new TeacherChatApiService();

export default teacherChatApiService; 