<template>
  <div id="apps" class="common-layout">
    <el-container class="chat-container">
      <el-aside width="360px" class="teacher-selection-panel">
        <div class="panel-header">
          <h2 class="panel-title">选择你的老师</h2>
          <el-button
              type="primary"
              size="small"
              :icon="ElIconRefresh"
              @click="refreshTeachers"
              :loading="loading"
              text
          >
            刷新
          </el-button>
        </div>

        <div class="search-box">
          <el-input
              v-model="searchQuery"
              placeholder="搜索老师姓名或专业..."
              :prefix-icon="ElIconSearch"
              clearable
              class="search-input"
          />
        </div>

        <el-scrollbar class="teacher-list-scrollbar">
          <div class="teacher-list" v-loading="loading" element-loading-text="加载中...">
            <div
                v-for="teacher in filteredTeachersBySearch"
                :key="teacher.id"
                class="teacher-card"
                :class="{ 'is-active': selectedTeacher && selectedTeacher.id === teacher.id }"
                @click="selectTeacher(teacher)"
            >
              <div class="card-avatar-wrapper" style="position:relative;">
                <el-avatar :size="64" :src="teacher.avatar" fit="cover" />
                <span class="online-status" :class="{ 'is-online': teacher.isOnline, 'is-offline': !teacher.isOnline }"></span>
                <span v-if="unreadMap[teacher.id] > 0" class="unread-badge">{{ unreadMap[teacher.id] }}</span>
              </div>
              <div class="teacher-info">
                <h3 class="teacher-name">{{ teacher.name }}</h3>
                <p class="teacher-description">{{ teacher.description }}</p>
                <div class="teacher-tags">
                  <el-tag v-for="tag in teacher.tags" :key="tag" size="small" effect="light" type="info">{{ tag }}</el-tag>
                </div>
              </div>
            </div>
            <el-empty
                v-if="!loading && filteredTeachersBySearch.length === 0"
                :description="courseIds.length === 0 ? '您还没有选择任何课程，请先选课' : '没有找到相关老师'"
                :image-size="80"
            />
          </div>
        </el-scrollbar>
      </el-aside>

      <el-main class="chat-main-panel">
        <div v-if="selectedTeacher" class="chat-panel">
          <div class="chat-header">
            <el-avatar :size="48" :src="selectedTeacher.avatar" fit="cover" class="chat-header-avatar" />
            <div class="header-info">
              <h3 class="header-name">{{ selectedTeacher.name }}</h3>
              <p class="header-status">
                {{ selectedTeacher.isOnline ? '在线' : '离线' }}
                <span v-if="connectionStatus[selectedTeacher.id]"
                      :class="'connection-indicator ' + connectionStatus[selectedTeacher.id]">
                  {{ getConnectionStatusText(connectionStatus[selectedTeacher.id]) }}
                </span>
              </p>
            </div>
            <el-dropdown style="margin-left:auto;" trigger="hover">
              <span class="el-dropdown-link" style="cursor:pointer;display:flex;align-items:center;">
                <el-icon><MoreFilled /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="showHistoryDialog = true">查看历史记录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <el-scrollbar ref="chatMessagesScrollbar" class="chat-messages-container">
            <div class="message-list">
              <div
                  v-for="(message, index) in currentChatMessages"
                  :key="index"
                  class="message-item"
                  :class="{ 'is-teacher': message.sender === 'teacher', 'is-student': message.sender === 'student' }"
              >
                <el-avatar v-if="message.sender === 'teacher'" :size="40" :src="selectedTeacher.avatar" class="message-avatar" fit="cover" />
                <el-avatar v-else :size="40" src="https://cube.elemecdn.com/0/88/03b0dff1ffc7ddf404412ec65dc4bbpng.png" class="message-avatar" fit="cover" />
                <div class="message-content">
                  <div class="message-bubble">
                    <p>{{ message.content }}</p>
                  </div>
                  <span class="message-time">{{ message.time }}</span>
                </div>
              </div>
            </div>
          </el-scrollbar>

          <div class="chat-input-area">
            <el-input
                v-model="newMessage"
                placeholder="输入消息..."
                @keyup.enter="handleSendMessage"
                size="large"
                clearable
                class="message-input"
            >
              <template #append>
                <el-button :icon="ElIconPaperclip" text/>
                <el-button type="primary" :icon="ElIconPromotion" @click="handleSendMessage">发送</el-button>
              </template>
            </el-input>
          </div>
        </div>
        <div v-else class="no-teacher-selected">
          <el-empty description="请选择一位老师开始对话"/>
        </div>
      </el-main>
    </el-container>
  </div>
  <el-dialog v-model="showHistoryDialog" title="历史对话详情" width="600px" :close-on-click-modal="false">
    <div v-if="selectedTeacher">
      <div v-for="(msg, idx) in chatMessages[selectedTeacher.id] || []" :key="idx" style="margin-bottom:10px;">
        <b>{{ msg.sender === 'teacher' ? selectedTeacher.name : '我' }}：</b>{{ msg.content }}
        <span style="color:#aaa;font-size:12px;margin-left:8px;">{{ msg.time }}</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="showHistoryDialog = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick, computed, onMounted, onUnmounted } from 'vue';
import { ElContainer, ElAside, ElMain, ElAvatar, ElScrollbar, ElInput, ElButton, ElEmpty, ElTag, ElDropdown, ElDropdownMenu, ElDropdownItem, ElIcon, ElLoading } from 'element-plus';
import {
  Search as ElIconSearch,
  Promotion as ElIconPromotion,
  Paperclip as ElIconPaperclip,
  MoreFilled,
  Refresh as ElIconRefresh,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { studentService, isOnline } from "@/services/api.js";
import { useAuthStore } from '@/store/authStore.js';
import chatSSEService from '@/services/chatSSEService.js';
import chatApiService from '@/services/chatApiService.js';

// 获取当前用户信息的通用函数
const getCurrentUser = () => {
  const authStore = useAuthStore();
  const currentUser = authStore.getUser;

  if (!currentUser || !currentUser.id) {
    throw new Error('无法获取当前用户信息');
  }

  return currentUser;
};

// 使用师生对话组合式函数
const studentId = 16; // 这里应该从用户状态或路由参数获取
//
// const res = studentService.teacherChat.markMessagesAsRead1()
// console.log(res)

// 课程ID列表 - 从学生选课信息获取
const courseIds = ref([]);

// 教师数据
const teachers = ref([]);
const loading = ref(false);

// SSE连接状态
const sseConnections = ref(new Map()); // 存储每个老师的SSE连接状态
const connectionStatus = ref({}); // 连接状态映射

// 获取学生已选课程
const fetchStudentCourses = async () => {
  try {
    // console.log('正在获取学生选课信息...');
    const response = await studentService.getOwnCourses();
    // console.log('选课信息响应:', response);

    if (response.data && response.data.data) {
      // 提取课程ID列表
      courseIds.value = response.data.data.map(course =>
          String(course.id || course.courseId)
      );
      // console.log('学生已选课程ID:', courseIds.value);

      if (courseIds.value.length === 0) {
        ElMessage.info('您还没有选择任何课程');
      } else {
        // ElMessage.success(`获取到 ${courseIds.value.length} 门已选课程`);
      }
    } else {
      console.warn('未获取到学生选课信息');
      courseIds.value = [];
      ElMessage.warning('未获取到选课信息');
    }
  } catch (error) {
    console.error('获取学生选课信息失败:', error);
    courseIds.value = [];
    ElMessage.error('获取选课信息失败');
  }
};

// 检查教师在线状态
const checkTeacherOnlineStatus = async (teacherId) => {
  try {
    const response = await isOnline(teacherId);
    // console.log(`教师${teacherId}在线状态:`, response);
    if (response.data && response.data.success) {
      return response.data.data; // 返回布尔值
    }
    return false;
  } catch (error) {
    console.error(`检查教师 ${teacherId} 在线状态失败:`, error);
    return false;
  }
};

// 获取教师列表
const fetchTeachers = async () => {
  try {
    loading.value = true;

    // 如果没有课程ID，先获取学生选课信息
    if (courseIds.value.length === 0) {
      await fetchStudentCourses();
    }

    if (courseIds.value.length === 0) {
      ElMessage.warning('您还没有选择任何课程，无法获取教师信息');
      teachers.value = [];
      return;
    }

    const response = await studentService.teacherChat.getTeachers(courseIds.value);

    if (response.data && response.data.success) {
      // 处理后端返回的数据格式
      const teacherData = response.data.data;

      // 使用异步方式获取用户ID并转换教师数据
      const teachersListPromises = teacherData.map(async (item) => {
        // 获取用户ID替换原teacherId
        const userId = await studentService.teacherChat.getUserIdByTeacher(item.teacherId);

        return {
          id: userId.data.data,  // 使用获取到的用户ID
          name: item.teacherName,
          description: `${item.courseName} - ${item.department}`,
          avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epicture.png',
          isOnline: false, // 初始设置为离线，稍后更新
          tags: item.tags || [],
          courseName: item.courseName,
          department: item.department,
          status: item.status
        };
      });

      // 等待所有教师数据转换完成
      let teachersList = await Promise.all(teachersListPromises);

      // 检查每个教师的在线状态
      const onlineStatusPromises = teachersList.map(async (teacher) => {
        const onlineStatus = await checkTeacherOnlineStatus(teacher.id);
        return {
          ...teacher,
          isOnline: onlineStatus
        };
      });

      // 等待所有在线状态检查完成
      teachers.value = await Promise.all(onlineStatusPromises);

      ElMessage.success(`成功获取到 ${teachers.value.length} 位教师信息`);
    } else {
      console.error('API返回失败:', response);
      ElMessage.error('获取教师列表失败');
    }
  } catch (error) {
    console.error('获取教师列表错误:', error);
    ElMessage.error(`获取教师列表失败: ${error.message}`);
  } finally {
    loading.value = false;
  }
};

// 刷新教师列表
const refreshTeachers = async () => {
  try {
    loading.value = true;
    await fetchTeachers();
    ElMessage.success('教师列表已更新');
  } catch (error) {
    console.error('刷新失败:', error);
    ElMessage.error('刷新失败，请重试');
  } finally {
    loading.value = false;
  }
};


// 连接到教师的SSE
const connectToTeacherSSE = async (teacherId) => {
  if (sseConnections.value.has(teacherId)) {
    console.log(`已连接到教师${teacherId}的SSE`);
    return;
  }

  try {
    // 获取当前学生ID
    const currentUser = getCurrentUser();
    const studentId = currentUser.id;

    console.log(`正在连接到教师${teacherId}的SSE，学生ID: ${studentId}...`);
    connectionStatus.value[teacherId] = 'connecting';

    await chatSSEService.connect(studentId.toString(), {
      onConnected: (data) => {
        console.log(`成功连接到教师${teacherId}的SSE`, data);
        connectionStatus.value[teacherId] = 'connected';
        sseConnections.value.set(teacherId, true);
      },
      onChatMessage: (data) => {
        console.log(`收到来自教师${teacherId}的消息:`, data);
        handleIncomingMessage(teacherId, data);
      },
      onError: (error) => {
        console.error(`连接教师${teacherId}的SSE失败:`, error);
        connectionStatus.value[teacherId] = 'error';
        sseConnections.value.delete(teacherId);
      },
      onReconnect: () => {
        console.log(`正在重连教师${teacherId}的SSE...`);
        connectionStatus.value[teacherId] = 'reconnecting';
      }
    });
  } catch (error) {
    console.error(`连接教师${teacherId}的SSE失败:`, error);
    connectionStatus.value[teacherId] = 'error';
    ElMessage.error(`连接${getTeacherName(teacherId)}失败: ${error.message}`);
  }
};

// 断开教师的SSE连接
const disconnectFromTeacherSSE = (teacherId) => {
  if (sseConnections.value.has(teacherId)) {
    try {
      // 获取当前学生ID
      const currentUser = getCurrentUser();
      const studentId = currentUser.id;

      chatSSEService.disconnect(studentId.toString());
      sseConnections.value.delete(teacherId);
      connectionStatus.value[teacherId] = 'disconnected';
      console.log(`已断开与教师${teacherId}的SSE连接`);
    } catch (error) {
      console.error('断开连接时获取用户信息失败:', error);
    }
  }
};

// 获取教师姓名
const getTeacherName = (teacherId) => {
  const teacher = teachers.value.find(t => t.id === teacherId);
  return teacher ? teacher.name : `教师${teacherId}`;
};

// 处理收到的消息
const handleIncomingMessage = (teacherId, data) => {
  try {
    // 获取当前用户信息
    const currentUser = getCurrentUser();
    const currentUserId = Number(currentUser.id);

    // 检查消息发送者，避免显示自己发送的消息
    const messageSenderId = Number(data.senderUserId || data.senderId || data.userId);

    console.log('收到消息详情:', {
      消息发送者ID: messageSenderId,
      当前用户ID: currentUserId,
      是否为自己发送: messageSenderId === currentUserId,
      消息内容: data.content,
      消息数据: data
    });

    // 如果是自己发送的消息，不处理（避免回显）
    if (messageSenderId === currentUserId) {
      console.log('检测到自己发送的消息，跳过处理');
      return;
    }

    // 确保消息内容存在
    if (!data.content && !data.message) {
      console.log('消息内容为空，跳过处理');
      return;
    }

    const message = {
      sender: 'teacher',
      content: data.content || data.message || '收到新消息',
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    };

    // 添加到聊天记录
    if (!chatMessages.value[teacherId]) {
      chatMessages.value[teacherId] = [];
    }
    chatMessages.value[teacherId].push(message);

    // 如果是当前选中的老师，更新当前消息列表
    if (selectedTeacher.value?.id === teacherId) {
      currentChatMessages.value = chatMessages.value[teacherId];
      nextTick(() => {
        scrollToBottom();
      });
    } else {
      // 增加未读消息计数
      unreadMap.value[teacherId] = (unreadMap.value[teacherId] || 0) + 1;
      ElMessage.info(`收到来自${getTeacherName(teacherId)}的新消息`);
    }
  } catch (error) {
    console.error('处理收到的消息时获取用户信息失败:', error);
  }
};

// 组件挂载时获取教师列表
onMounted(async () => {
  // 先获取学生选课信息，再获取教师列表
  await fetchStudentCourses();
  await fetchTeachers();
});

// 状态
const selectedTeacher = ref(null);
const chatMessages = ref({});
const currentChatMessages = ref([]);
const unreadMap = ref({ 1: 1 }); // 示例未读数
const isConnecting = ref(false);
const typingStatus = ref('');
const filteredTeachers = computed(() => teachers.value);

// 方法
const selectTeacher = async (teacher) => {
  selectedTeacher.value = teacher;
  currentChatMessages.value = chatMessages.value[teacher.id] || [];
  if (unreadMap.value[teacher.id]) {
    unreadMap.value[teacher.id] = 0;
  }

  // 建立与教师的连接并获取会话ID
  try {
    // 获取当前学生ID
    const currentUser = getCurrentUser();
    const studentId = currentUser.id;

    const connectionResult = await studentService.teacherChat.setConnection(studentId, teacher.id);
    console.log('建立连接成功，连接ID：', connectionResult);

    // 存储连接ID
    if (connectionResult && connectionResult.data && connectionResult.data.data) {
      conversationIds.value.set(teacher.id, connectionResult.data.data);
    }

    ElMessage.success(`已连接到${teacher.name}`);
  } catch (error) {
    console.error('建立连接失败:', error);
    ElMessage.error(`连接${teacher.name}失败`);
  }

  // 连接到教师的SSE
  await connectToTeacherSSE(teacher.id);

  // 加载历史记录
  // await loadChatHistory();
};

const sendMessage = async (messageContent) => {
  if (!selectedTeacher.value) return;
  const teacherId = selectedTeacher.value.id;
  const currentTime = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

  // 先添加到本地消息列表
  if (!chatMessages.value[teacherId]) {
    chatMessages.value[teacherId] = [];
  }

  const newMessage = {
    sender: 'student',
    content: messageContent,
    time: currentTime,
  };

  // 添加到聊天记录
  chatMessages.value[teacherId].push(newMessage);
  console.log("收到消息" + newMessage)

  // 更新当前显示的消息列表
  currentChatMessages.value = chatMessages.value[teacherId];

  try {
    // 调用后端API发送消息
    const response = await sendMessageToBackend(teacherId, messageContent);

    if (response.success) {
      ElMessage.success('消息已发送');
    } else {
      ElMessage.error(`发送失败: ${response.message}`);
    }
  } catch (error) {
    console.error('发送消息失败:', error);
    ElMessage.error('发送失败，请重试');
  }
};

// 调用后端API发送消息
const sendMessageToBackend = async (teacherId, content) => {
  try {
    // 获取会话ID
    const conversationId = conversationIds.value.get(teacherId);
    if (!conversationId) {
      throw new Error('未找到会话ID，请先建立连接');
    }

    // 使用API服务发送消息，传入会话ID
    const result = await chatApiService.sendMessage(teacherId, content, 'text', null, conversationId);
    return result;
  } catch (error) {
    console.error('调用后端发送消息API失败:', error);
    throw error;
  }
};

// 获取或创建会话ID
const conversationIds = ref(new Map()); // 存储教师ID对应的会话ID

const getConversationId = (teacherId) => {
  // 如果已有会话ID，直接返回
  if (conversationIds.value.has(teacherId)) {
    return conversationIds.value.get(teacherId);
  }

  try {
    // 使用学生用户ID作为会话ID
    const currentUser = getCurrentUser();
    const studentId = Number(currentUser.id);

    conversationIds.value.set(teacherId, studentId);
    return studentId;
  } catch (error) {
    console.error('获取会话ID时获取用户信息失败:', error);
    throw new Error('无法获取学生用户ID');
  }
};

const markMessagesAsRead = () => {
  if (selectedTeacher.value && unreadMap.value[selectedTeacher.value.id]) {
    unreadMap.value[selectedTeacher.value.id] = 0;
  }
};

// 加载聊天历史记录
const loadChatHistory = async () => {
  if (!selectedTeacher.value) {
    ElMessage.warning('请先选择一位老师');
    return;
  }

  try {
    const teacherId = selectedTeacher.value.id;
    // 获取会话ID
    const conversationId = conversationIds.value.get(teacherId);
    if (!conversationId) {
      console.warning('未找到会话ID，无法获取历史记录');
      return;
    }

    const response = await studentService.teacherChat.getChatHistory(conversationId);

    if (response.data && response.data.success) {
      const historyData = response.data.data;

      if (historyData && historyData.length > 0) {
        try {
          // 获取当前用户信息
          const currentUser = getCurrentUser();

          // 转换历史记录格式
          const formattedHistory = historyData.map(msg => ({
            sender: msg.senderUserId === Number(currentUser.id) ? 'student' : 'teacher',
            content: msg.content,
            time: new Date(msg.createdAt).toLocaleTimeString('zh-CN', {
              hour: '2-digit',
              minute: '2-digit'
            })
          }));

          // 更新聊天记录
          chatMessages.value[teacherId] = formattedHistory;
          currentChatMessages.value = formattedHistory;

          ElMessage.success(`成功加载 ${formattedHistory.length} 条历史记录`);

          // 滚动到底部
          nextTick(() => {
            scrollToBottom();
          });
        } catch (error) {
          console.error('处理历史记录时获取用户信息失败:', error);
        }
      } else {
        ElMessage.info('暂无历史记录');
      }
    } else {
      console.error('获取历史记录失败');
    }
  } catch (error) {
    console.error('获取历史记录失败:', error);
  }
};

// 搜索查询
const searchQuery = ref('');

// 根据搜索查询过滤教师列表
const filteredTeachersBySearch = computed(() => {
  if (!searchQuery.value) {
    return filteredTeachers.value;
  }
  const query = searchQuery.value.toLowerCase();
  return filteredTeachers.value.filter(teacher =>
      teacher.name.toLowerCase().includes(query) ||
      teacher.description.toLowerCase().includes(query) ||
      teacher.tags.some(tag => tag.toLowerCase().includes(query))
  );
});

// 新消息内容
const newMessage = ref('');

// 滚动条引用
const chatMessagesScrollbar = ref(null);

// 历史对话详情对话框
const showHistoryDialog = ref(false);

// 监听选中的老师变化，更新当前对话消息
watch(selectedTeacher, (newVal, oldVal) => {
  if (oldVal) {
    // 断开与之前老师的连接
    disconnectFromTeacherSSE(oldVal.id);
  }

  if (newVal) {
    currentChatMessages.value = chatMessages.value[newVal.id] || [];
    // 切换老师后，滚动到底部
    nextTick(() => {
      scrollToBottom();
    });
  } else {
    currentChatMessages.value = [];
  }
}, { immediate: true });

// 发送消息的包装函数
const handleSendMessage = async () => {
  if (newMessage.value.trim() === '') return;

  await sendMessage(newMessage.value.trim());
  newMessage.value = ''; // 清空输入框
  scrollToBottom(); // 发送消息后滚动到底部
};

// 滚动到底部
const scrollToBottom = () => {
  if (chatMessagesScrollbar.value) {
    const wrap = chatMessagesScrollbar.value.wrap$;
    if (wrap) {
      wrap.scrollTop = wrap.scrollHeight;
    }
  }
};

// 获取连接状态文本
const getConnectionStatusText = (status) => {
  const statusMap = {
    'connecting': '连接中...',
    'connected': '已连接',
    'error': '连接失败',
    'reconnecting': '重连中...',
    'disconnected': '已断开'
  };
  return statusMap[status] || '';
};

// 组件卸载时清理所有SSE连接
onUnmounted(() => {
  sseConnections.value.forEach((connected, teacherId) => {
    if (connected) {
      disconnectFromTeacherSSE(teacherId);
    }
  });
});
</script>

<style lang="less" scoped>
// 引入参照的自定义色彩
@primary: #2563eb; /* 主蓝色 */
@primary-light: #3b82f6; /* 浅一点的蓝色 */
@primary-dark: #1d4ed8; /* 深一点的蓝色 */
@primary-bg: #eff6ff; /* 蓝色背景 */
@primary-border: #dbeafe; /* 蓝色边框 */
@secondary: #60a5fa; /* 辅助蓝色 */
@success: #10b981; /* 成功色（搭配蓝色） */
@warning: #f59e0b; /* 警告色 */
@danger: #ef4444; /* 危险色 */
@text-primary: #1e293b; /* 主要文字 */
@text-secondary: #64748b; /* 次要文字 */
@text-tertiary: #94a3b8; /* tertiary文字 */
@bg-white: #ffffff; /* 白色背景 */
@bg-light: #f8fafc; /* 浅色背景 */
@border-light: #e2e8f0; /* 浅色边框 */
@shadow-sm: 0 2px 8px rgba(37, 99, 235, 0.1); /* 小阴影 */
@shadow-md: 0 4px 16px rgba(37, 99, 235, 0.15); /* 中阴影 */
@radius-sm: 6px;
@radius-md: 12px;
@radius-lg: 16px;
@transition: all 0.3s ease;
@breakpoint-md: 1024px;
@breakpoint-sm: 768px;

// 图标基础样式
.icon-base() {
  display: inline-block;
  width: 1em;
  height: 1em;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

#apps {
  font-family: "Inter", "Helvetica Neue", sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: @text-primary;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, @primary-bg 0%, #e0f2fe 100%);
}

.common-layout {
  height: 88vh;
  border-radius: @radius-lg;
  background-color: @bg-white;
  box-shadow: @shadow-md;
  overflow: hidden;
  display: flex;
  transition: @transition;
}

.chat-container {
  width: 100%;
  height: 100%;
  display: flex;
}

.teacher-selection-panel {
  background-color: @bg-white;
  border-right: 1px solid @primary-border;
  padding: 35px 25px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;

  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: url("data:image/svg+xml,%3Csvg width%3D%226%22 height%3D%226%22 viewBox%3D%220 0 6 6%22 xmlns%3D%22http%3A//www.w3.org/2000/svg%22%3E%3Cg fill%3D%22%2393c5fd%22 fill-opacity%3D%220.03%22 fill-rule%3D%22evenodd%22%3E%3Cpath d%3D%22M5 0h1L0 6V5zM6 5v1H0V0l6 6z%22/%3E%3C/g%3E%3C/svg%3E");
    opacity: 0.8;
    pointer-events: none;
    z-index: 0;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 25px;

    .panel-title {
      font-size: 26px;
      color: @text-primary;
      margin: 0;
      font-weight: 700;
      position: relative;
      letter-spacing: 0.5px;
      padding-bottom: 10px;

      &::after {
        content: "";
        position: absolute;
        bottom: 0;
        left: 0;
        width: 70px;
        height: 4px;
        background-color: @primary;
        border-radius: 3px;
        opacity: 0.8;
      }
    }
  }
}

.search-box {
  margin-bottom: 25px;
  .search-input {
    /deep/ .el-input__wrapper {
      border-radius: 30px;
      padding: 10px 15px;
      box-shadow: 0 3px 10px rgba(37, 99, 235, 0.05) inset;
      border: 1px solid @primary-border;
      transition: @transition;
      &:hover {
        border-color: @secondary;
      }
      &.is-focus {
        border-color: @primary;
        box-shadow: 0 0 0 1px @primary inset, 0 3px 10px rgba(37, 99, 235, 0.1);
      }
    }
    /deep/ .el-input__prefix {
      color: @text-secondary;
    }
  }
}

.teacher-list-scrollbar {
  flex-grow: 1;
  height: calc(100% - 150px);
}

.teacher-list {
  padding-right: 15px;
}



.teacher-card {
  display: flex;
  align-items: center;
  padding: 20px 22px;
  margin: 10px 2px;
  border-radius: @radius-md;
  cursor: pointer;
  transition: @transition;
  background-color: @bg-white;
  border: 1px solid @primary-border;
  box-shadow: @shadow-sm;

  &:hover {
    background-color: @primary-bg;
    border-color: @primary;
    box-shadow: @shadow-md;
    transform: translateY(-5px);
  }

  &.is-active {
    background: linear-gradient(to right, @primary-bg, #dbeafe);
    border-color: @primary;
    box-shadow: @shadow-sm;
    transform: scale(1.01);
    .teacher-name,
    .teacher-description,
    .teacher-tags .el-tag {
      color: @text-primary;
    }
    .online-status {
      border-color: @primary-light;
    }
  }

  .card-avatar-wrapper {
    position: relative;
    margin-right: 20px;
    flex-shrink: 0;

    /deep/ .el-avatar {
      border: 3px solid @primary-border;
      box-shadow: 0 3px 10px rgba(37, 99, 235, 0.1);
      transition: @transition;
    }

    .online-status {
      position: absolute;
      bottom: -2px;
      right: -2px;
      width: 14px;
      height: 14px;
      border-radius: 50%;
      background-color: @text-tertiary;
      border: 3px solid @bg-white;
      transition: background-color 0.3s ease;
      &.is-online {
        background-color: @success;
        box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
      }
      &.is-offline {
        background-color: @text-tertiary;
        opacity: 0.6;
      }
    }

    .unread-badge {
      position: absolute;
      top: -6px;
      right: -6px;
      background: @danger;
      color: @bg-white;
      border-radius: 50%;
      padding: 2px 7px;
      font-size: 12px;
      font-weight: bold;
      z-index: 2;
      box-shadow: 0 0 4px rgba(239, 68, 68, 0.2);
    }
  }

  .teacher-info {
    flex-grow: 1;
    display: flex;
    flex-direction: column;

    .teacher-name {
      font-size: 20px;
      margin: 0 0 8px 0;
      color: @text-primary;
      font-weight: 600;
      letter-spacing: 0.2px;
    }

    .teacher-description {
      font-size: 14px;
      color: @text-secondary;
      margin: 0 0 10px 0;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .teacher-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      /deep/ .el-tag {
        border-radius: 6px;
        background-color: @primary-bg;
        color: @primary;
        border: 1px solid @primary-border;
        padding: 0 10px;
        height: 28px;
        font-size: 12px;
        &.el-tag--info {
          --el-tag-bg-color: @primary-bg;
          --el-tag-text-color: @primary;
          --el-tag-border-color: @primary-border;
        }
      }
    }
  }
}

/deep/ .el-empty {
  /deep/ .el-empty__image {
    margin-bottom: 15px;
  }
  /deep/ .el-empty__description p {
    font-size: 15px;
    color: @text-secondary;
  }
}

.chat-main-panel {
  background-color: @bg-light;
  display: flex;
  flex-direction: column;
  padding: 0;
  position: relative;
  overflow: hidden;

  &::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: url("data:image/svg+xml,%3Csvg width%3D%226%22 height%3D%226%22 viewBox%3D%220 0 6 6%22 xmlns%3D%22http%3A//www.w3.org/2000/svg%22%3E%3Cg fill%3D%22%2393c5fd%22 fill-opacity%3D%220.08%22 fill-rule%3D%22evenodd%22%3E%3Cpath d%3D%22M5 0h1L0 6V5zM6 5v1H0V0l6 6z%22/%3E%3C/g%3E%3C/svg%3E");
    opacity: 0.6;
    pointer-events: none;
    z-index: 0;
    background-size: 8px 8px;
  }
}

.no-teacher-selected {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-color: @primary-bg;
  /deep/ .el-empty {
    /deep/ .el-empty__description p {
      font-size: 18px;
      color: @text-secondary;
      margin-top: 20px;
      font-weight: 500;
    }
  }
}

.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  z-index: 1;
}

.chat-header {
  padding: 20px 30px;
  border-bottom: 1px solid @primary-border;
  background-color: @bg-white;
  display: flex;
  align-items: center;
  box-shadow: @shadow-sm;
  z-index: 10;

  .chat-header-avatar {
    margin-right: 15px;
    border: 2px solid @primary-border;
  }
  .header-info {
    .header-name {
      margin: 0;
      font-size: 22px;
      color: @text-primary;
      font-weight: 600;
      letter-spacing: 0.3px;
    }
    .header-status {
      font-size: 13px;
      color: @text-secondary;
      margin-top: 5px;
      display: flex;
      align-items: center;
      gap: 8px;

      .connection-indicator {
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 11px;
        font-weight: 500;

        &.connecting {
          background-color: #fef3c7;
          color: #d97706;
        }

        &.connected {
          background-color: #d1fae5;
          color: #059669;
        }

        &.error {
          background-color: #fee2e2;
          color: #dc2626;
        }

        &.reconnecting {
          background-color: #fef3c7;
          color: #d97706;
        }

        &.disconnected {
          background-color: #f3f4f6;
          color: #6b7280;
        }
      }
    }
  }
}

.chat-messages-container {
  flex-grow: 1;
  padding: 30px;
  overflow-y: auto;
  background-color: transparent;
}

.message-list {
  display: flex;
  flex-direction: column;
}

.message-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 25px;

  .message-avatar {
    flex-shrink: 0;
    border: 2px solid @bg-white;
    box-shadow: 0 1px 5px rgba(37, 99, 235, 0.1);
    margin-top: 5px;
  }

  .message-content {
    display: flex;
    flex-direction: column;
    max-width: 70%;
    position: relative;
    animation: fadeInSlideUp 0.3s ease-out forwards;
  }

  .message-bubble {
    padding: 14px 20px;
    border-radius: 20px;
    word-wrap: break-word;
    white-space: pre-wrap;
    box-shadow: @shadow-sm;
    line-height: 1.6;

    p {
      margin: 0;
      font-size: 16px;
      color: @text-primary;
    }
  }

  .message-time {
    font-size: 12px;
    color: @text-tertiary;
    margin-top: 8px;
    opacity: 0.9;
    letter-spacing: 0.1px;
  }

  &.is-teacher {
    justify-content: flex-start;
    .message-avatar {
      margin-right: 15px;
    }
    .message-content {
      align-items: flex-start;
    }
    .message-bubble {
      background-color: @bg-white;
      border: 1px solid @primary-border;
      border-top-left-radius: 6px;
    }
    .message-time {
      text-align: left;
    }
  }

  &.is-student {
    justify-content: flex-end;
    .message-avatar {
      margin-left: 15px;
      order: 2;
    }
    .message-content {
      align-items: flex-end;
      order: 1;
    }
    .message-bubble {
      background-color: @primary;
      border-top-right-radius: 6px;

      p {
        color: @bg-white;
      }
    }
    .message-time {
      text-align: right;
      color: @text-tertiary;
    }
  }
}

@keyframes fadeInSlideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-input-area {
  padding: 15px 30px;
  border-top: 1px solid @primary-border;
  background-color: @bg-white;
  box-shadow: 0 -2px 10px rgba(37, 99, 235, 0.05);
  .el-button {
    border-radius: 50%;
    width: 40px;
    height: 40px;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 18px;
    transition: @transition;
    margin-left: 8px;

    &:not(.el-button--primary) {
      color: @text-secondary;
      background-color: @primary-bg;

      &:hover {
        color: @primary;
        background-color: @primary-border;
        transform: scale(1.1);
      }
    }

    &.el-button--primary {
      color: @bg-white;
      background-color: @primary;
      width: auto;
      min-width: 80px;
      padding: 0 20px;
      border-radius: 20px;
      justify-content: space-between;

      &:hover {
        background-color: @primary-dark;
        transform: scale(1.05);
      }
    }
  }

  .message-input {
    width: 100%;
    /deep/ .el-input__wrapper {
      border-radius: 30px;
      padding: 12px 20px;
      box-shadow: 0 3px 12px rgba(37, 99, 235, 0.05) inset;
      border: 1px solid @primary-border;
      transition: @transition;
      &:hover {
        border-color: @secondary;
      }
      &.is-focus {
        border-color: @primary;
        box-shadow: 0 0 0 1px @primary inset, 0 3px 12px rgba(37, 99, 235, 0.1);
      }
    }
    /deep/ .el-input-group__append {
      background-color: transparent;
      border: none;
      box-shadow: none;
      padding: 0 10px;
      display: flex;
      align-items: center;
      gap: 20px;
    }
  }
}

.input-btn {
  /deep/ span {
    color: @text-secondary;
    transition: @transition;
    &:hover {
      color: @primary;
      transform: scale(1.1);
    }
  }
}

.send-btn {
  /deep/ span {
    color: @bg-white;
    transition: @transition;
  }
}

/deep/ .el-scrollbar__wrap {
  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: rgba(37, 99, 235, 0.4);
    border-radius: 4px;
    &:hover {
      background-color: rgba(37, 99, 235, 0.6);
    }
  }
  &::-webkit-scrollbar-track {
    background-color: transparent;
  }
}

// 响应式设计
@media (max-width: @breakpoint-md) {
  .teacher-selection-panel {
    padding: 25px 15px;
  }

  .chat-header {
    padding: 15px 20px;
  }

  .chat-messages-container {
    padding: 20px;
  }
}

@media (max-width: @breakpoint-sm) {
  .common-layout {
    height: 100vh;
    border-radius: 0;
  }

  .teacher-selection-panel {
    width: 100% !important;
    height: 100% !important;
    position: absolute;
    z-index: 100;
    transform: translateX(-100%);
    transition: transform 0.3s ease;

    &.active {
      transform: translateX(0);
    }
  }

  .chat-main-panel {
    width: 100% !important;
  }
}
</style>