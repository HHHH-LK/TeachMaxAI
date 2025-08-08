import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import teacherChatSSEService from '@/services/teacherChatSSE.js';

/**
 * 师生对话组合式函数
 * 提供低耦合、易维护的师生对话功能
 */
export function useTeacherChat(studentId) {
  // 响应式状态
  const teachers = ref([]);
  const selectedTeacher = ref(null);
  const chatMessages = ref({});
  const currentChatMessages = ref([]);
  const unreadMap = ref({});
  const isConnecting = ref(false);
  const connectionStatus = ref({}); // 存储每个老师的连接状态
  const typingStatus = ref({}); // 存储每个老师的输入状态

  // 计算属性
  const filteredTeachers = computed(() => {
    return teachers.value;
  });

  const onlineTeachers = computed(() => {
    return teachers.value.filter(teacher => teacher.isOnline);
  });

  // 初始化
  const initialize = async () => {
    try {
      await loadTeachers();
      await loadOnlineStatus();
    } catch (error) {
      console.error('初始化师生对话失败:', error);
      ElMessage.error('初始化失败，请刷新页面重试');
    }
  };

  // 加载老师列表
  const loadTeachers = async () => {
    try {
      // 这里可以从API获取老师列表，暂时使用模拟数据
      teachers.value = [
        { id: 1, name: '张老师', avatar: 'https://images.unsplash.com/photo-1534528736809-fcf6dd62947a?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '资深数学教育专家，十年教学经验', tags: ['数学', '微积分', '几何'], isOnline: true },
        { id: 2, name: '李老师', avatar: 'https://images.unsplash.com/photo-1520813791244-63328ce7f511?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '文学硕士，擅长古诗词鉴赏与写作', tags: ['语文', '古诗词', '写作'], isOnline: false },
        { id: 3, name: '王老师', avatar: 'https://images.unsplash.com/photo-1544717305-ad2459b3438a?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '英语口语教练，发音纯正，耐心指导', tags: ['英语', '口语', '语法', '雅思'], isOnline: true },
        { id: 4, name: '赵老师', avatar: 'https://images.unsplash.com/photo-1557862590-f94747c34d55?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '物理实验指导专家，深入浅出讲解', tags: ['物理', '实验', '力学'], isOnline: true },
        { id: 5, name: '刘老师', avatar: 'https://images.unsplash.com/photo-1531123896582-7773ad648f07?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '化学高级教师，高考重难点解析', tags: ['化学', '有机化学', '无机化学'], isOnline: false },
        { id: 6, name: '孙老师', avatar: 'https://images.unsplash.com/photo-1573496359142-b8d8773c9ce7?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '编程入门导师，擅长Python与Java', tags: ['编程', 'Python', 'Java', '数据结构'], isOnline: true },
        { id: 7, name: '周老师', avatar: 'https://images.unsplash.com/photo-1552058544-d8f95c02b926?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', description: '艺术史教授，带你领略艺术之美', tags: ['艺术', '美术史', '鉴赏'], isOnline: false },
      ];

      // 初始化聊天消息
      teachers.value.forEach(teacher => {
        if (!chatMessages.value[teacher.id]) {
          chatMessages.value[teacher.id] = [];
        }
      });
    } catch (error) {
      console.error('加载老师列表失败:', error);
      throw error;
    }
  };

  // 加载在线状态
  const loadOnlineStatus = async () => {
    try {
      const onlineTeachers = await teacherChatSSEService.getOnlineTeachers();
      // 更新老师在线状态
      onlineTeachers.forEach(teacher => {
        const teacherIndex = teachers.value.findIndex(t => t.id === teacher.id);
        if (teacherIndex !== -1) {
          teachers.value[teacherIndex].isOnline = teacher.isOnline;
        }
      });
    } catch (error) {
      console.error('加载在线状态失败:', error);
      // 如果API失败，使用模拟数据
    }
  };

  // 选择老师
  const selectTeacher = async (teacher) => {
    if (selectedTeacher.value?.id === teacher.id) return;

    selectedTeacher.value = teacher;
    currentChatMessages.value = chatMessages.value[teacher.id] || [];
    
    // 清除未读消息
    unreadMap.value[teacher.id] = 0;
    
    // 标记消息为已读
    await markMessagesAsRead(teacher.id);
    
    // 连接到老师的SSE流
    await connectToTeacher(teacher.id);
  };

  // 连接到老师的SSE流
  const connectToTeacher = async (teacherId) => {
    if (isConnecting.value) return;
    
    isConnecting.value = true;
    connectionStatus.value[teacherId] = 'connecting';

    try {
      teacherChatSSEService.connectToTeacher(teacherId, studentId, {
        onMessage: (data) => handleIncomingMessage(teacherId, data),
        onTeacherOnline: (data) => handleTeacherOnline(teacherId, data),
        onTeacherOffline: (data) => handleTeacherOffline(teacherId, data),
        onTyping: (data) => handleTypingStatus(teacherId, data),
        onReadReceipt: (data) => handleReadReceipt(teacherId, data),
        onError: (error) => handleConnectionError(teacherId, error),
        onReconnect: () => handleReconnect(teacherId)
      });

      connectionStatus.value[teacherId] = 'connected';
    } catch (error) {
      console.error(`连接老师${teacherId}失败:`, error);
      connectionStatus.value[teacherId] = 'error';
      ElMessage.error(`连接失败，请重试`);
    } finally {
      isConnecting.value = false;
    }
  };

  // 处理收到的消息
  const handleIncomingMessage = (teacherId, data) => {
    const message = {
      id: data.messageId,
      sender: 'teacher',
      content: data.content,
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false }),
      timestamp: data.timestamp || Date.now(),
      read: false
    };

    // 添加到聊天记录
    if (!chatMessages.value[teacherId]) {
      chatMessages.value[teacherId] = [];
    }
    chatMessages.value[teacherId].push(message);

    // 如果是当前选中的老师，更新当前消息列表
    if (selectedTeacher.value?.id === teacherId) {
      currentChatMessages.value = chatMessages.value[teacherId];
    } else {
      // 增加未读消息计数
      unreadMap.value[teacherId] = (unreadMap.value[teacherId] || 0) + 1;
    }
  };

  // 处理老师上线
  const handleTeacherOnline = (teacherId, data) => {
    const teacherIndex = teachers.value.findIndex(t => t.id === teacherId);
    if (teacherIndex !== -1) {
      teachers.value[teacherIndex].isOnline = true;
    }
    
    if (selectedTeacher.value?.id === teacherId) {
      ElMessage.success(`${teachers.value[teacherIndex]?.name || '老师'}已上线`);
    }
  };

  // 处理老师下线
  const handleTeacherOffline = (teacherId, data) => {
    const teacherIndex = teachers.value.findIndex(t => t.id === teacherId);
    if (teacherIndex !== -1) {
      teachers.value[teacherIndex].isOnline = false;
    }
    
    if (selectedTeacher.value?.id === teacherId) {
      ElMessage.warning(`${teachers.value[teacherIndex]?.name || '老师'}已下线`);
    }
  };

  // 处理输入状态
  const handleTypingStatus = (teacherId, data) => {
    typingStatus.value[teacherId] = data.isTyping;
  };

  // 处理已读回执
  const handleReadReceipt = (teacherId, data) => {
    // 标记消息为已读
    if (chatMessages.value[teacherId]) {
      data.messageIds.forEach(messageId => {
        const message = chatMessages.value[teacherId].find(m => m.id === messageId);
        if (message) {
          message.read = true;
        }
      });
    }
  };

  // 处理连接错误
  const handleConnectionError = (teacherId, error) => {
    connectionStatus.value[teacherId] = 'error';
    console.error(`与老师${teacherId}的连接错误:`, error);
  };

  // 处理重连
  const handleReconnect = (teacherId) => {
    connectionStatus.value[teacherId] = 'reconnecting';
    console.log(`正在重连老师${teacherId}...`);
  };

  // 发送消息
  const sendMessage = async (content) => {
    if (!selectedTeacher.value || !content.trim()) return;

    const teacherId = selectedTeacher.value.id;
    const message = {
      id: Date.now().toString(),
      sender: 'student',
      content: content.trim(),
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false }),
      timestamp: Date.now(),
      read: false
    };

    // 添加到本地消息列表
    currentChatMessages.value.push(message);
    chatMessages.value[teacherId] = currentChatMessages.value;

    try {
      // 发送到服务器
      await teacherChatSSEService.sendMessage(teacherId, studentId, content);
    } catch (error) {
      console.error('发送消息失败:', error);
      ElMessage.error('发送失败，请重试');
    }
  };

  // 标记消息为已读
  const markMessagesAsRead = async (teacherId) => {
    if (!chatMessages.value[teacherId]) return;

    const unreadMessages = chatMessages.value[teacherId]
      .filter(msg => msg.sender === 'teacher' && !msg.read)
      .map(msg => msg.id);

    if (unreadMessages.length > 0) {
      try {
        await teacherChatSSEService.markMessagesAsRead(teacherId, studentId, unreadMessages);
        
        // 更新本地消息状态
        unreadMessages.forEach(messageId => {
          const message = chatMessages.value[teacherId].find(m => m.id === messageId);
          if (message) {
            message.read = true;
          }
        });
      } catch (error) {
        console.error('标记消息已读失败:', error);
      }
    }
  };

  // 获取聊天历史
  const loadChatHistory = async (teacherId, params = {}) => {
    try {
      const history = await teacherChatSSEService.getChatHistory(teacherId, studentId, params);
      chatMessages.value[teacherId] = history.messages || [];
      
      if (selectedTeacher.value?.id === teacherId) {
        currentChatMessages.value = chatMessages.value[teacherId];
      }
    } catch (error) {
      console.error('加载聊天历史失败:', error);
    }
  };

  // 断开连接
  const disconnectFromTeacher = (teacherId) => {
    teacherChatSSEService.disconnectFromTeacher(teacherId);
    connectionStatus.value[teacherId] = 'disconnected';
  };

  // 清理资源
  const cleanup = () => {
    teacherChatSSEService.disconnectAll();
  };

  // 监听选中老师变化
  watch(selectedTeacher, (newTeacher, oldTeacher) => {
    if (oldTeacher) {
      // 断开与之前老师的连接
      disconnectFromTeacher(oldTeacher.id);
    }
    
    if (newTeacher) {
      // 连接到新老师
      connectToTeacher(newTeacher.id);
    }
  });

  // 组件挂载时初始化
  onMounted(() => {
    initialize();
  });

  // 组件卸载时清理
  onUnmounted(() => {
    cleanup();
  });

  return {
    // 状态
    teachers,
    selectedTeacher,
    chatMessages,
    currentChatMessages,
    unreadMap,
    isConnecting,
    connectionStatus,
    typingStatus,
    
    // 计算属性
    filteredTeachers,
    onlineTeachers,
    
    // 方法
    selectTeacher,
    sendMessage,
    loadChatHistory,
    markMessagesAsRead,
    disconnectFromTeacher,
    cleanup
  };
} 