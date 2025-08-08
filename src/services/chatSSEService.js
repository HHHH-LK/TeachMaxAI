import { useAuthStore } from "@/store/authStore.js";

/**
 * 通用聊天SSE服务
 * 使用fetch API建立SSE连接，支持认证token
 */
class ChatSSEService {
  constructor() {
    this.controllers = new Map(); // 存储AbortController实例
    this.messageHandlers = new Map(); // 存储消息处理器
    this.reconnectAttempts = new Map(); // 存储重连次数
    this.maxReconnectAttempts = 3; // 最大重连次数
    this.reconnectDelay = 1000; // 重连延迟（毫秒）
    this.baseURL = 'http://localhost:8108'; // 后端API基础URL
  }

  /**
   * 建立SSE连接
   * @param {string} userId - 用户ID
   * @param {Object} handlers - 事件处理器
   * @param {Function} handlers.onConnected - 连接成功时的回调
   * @param {Function} handlers.onAiStreamMessage - 收到AI流式消息时的回调
   * @param {Function} handlers.onChatMessage - 收到聊天消息时的回调
   * @param {Function} handlers.onError - 错误处理回调
   * @param {Function} handlers.onReconnect - 重连时的回调
   * @returns {Promise} 连接Promise
   */
  async connect(userId, handlers = {}) {
    // 如果已经连接，先断开
    this.disconnect(userId);

    // 获取认证token
    const authStore = useAuthStore();
    const token = authStore.getToken;

    if (!token) {
      const error = new Error('未找到认证token');
      handlers.onError && handlers.onError(error);
      throw error;
    }

    // 存储事件处理器
    this.messageHandlers.set(userId, handlers);

    // 创建AbortController用于取消连接
    const controller = new AbortController();
    this.controllers.set(userId, controller);

    try {
      const url = this.buildSSEUrl(userId);
      console.log(`正在建立SSE连接: ${url}`);

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'token': token, // 发送认证token
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache',
          'Connection': 'keep-alive'
        },
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      console.log(`SSE连接已建立: 用户${userId}`);
      this.reconnectAttempts.set(userId, 0);

      // 调用连接成功回调
      handlers.onConnected && handlers.onConnected({ userId });

      // 开始读取流数据
      this.readSSEStream(userId, response.body, handlers);

    } catch (error) {
      console.error(`SSE连接失败: 用户${userId}`, error);
      this.handleError(userId, error);
      throw error;
    }
  }

  /**
   * 读取SSE流数据
   * @param {string} userId - 用户ID
   * @param {ReadableStream} stream - 响应流
   * @param {Object} handlers - 事件处理器
   */
  async readSSEStream(userId, stream, handlers) {
    const reader = stream.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    try {
      while (true) {
        const { value, done } = await reader.read();

        if (done) {
          console.log(`SSE流结束: 用户${userId}`);
          break;
        }

        if (value) {
          const chunk = decoder.decode(value, { stream: true });
          buffer += chunk;

          // 按行分割并处理SSE事件
          const lines = buffer.split('\n');
          buffer = lines.pop() || ''; // 保留不完整的行

          let currentEvent = '';
          let currentData = '';

          for (const line of lines) {
            const trimmedLine = line.trim();

            if (trimmedLine.startsWith('event:')) {
              currentEvent = trimmedLine.substring(6).trim();
            } else if (trimmedLine.startsWith('data:')) {
              currentData = trimmedLine.substring(5).trim();
            } else if (trimmedLine === '') {
              // 空行表示事件结束，处理当前事件
              if (currentEvent && currentData) {
                this.handleSSEEvent(userId, currentEvent, currentData, handlers);
              }
              currentEvent = '';
              currentData = '';
            }
          }
        }
      }
    } catch (error) {
      if (error.name !== 'AbortError') {
        console.error(`读取SSE流错误: 用户${userId}`, error);
        this.handleError(userId, error);
      }
    } finally {
      try {
        reader.releaseLock();
      } catch (e) {
        // 忽略释放锁的错误
      }
    }
  }

  /**
   * 处理SSE事件
   * @param {string} userId - 用户ID
   * @param {string} eventType - 事件类型
   * @param {string} eventData - 事件数据
   * @param {Object} handlers - 事件处理器
   */
  handleSSEEvent(userId, eventType, eventData, handlers) {
    console.log(`收到SSE事件: ${eventType} -> ${eventData}`);

    try {
      let data;

      // 尝试解析为JSON，如果失败则使用原始数据
      try {
        data = JSON.parse(eventData);
      } catch (parseError) {
        // 如果不是JSON格式，使用原始文本数据
        data = eventData;
        console.log(`事件数据不是JSON格式，使用原始数据: ${eventData}`);
      }

      switch (eventType) {
        case 'connected':
          handlers.onConnected && handlers.onConnected(data);
          break;
        case 'ai_stream_message':
          handlers.onAiStreamMessage && handlers.onAiStreamMessage(data);
          break;
        case 'chat_message':
          handlers.onChatMessage && handlers.onChatMessage(data);
          break;
        default:
          console.log(`未处理的事件类型: ${eventType}`);
      }
    } catch (error) {
      console.error('处理SSE事件失败:', error);
    }
  }

  /**
   * 构建SSE URL
   * @param {string} userId - 用户ID
   * @returns {string} SSE URL
   */
  buildSSEUrl(userId) {
    return `${this.baseURL}/api/chat/events/${userId}`;
  }

  /**
   * 处理连接错误
   * @param {string} userId - 用户ID
   * @param {Error} error - 错误对象
   */
  handleError(userId, error) {
    const handlers = this.messageHandlers.get(userId);
    const currentAttempts = this.reconnectAttempts.get(userId) || 0;

    if (handlers && handlers.onError) {
      handlers.onError(error);
    }

    // 尝试重连
    if (currentAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts.set(userId, currentAttempts + 1);

      setTimeout(() => {
        console.log(`尝试重连: 用户${userId}, 第${currentAttempts + 1}次`);
        this.reconnect(userId);
      }, this.reconnectDelay * (currentAttempts + 1));
    } else {
      console.error(`SSE连接失败，已达到最大重连次数: 用户${userId}`);
      this.disconnect(userId);
    }
  }

  /**
   * 重连到指定用户
   * @param {string} userId - 用户ID
   */
  reconnect(userId) {
    const handlers = this.messageHandlers.get(userId);
    if (handlers && handlers.onReconnect) {
      handlers.onReconnect();
    }

    // 取消当前连接
    const controller = this.controllers.get(userId);
    if (controller) {
      controller.abort();
      this.controllers.delete(userId);
    }

    // 重新建立连接
    this.connect(userId, handlers);
  }

  /**
   * 断开与指定用户的SSE连接
   * @param {string} userId - 用户ID
   */
  disconnect(userId) {
    const controller = this.controllers.get(userId);
    if (controller) {
      controller.abort();
      this.controllers.delete(userId);
    }

    this.messageHandlers.delete(userId);
    this.reconnectAttempts.delete(userId);
    console.log(`SSE连接已断开: 用户${userId}`);
  }

  /**
   * 断开所有SSE连接
   */
  disconnectAll() {
    this.controllers.forEach((controller, userId) => {
      controller.abort();
    });

    this.controllers.clear();
    this.messageHandlers.clear();
    this.reconnectAttempts.clear();
    console.log('所有SSE连接已断开');
  }

  /**
   * 检查用户是否已连接
   * @param {string} userId - 用户ID
   * @returns {boolean} 是否已连接
   */
  isConnected(userId) {
    return this.controllers.has(userId);
  }

  /**
   * 获取所有已连接的用户ID
   * @returns {Array} 用户ID数组
   */
  getConnectedUsers() {
    return Array.from(this.controllers.keys());
  }

  /**
   * 获取在线用户数
   * @returns {Promise<number>} 在线用户数
   */
  async getOnlineCount() {
    try {
      const authStore = useAuthStore();
      const token = authStore.getToken;

      const response = await fetch(`${this.baseURL}/api/chat/online-count`, {
        method: 'GET',
        headers: {
          'token': token
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      return data.data || 0;
    } catch (error) {
      console.error('获取在线用户数失败:', error);
      return 0;
    }
  }

  /**
   * 检查用户是否在线
   * @param {string} userId - 用户ID
   * @returns {Promise<boolean>} 是否在线
   */
  async isUserOnline(userId) {
    try {
      const authStore = useAuthStore();
      const token = authStore.getToken;

      const response = await fetch(`${this.baseURL}/api/chat/is-online/${userId}`, {
        method: 'GET',
        headers: {
          'token': token
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      return data.data || false;
    } catch (error) {
      console.error('检查用户在线状态失败:', error);
      return false;
    }
  }
}

// 创建单例实例
const chatSSEService = new ChatSSEService();

// 在页面卸载时清理所有连接
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    chatSSEService.disconnectAll();
  });
}

export default chatSSEService;