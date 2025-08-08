import apiClient from "@/utils/https.js";

/**
 * 师生对话SSE通信服务
 * 采用低耦合、易维护的设计模式
 */
class TeacherChatSSEService {
  constructor() {
    this.eventSources = new Map(); // 存储每个老师的SSE连接
    this.messageHandlers = new Map(); // 存储消息处理器
    this.reconnectAttempts = new Map(); // 存储重连次数
    this.maxReconnectAttempts = 3; // 最大重连次数
    this.reconnectDelay = 1000; // 重连延迟（毫秒）
  }

  /**
   * 连接到指定老师的SSE流
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {Object} handlers - 事件处理器
   * @param {Function} handlers.onMessage - 收到消息时的回调
   * @param {Function} handlers.onTeacherOnline - 老师上线时的回调
   * @param {Function} handlers.onTeacherOffline - 老师下线时的回调
   * @param {Function} handlers.onError - 错误处理回调
   * @param {Function} handlers.onReconnect - 重连时的回调
   */
  connectToTeacher(teacherId, studentId, handlers = {}) {
    // 如果已经连接，先断开
    this.disconnectFromTeacher(teacherId);

    // 在开发环境下使用模拟SSE服务器
    if (import.meta.env.DEV) {
      return this.connectToMockServer(teacherId, studentId, handlers);
    }

    const url = this.buildSSEUrl(teacherId, studentId);
    const eventSource = new EventSource(url);

    // 存储事件处理器
    this.messageHandlers.set(teacherId, handlers);

    // 设置事件监听器
    eventSource.onmessage = (event) => {
      this.handleMessage(teacherId, event);
    };

    eventSource.onopen = () => {
      console.log(`SSE连接已建立: 老师${teacherId}`);
      this.reconnectAttempts.set(teacherId, 0);
    };

    eventSource.onerror = (error) => {
      console.error(`SSE连接错误: 老师${teacherId}`, error);
      this.handleError(teacherId, error);
    };

    // 存储EventSource实例
    this.eventSources.set(teacherId, eventSource);

    return eventSource;
  }

  /**
   * 连接到模拟SSE服务器（开发环境）
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {Object} handlers - 事件处理器
   */
  connectToMockServer(teacherId, studentId, handlers = {}) {
    // 动态导入模拟服务器
    import('@/utils/mockSSEServer.js').then(({ default: mockSSEServer }) => {
      // 创建模拟的EventSource
      const mockEventSource = {
        onmessage: null,
        onopen: null,
        onerror: null,
        close: () => {
          mockSSEServer.removeConnection(teacherId);
        }
      };

      // 存储事件处理器
      this.messageHandlers.set(teacherId, handlers);

      // 模拟连接建立
      setTimeout(() => {
        if (mockEventSource.onopen) {
          mockEventSource.onopen();
        }
      }, 100);

      // 添加到模拟服务器
      mockSSEServer.addConnection(teacherId, studentId, mockEventSource);

      // 设置消息处理
      mockEventSource.onmessage = (event) => {
        this.handleMessage(teacherId, event);
      };

      // 存储模拟EventSource实例
      this.eventSources.set(teacherId, mockEventSource);

      console.log(`模拟SSE连接已建立: 老师${teacherId}`);
    });

    return null;
  }

  /**
   * 构建SSE URL
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @returns {string} SSE URL
   */
  buildSSEUrl(teacherId, studentId) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
    const params = new URLSearchParams({
      teacherId: teacherId.toString(),
      studentId: studentId.toString(),
      timestamp: Date.now().toString() // 防止缓存
    });
    return `${baseUrl}/api/teacher-chat/sse?${params.toString()}`;
  }

  /**
   * 处理收到的消息
   * @param {number} teacherId - 老师ID
   * @param {MessageEvent} event - 消息事件
   */
  handleMessage(teacherId, event) {
    try {
      const data = JSON.parse(event.data);
      const handlers = this.messageHandlers.get(teacherId);

      if (!handlers) return;

      switch (data.type) {
        case 'message':
          handlers.onMessage && handlers.onMessage(data);
          break;
        case 'teacher_online':
          handlers.onTeacherOnline && handlers.onTeacherOnline(data);
          break;
        case 'teacher_offline':
          handlers.onTeacherOffline && handlers.onTeacherOffline(data);
          break;
        case 'typing':
          handlers.onTyping && handlers.onTyping(data);
          break;
        case 'read_receipt':
          handlers.onReadReceipt && handlers.onReadReceipt(data);
          break;
        default:
          console.warn(`未知的消息类型: ${data.type}`);
      }
    } catch (error) {
      console.error('解析SSE消息失败:', error);
    }
  }

  /**
   * 处理连接错误
   * @param {number} teacherId - 老师ID
   * @param {Event} error - 错误事件
   */
  handleError(teacherId, error) {
    const handlers = this.messageHandlers.get(teacherId);
    const currentAttempts = this.reconnectAttempts.get(teacherId) || 0;

    if (handlers && handlers.onError) {
      handlers.onError(error);
    }

    // 尝试重连
    if (currentAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts.set(teacherId, currentAttempts + 1);
      
      setTimeout(() => {
        console.log(`尝试重连: 老师${teacherId}, 第${currentAttempts + 1}次`);
        this.reconnect(teacherId);
      }, this.reconnectDelay * (currentAttempts + 1));
    } else {
      console.error(`SSE连接失败，已达到最大重连次数: 老师${teacherId}`);
      this.disconnectFromTeacher(teacherId);
    }
  }

  /**
   * 重连到指定老师
   * @param {number} teacherId - 老师ID
   */
  reconnect(teacherId) {
    const handlers = this.messageHandlers.get(teacherId);
    if (handlers && handlers.onReconnect) {
      handlers.onReconnect();
    }
    
    // 重新连接
    const eventSource = this.eventSources.get(teacherId);
    if (eventSource) {
      eventSource.close();
      this.eventSources.delete(teacherId);
    }
  }

  /**
   * 断开与指定老师的SSE连接
   * @param {number} teacherId - 老师ID
   */
  disconnectFromTeacher(teacherId) {
    const eventSource = this.eventSources.get(teacherId);
    if (eventSource) {
      eventSource.close();
      this.eventSources.delete(teacherId);
    }
    
    this.messageHandlers.delete(teacherId);
    this.reconnectAttempts.delete(teacherId);
    console.log(`SSE连接已断开: 老师${teacherId}`);
  }

  /**
   * 断开所有SSE连接
   */
  disconnectAll() {
    this.eventSources.forEach((eventSource, teacherId) => {
      eventSource.close();
    });
    
    this.eventSources.clear();
    this.messageHandlers.clear();
    this.reconnectAttempts.clear();
    console.log('所有SSE连接已断开');
  }

  /**
   * 发送消息到老师
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {string} message - 消息内容
   * @param {Object} options - 额外选项
   * @returns {Promise} API响应
   */
  async sendMessage(teacherId, studentId, message, options = {}) {
    // 在开发环境下使用模拟服务器
    if (import.meta.env.DEV) {
      return this.sendMessageToMockServer(teacherId, studentId, message, options);
    }

    try {
      const response = await apiClient.post('/api/teacher-chat/send-message', {
        teacherId,
        studentId,
        message,
        ...options
      });
      return response.data;
    } catch (error) {
      console.error('发送消息失败:', error);
      throw error;
    }
  }

  /**
   * 发送消息到模拟服务器（开发环境）
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {string} message - 消息内容
   * @param {Object} options - 额外选项
   * @returns {Promise} 模拟响应
   */
  async sendMessageToMockServer(teacherId, studentId, message, options = {}) {
    try {
      const { default: mockSSEServer } = await import('@/utils/mockSSEServer.js');
      
      // 处理学生消息
      mockSSEServer.handleStudentMessage(teacherId, studentId, message);
      
      // 返回模拟响应
      return {
        success: true,
        messageId: Date.now().toString(),
        timestamp: Date.now()
      };
    } catch (error) {
      console.error('发送消息到模拟服务器失败:', error);
      throw error;
    }
  }

  /**
   * 获取与指定老师的聊天历史
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {Object} params - 查询参数
   * @returns {Promise} API响应
   */
  async getChatHistory(teacherId, studentId, params = {}) {
    try {
      const response = await apiClient.get('/api/teacher-chat/history', {
        params: {
          teacherId,
          studentId,
          ...params
        }
      });
      return response.data;
    } catch (error) {
      console.error('获取聊天历史失败:', error);
      throw error;
    }
  }

  /**
   * 标记消息为已读
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {Array} messageIds - 消息ID数组
   * @returns {Promise} API响应
   */
  async markMessagesAsRead(teacherId, studentId, messageIds) {
    try {
      const response = await apiClient.post('/api/teacher-chat/mark-read', {
        teacherId,
        studentId,
        messageIds
      });
      return response.data;
    } catch (error) {
      console.error('标记消息已读失败:', error);
      throw error;
    }
  }

  /**
   * 获取在线老师列表
   * @returns {Promise} API响应
   */
  async getOnlineTeachers() {
    try {
      const response = await apiClient.get('/api/teacher-chat/online-teachers');
      return response.data;
    } catch (error) {
      console.error('获取在线老师列表失败:', error);
      throw error;
    }
  }

  /**
   * 检查连接状态
   * @param {number} teacherId - 老师ID
   * @returns {boolean} 是否已连接
   */
  isConnected(teacherId) {
    return this.eventSources.has(teacherId);
  }

  /**
   * 获取所有已连接的老师ID
   * @returns {Array} 老师ID数组
   */
  getConnectedTeachers() {
    return Array.from(this.eventSources.keys());
  }
}

// 创建单例实例
const teacherChatSSEService = new TeacherChatSSEService();

// 在页面卸载时清理所有连接
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    teacherChatSSEService.disconnectAll();
  });
}

export default teacherChatSSEService; 