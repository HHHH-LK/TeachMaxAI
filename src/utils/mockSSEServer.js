/**
 * 模拟SSE服务器
 * 用于测试师生对话功能
 */
class MockSSEServer {
  constructor() {
    this.connections = new Map();
    this.messageQueue = new Map();
    this.isRunning = false;
  }

  /**
   * 启动模拟服务器
   */
  start() {
    if (this.isRunning) return;
    
    this.isRunning = true;
    console.log('模拟SSE服务器已启动');
    
    // 模拟老师上线/下线
    this.simulateTeacherStatus();
    
    // 模拟消息发送
    this.simulateMessages();
  }

  /**
   * 停止模拟服务器
   */
  stop() {
    this.isRunning = false;
    this.connections.clear();
    this.messageQueue.clear();
    console.log('模拟SSE服务器已停止');
  }

  /**
   * 模拟老师状态变化
   */
  simulateTeacherStatus() {
    const teachers = [1, 2, 3, 4, 5, 6, 7];
    
    setInterval(() => {
      if (!this.isRunning) return;
      
      const randomTeacher = teachers[Math.floor(Math.random() * teachers.length)];
      const isOnline = Math.random() > 0.3; // 70%概率在线
      
      this.broadcastToTeacher(randomTeacher, {
        type: isOnline ? 'teacher_online' : 'teacher_offline',
        teacherId: randomTeacher,
        timestamp: Date.now()
      });
    }, 30000); // 每30秒随机改变一个老师的状态
  }

  /**
   * 模拟消息发送
   */
  simulateMessages() {
    const teachers = [1, 2, 3, 4, 5, 6, 7];
    const messages = [
      '您好，有什么问题可以帮助您解答吗？',
      '这个问题很有趣，让我详细为您解释一下。',
      '您提到的这个知识点确实很重要，建议您多加练习。',
      '我注意到您在这个方面有些困惑，我们可以一起探讨。',
      '您的理解基本正确，但还需要注意一些细节。',
      '这个问题可以从多个角度来思考，让我为您分析一下。',
      '您提出的问题很有深度，这体现了您的思考能力。',
      '我建议您先复习一下相关的基础知识，然后再继续。',
      '您的学习方法很好，继续保持这种学习态度。',
      '这个问题比较复杂，我们可以分步骤来解决。'
    ];
    
    setInterval(() => {
      if (!this.isRunning) return;
      
      // 随机选择一个老师发送消息
      const randomTeacher = teachers[Math.floor(Math.random() * teachers.length)];
      const randomMessage = messages[Math.floor(Math.random() * messages.length)];
      
      // 检查是否有学生连接到这个老师
      if (this.connections.has(randomTeacher)) {
        this.sendMessageToTeacher(randomTeacher, {
          type: 'message',
          messageId: Date.now().toString(),
          content: randomMessage,
          timestamp: Date.now(),
          teacherId: randomTeacher
        });
      }
    }, 45000); // 每45秒随机发送一条消息
  }

  /**
   * 添加连接
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {EventSource} eventSource - SSE连接
   */
  addConnection(teacherId, studentId, eventSource) {
    const key = `${teacherId}-${studentId}`;
    this.connections.set(teacherId, {
      studentId,
      eventSource,
      connectedAt: Date.now()
    });
    
    console.log(`学生${studentId}连接到老师${teacherId}`);
    
    // 发送连接确认消息
    this.sendMessageToTeacher(teacherId, {
      type: 'connection_established',
      teacherId,
      studentId,
      timestamp: Date.now()
    });
  }

  /**
   * 移除连接
   * @param {number} teacherId - 老师ID
   */
  removeConnection(teacherId) {
    if (this.connections.has(teacherId)) {
      const connection = this.connections.get(teacherId);
      console.log(`学生${connection.studentId}断开与老师${teacherId}的连接`);
      this.connections.delete(teacherId);
    }
  }

  /**
   * 向指定老师发送消息
   * @param {number} teacherId - 老师ID
   * @param {Object} data - 消息数据
   */
  sendMessageToTeacher(teacherId, data) {
    const connection = this.connections.get(teacherId);
    if (connection && connection.eventSource) {
      try {
        const event = new MessageEvent('message', {
          data: JSON.stringify(data)
        });
        connection.eventSource.dispatchEvent(event);
      } catch (error) {
        console.error('发送消息失败:', error);
      }
    }
  }

  /**
   * 广播消息给指定老师的所有连接
   * @param {number} teacherId - 老师ID
   * @param {Object} data - 消息数据
   */
  broadcastToTeacher(teacherId, data) {
    this.sendMessageToTeacher(teacherId, data);
  }

  /**
   * 处理学生发送的消息
   * @param {number} teacherId - 老师ID
   * @param {number} studentId - 学生ID
   * @param {string} message - 消息内容
   */
  handleStudentMessage(teacherId, studentId, message) {
    console.log(`收到学生${studentId}发给老师${teacherId}的消息:`, message);
    
    // 模拟老师回复
    setTimeout(() => {
      const replies = [
        '我收到了您的消息，正在为您准备详细的解答。',
        '您的问题很有价值，让我为您详细分析一下。',
        '感谢您的提问，这个问题确实值得深入探讨。',
        '您的理解方向是正确的，我们可以进一步讨论。',
        '这个问题涉及到几个重要概念，让我为您梳理一下。'
      ];
      
      const randomReply = replies[Math.floor(Math.random() * replies.length)];
      
      this.sendMessageToTeacher(teacherId, {
        type: 'message',
        messageId: Date.now().toString(),
        content: randomReply,
        timestamp: Date.now(),
        teacherId,
        isReply: true
      });
    }, 2000 + Math.random() * 3000); // 2-5秒后回复
  }

  /**
   * 获取连接统计
   * @returns {Object} 连接统计信息
   */
  getConnectionStats() {
    return {
      totalConnections: this.connections.size,
      connections: Array.from(this.connections.entries()).map(([teacherId, connection]) => ({
        teacherId,
        studentId: connection.studentId,
        connectedAt: connection.connectedAt,
        duration: Date.now() - connection.connectedAt
      }))
    };
  }
}

// 创建全局实例
const mockSSEServer = new MockSSEServer();

// 在开发环境下自动启动
if (import.meta.env.DEV) {
  mockSSEServer.start();
}

export default mockSSEServer; 