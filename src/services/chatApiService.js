import { useAuthStore } from '@/store/authStore.js';

/**
 * 聊天API服务
 * 统一管理聊天相关的API调用
 */
class ChatApiService {
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
   * 发送消息到后端
   * @param {number} teacherId - 教师ID（接收者）
   * @param {string} content - 消息内容
   * @param {string} messageType - 消息类型，默认为'text'
   * @param {string} fileUrl - 文件URL，可选
   * @param {number} conversationId - 会话ID，可选
   * @returns {Promise<Object>} API响应
   */
  async sendMessage(teacherId, content, messageType = 'text', fileUrl = null, conversationId = null) {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      // 验证参数
      if (!teacherId) {
        throw new Error('教师ID不能为空');
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
      const teacherIdStr = String(teacherId);
      
      console.log('用户ID比较:', {
        当前用户ID: currentUserId,
        教师ID: teacherIdStr,
        是否相同: currentUserId === teacherIdStr
      });
      
      if (currentUserId === teacherIdStr) {
        throw new Error('不能给自己发送消息');
      }

      // 如果没有提供会话ID，使用学生用户ID作为会话ID
      if (!conversationId) {
        conversationId = Number(currentUser.id);
        console.log('使用学生用户ID作为会话ID:', conversationId);
      }

      const requestData = {
        conversationId: conversationId,
        content: content,
        messageType: messageType,
        fileUrl: fileUrl,
        useId: teacherId // 接收者用户ID（教师ID）
      };

      console.log('发送消息请求:', requestData);
      console.log('当前用户ID:', Number(currentUser.id));
      console.log('接收者用户ID (useId):', teacherId);
      console.log('当前用户信息:', currentUser);
      console.log('当前用户ID:', currentUser?.id || currentUser?.userId);
      
      // 详细调试信息
      console.log('=== 详细调试信息 ===');
      console.log('请求数据详情:', {
        conversationId: conversationId,
        content: content,
        messageType: messageType,
        fileUrl: fileUrl,
        useId: teacherId
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

  /**
   * 获取聊天历史记录
   * @param {number} teacherId - 教师ID
   * @param {number} conversationId - 会话ID
   * @param {Object} params - 查询参数
   * @returns {Promise<Object>} API响应
   */
  async getChatHistory(teacherId, conversationId, params = {}) {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      const queryParams = new URLSearchParams({
        teacherId: teacherId.toString(),
        conversationId: conversationId.toString(),
        ...params
      });

      const response = await fetch(`${this.baseURL}/api/chat/history?${queryParams}`, {
        method: 'GET',
        headers: {
          'token': token
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      return result;
    } catch (error) {
      console.error('获取聊天历史失败:', error);
      throw error;
    }
  }

  /**
   * 创建新的会话
   * @param {number} teacherId - 教师ID
   * @returns {Promise<Object>} API响应
   */
  async createConversation(teacherId) {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      const response = await fetch(`${this.baseURL}/api/chat/conversation`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'token': token
        },
        body: JSON.stringify({
          teacherId: teacherId
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      return result;
    } catch (error) {
      console.error('创建会话失败:', error);
      throw error;
    }
  }

  /**
   * 获取会话列表
   * @returns {Promise<Object>} API响应
   */
  async getConversations() {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      const response = await fetch(`${this.baseURL}/api/chat/conversations`, {
        method: 'GET',
        headers: {
          'token': token
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      return result;
    } catch (error) {
      console.error('获取会话列表失败:', error);
      throw error;
    }
  }

  /**
   * 标记消息为已读
   * @param {number} conversationId - 会话ID
   * @param {Array<number>} messageIds - 消息ID数组
   * @returns {Promise<Object>} API响应
   */
  async markMessagesAsRead(conversationId, messageIds) {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      const response = await fetch(`${this.baseURL}/api/chat/mark-read`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'token': token
        },
        body: JSON.stringify({
          conversationId: conversationId,
          messageIds: messageIds
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      return result;
    } catch (error) {
      console.error('标记消息已读失败:', error);
      throw error;
    }
  }

  /**
   * 删除消息
   * @param {number} messageId - 消息ID
   * @returns {Promise<Object>} API响应
   */
  async deleteMessage(messageId) {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      const response = await fetch(`${this.baseURL}/api/chat/message/${messageId}`, {
        method: 'DELETE',
        headers: {
          'token': token
        }
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      return result;
    } catch (error) {
      console.error('删除消息失败:', error);
      throw error;
    }
  }

  /**
   * 上传文件
   * @param {File} file - 要上传的文件
   * @returns {Promise<Object>} API响应
   */
  async uploadFile(file) {
    try {
      const token = this.getAuthToken();
      if (!token) {
        throw new Error('未找到认证token，请先登录');
      }

      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`${this.baseURL}/api/chat/upload`, {
        method: 'POST',
        headers: {
          'token': token
        },
        body: formData
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      return result;
    } catch (error) {
      console.error('上传文件失败:', error);
      throw error;
    }
  }
}

// 创建单例实例
const chatApiService = new ChatApiService();

export default chatApiService; 