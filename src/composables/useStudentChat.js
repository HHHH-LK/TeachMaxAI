import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import teacherChatSSEService from '@/services/teacherChatSSE.js';

/**
 * 教师与学生对话组合式函数
 * 提供低耦合、易维护的教师与学生对话功能
 */
export function useStudentChat(teacherId) {
  // 响应式状态
  const students = ref([]);
  const selectedStudent = ref(null);
  const chatMessages = ref({});
  const currentChatMessages = ref([]);
  const unreadMap = ref({});
  const isConnecting = ref(false);
  const connectionStatus = ref({}); // 存储每个学生的连接状态
  const typingStatus = ref({}); // 存储每个学生的输入状态

  // 计算属性
  const filteredStudents = computed(() => {
    return students.value;
  });

  const onlineStudents = computed(() => {
    return students.value.filter(student => student.isOnline);
  });

  // 初始化
  const initialize = async () => {
    try {
      await loadStudents();
      await loadOnlineStatus();
    } catch (error) {
      console.error('初始化师生对话失败:', error);
      ElMessage.error('初始化失败，请刷新页面重试');
    }
  };

  // 加载学生列表
  const loadStudents = async () => {
    try {
      // 这里可以从API获取学生列表，暂时使用模拟数据
      students.value = [
        { id: 1, name: '张三', avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '数学成绩优秀，学习态度积极', tags: ['数学', '优秀', '积极'], isOnline: true },
        { id: 2, name: '李四', avatar: 'https://images.unsplash.com/photo-1494790108755-2616b612b786?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '语文基础扎实，写作能力突出', tags: ['语文', '写作', '基础好'], isOnline: false },
        { id: 3, name: '王五', avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '英语口语流利，发音标准', tags: ['英语', '口语', '发音好'], isOnline: true },
        { id: 4, name: '赵六', avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '物理实验能力强，动手能力好', tags: ['物理', '实验', '动手能力'], isOnline: true },
        { id: 5, name: '刘七', avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '化学成绩稳定，理解能力强', tags: ['化学', '理解力', '稳定'], isOnline: false },
        { id: 6, name: '孙八', avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '编程兴趣浓厚，逻辑思维清晰', tags: ['编程', '逻辑思维', '兴趣浓厚'], isOnline: true },
        { id: 7, name: '周九', avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '艺术天赋突出，创造力丰富', tags: ['艺术', '创造力', '天赋'], isOnline: false },
      ];

      // 初始化聊天消息
      students.value.forEach(student => {
        if (!chatMessages.value[student.id]) {
          chatMessages.value[student.id] = [];
        }
      });
    } catch (error) {
      console.error('加载学生列表失败:', error);
      throw error;
    }
  };

  // 加载在线状态
  const loadOnlineStatus = async () => {
    try {
      const onlineStudents = await teacherChatSSEService.getOnlineStudents();
      // 更新学生在线状态
      onlineStudents.forEach(student => {
        const studentIndex = students.value.findIndex(s => s.id === student.id);
        if (studentIndex !== -1) {
          students.value[studentIndex].isOnline = student.isOnline;
        }
      });
    } catch (error) {
      console.error('加载在线状态失败:', error);
      // 如果API失败，使用模拟数据
    }
  };

  // 选择学生
  const selectStudent = async (student) => {
    if (selectedStudent.value?.id === student.id) return;

    selectedStudent.value = student;
    currentChatMessages.value = chatMessages.value[student.id] || [];
    
    // 清除未读消息
    unreadMap.value[student.id] = 0;
    
    // 标记消息为已读
    await markMessagesAsRead(student.id);
    
    // 连接到学生的SSE流
    await connectToStudent(student.id);
  };

  // 连接到学生的SSE流
  const connectToStudent = async (studentId) => {
    if (isConnecting.value) return;
    
    isConnecting.value = true;
    connectionStatus.value[studentId] = 'connecting';

    try {
      teacherChatSSEService.connectToStudent(studentId, teacherId, {
        onMessage: (data) => handleIncomingMessage(studentId, data),
        onStudentOnline: (data) => handleStudentOnline(studentId, data),
        onStudentOffline: (data) => handleStudentOffline(studentId, data),
        onTyping: (data) => handleTypingStatus(studentId, data),
        onReadReceipt: (data) => handleReadReceipt(studentId, data),
        onError: (error) => handleConnectionError(studentId, error),
        onReconnect: () => handleReconnect(studentId)
      });

      connectionStatus.value[studentId] = 'connected';
    } catch (error) {
      console.error(`连接学生${studentId}失败:`, error);
      connectionStatus.value[studentId] = 'error';
      ElMessage.error(`连接失败，请重试`);
    } finally {
      isConnecting.value = false;
    }
  };

  // 处理收到的消息
  const handleIncomingMessage = (studentId, data) => {
    const message = {
      id: data.messageId,
      sender: 'student',
      content: data.content,
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false }),
      timestamp: data.timestamp || Date.now(),
      read: false
    };

    // 添加到聊天记录
    if (!chatMessages.value[studentId]) {
      chatMessages.value[studentId] = [];
    }
    chatMessages.value[studentId].push(message);

    // 如果是当前选中的学生，更新当前消息列表
    if (selectedStudent.value?.id === studentId) {
      currentChatMessages.value = chatMessages.value[studentId];
    } else {
      // 增加未读消息计数
      unreadMap.value[studentId] = (unreadMap.value[studentId] || 0) + 1;
    }
  };

  // 处理学生上线
  const handleStudentOnline = (studentId, data) => {
    const studentIndex = students.value.findIndex(s => s.id === studentId);
    if (studentIndex !== -1) {
      students.value[studentIndex].isOnline = true;
    }
    
    if (selectedStudent.value?.id === studentId) {
      ElMessage.success(`${students.value[studentIndex]?.name || '学生'}已上线`);
    }
  };

  // 处理学生下线
  const handleStudentOffline = (studentId, data) => {
    const studentIndex = students.value.findIndex(s => s.id === studentId);
    if (studentIndex !== -1) {
      students.value[studentIndex].isOnline = false;
    }
    
    if (selectedStudent.value?.id === studentId) {
      ElMessage.warning(`${students.value[studentIndex]?.name || '学生'}已下线`);
    }
  };

  // 处理输入状态
  const handleTypingStatus = (studentId, data) => {
    typingStatus.value[studentId] = data.isTyping;
  };

  // 处理已读回执
  const handleReadReceipt = (studentId, data) => {
    // 标记消息为已读
    if (chatMessages.value[studentId]) {
      data.messageIds.forEach(messageId => {
        const message = chatMessages.value[studentId].find(m => m.id === messageId);
        if (message) {
          message.read = true;
        }
      });
    }
  };

  // 处理连接错误
  const handleConnectionError = (studentId, error) => {
    connectionStatus.value[studentId] = 'error';
    console.error(`与学生${studentId}的连接错误:`, error);
  };

  // 处理重连
  const handleReconnect = (studentId) => {
    connectionStatus.value[studentId] = 'reconnecting';
    console.log(`正在重连学生${studentId}...`);
  };

  // 发送消息
  const sendMessage = async (content) => {
    if (!selectedStudent.value || !content.trim()) return;

    const studentId = selectedStudent.value.id;
    const message = {
      id: Date.now().toString(),
      sender: 'teacher',
      content: content.trim(),
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false }),
      timestamp: Date.now(),
      read: false
    };

    // 添加到本地消息列表
    currentChatMessages.value.push(message);
    chatMessages.value[studentId] = currentChatMessages.value;

    try {
      // 发送到服务器
      await teacherChatSSEService.sendMessageToStudent(studentId, teacherId, content);
    } catch (error) {
      console.error('发送消息失败:', error);
      ElMessage.error('发送失败，请重试');
    }
  };

  // 标记消息为已读
  const markMessagesAsRead = async (studentId) => {
    if (!chatMessages.value[studentId]) return;

    const unreadMessages = chatMessages.value[studentId]
      .filter(msg => msg.sender === 'student' && !msg.read)
      .map(msg => msg.id);

    if (unreadMessages.length > 0) {
      try {
        await teacherChatSSEService.markMessagesAsReadByTeacher(studentId, teacherId, unreadMessages);
        
        // 更新本地消息状态
        unreadMessages.forEach(messageId => {
          const message = chatMessages.value[studentId].find(m => m.id === messageId);
          if (message) {
            message.read = true;
          }
        });
      } catch (error) {
        console.error('标记消息已读失败:', error);
      }
    }
  };

  // 加载聊天历史
  const loadChatHistory = async (studentId, params = {}) => {
    try {
      const history = await teacherChatSSEService.getChatHistory(studentId, teacherId, params);
      chatMessages.value[studentId] = history;
      
      if (selectedStudent.value?.id === studentId) {
        currentChatMessages.value = history;
      }
    } catch (error) {
      console.error('加载聊天历史失败:', error);
    }
  };

  // 断开与学生连接
  const disconnectFromStudent = (studentId) => {
    teacherChatSSEService.disconnectFromStudent(studentId, teacherId);
    connectionStatus.value[studentId] = 'disconnected';
  };

  // 清理资源
  const cleanup = () => {
    students.value.forEach(student => {
      disconnectFromStudent(student.id);
    });
  };

  // 生命周期
  onMounted(() => {
    initialize();
  });

  onUnmounted(() => {
    cleanup();
  });

  return {
    // 状态
    students,
    selectedStudent,
    chatMessages,
    currentChatMessages,
    unreadMap,
    isConnecting,
    connectionStatus,
    typingStatus,
    
    // 计算属性
    filteredStudents,
    onlineStudents,
    
    // 方法
    selectStudent,
    sendMessage,
    loadChatHistory,
    markMessagesAsRead,
    disconnectFromStudent,
    cleanup
  };
} 