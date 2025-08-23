<template>
  <div id="apps" class="common-layout">
    <el-container class="chat-container">
      <el-aside width="360px" class="student-selection-panel">
        <div class="panel-header">
          <h2 class="panel-title">待回复的学生</h2>
          <el-button
              type="primary"
              size="small"
              :icon="ElIconRefresh"
              @click="refreshAllStudents"
              :loading="loading"
              text
          >
            刷新
          </el-button>
        </div>

        <div class="search-box">
          <el-input
              v-model="searchQuery"
              placeholder="搜索学生姓名或专业..."
              :prefix-icon="ElIconSearch"
              clearable
              class="search-input"
          />
        </div>

        <el-scrollbar class="student-list-scrollbar">
          <div class="student-list">
            <div v-if="loading" class="loading-container">
              <el-skeleton :rows="3" animated />
            </div>
            <div
                v-else
                v-for="student in filteredStudentsBySearch"
                :key="student.id"
                class="student-card"
                :class="{ 'is-active': selectedStudent && selectedStudent.id === student.id }"
                @click="selectStudent(student)"
            >
              <div class="card-avatar-wrapper" style="position:relative;">
                <el-avatar :size="64" :src="student.avatar" fit="cover" />
                <span class="online-status" :class="{ 'is-online': student.isOnline }"></span>
                <span v-if="unreadMap[student.id] > 0" class="unread-badge">{{ unreadMap[student.id] }}</span>
              </div>
              <div class="student-info">
                <h3 class="student-name">{{ student.name }}</h3>
                <p class="student-description">{{ student.description }}</p>
              </div>
            </div>
            <el-empty v-if="!loading && filteredStudentsBySearch.length === 0" description="没有找到相关学生" :image-size="80" />
          </div>
        </el-scrollbar>
      </el-aside>

      <el-main class="chat-main-panel">
        <div v-if="selectedStudent" class="chat-panel">
          <div class="chat-header">
            <el-avatar :size="48" :src="selectedStudent.avatar" fit="cover" class="chat-header-avatar" />
            <div class="header-info">
              <h3 class="header-name">{{ selectedStudent.name }}</h3>
              <p class="header-status">
                {{ selectedStudent.isOnline ? '在线' : '离线' }}
                <span v-if="connectionStatus"
                      :class="'connection-indicator ' + connectionStatus">
                  {{ getConnectionStatusText(connectionStatus) }}
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
                  :class="{ 'is-student': message.sender === 'student', 'is-teacher': message.sender === 'teacher' }"
              >
                <el-avatar v-if="message.sender === 'student'" :size="40" :src="selectedStudent.avatar" class="message-avatar" fit="cover" />
                <el-avatar v-else :size="40" :src="teacherAvatar" class="message-avatar" fit="cover" />
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
        <div v-else class="no-student-selected">
          <el-empty description="请选择一位学生开始对话"/>
        </div>
      </el-main>
    </el-container>
  </div>
  <el-dialog v-model="showHistoryDialog" title="历史对话详情" width="600px" :close-on-click-modal="false">
    <div v-if="selectedStudent">
      <div v-for="(msg, idx) in chatMessages[selectedStudent.id] || []" :key="idx" style="margin-bottom:10px;">
        <b>{{ msg.sender === 'student' ? selectedStudent.name : '我' }}：</b>{{ msg.content }}
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
import { ElContainer, ElAside, ElMain, ElAvatar, ElScrollbar, ElInput, ElButton, ElEmpty, ElTag, ElDropdown, ElDropdownMenu, ElDropdownItem, ElIcon, ElSkeleton, ElDescriptions, ElDescriptionsItem } from 'element-plus';
import {
  Search as ElIconSearch,
  Promotion as ElIconPromotion,
  Paperclip as ElIconPaperclip,
  MoreFilled,
  Refresh as ElIconRefresh,
} from '@element-plus/icons-vue';
import teacherAvatar from '@/assets/teacher-avatar.png';
import {teacherService, isOnline, studentService} from "@/services/api.js";
import chatSSEService from '@/services/chatSSEService.js';
import {getCurrentUser, getCurrentUserId} from '@/utils/userUtils.js';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/authStore.js';

// 学生数据状态
const students = ref([]);
const loading = ref(false);
const course = ref(null);

// 全局（当前老师自己）的SSE连接状态
const sseUserId = ref(null);
const connectionStatus = ref('disconnected'); // connecting | connected | error | reconnecting | disconnected
const lastHeartbeatAt = ref(0);

// 获取未读消息数据
const fetchUnreadMessages = async () => {
  try {
    const res = await studentService.teacherChat.markMessagesAsRead2();
    console.log('获取未读消息数据:', res);

    if (res.data && res.data.code === 0 && res.data.data) {
      // 处理未读消息数据，按学生ID分组
      const unreadData = res.data.data;
      const unreadByStudent = {};

      unreadData.forEach(item => {
        const studentId = item.studentId;
        if (!unreadByStudent[studentId]) {
          unreadByStudent[studentId] = {
            totalUnread: 0,
            conversations: []
          };
        }
        unreadByStudent[studentId].totalUnread += item.unreadCount;
        unreadByStudent[studentId].conversations.push({
          conversationId: item.conversationId,
          unreadCount: item.unreadCount
        });
      });

      // 更新未读消息映射
      Object.keys(unreadByStudent).forEach(studentId => {
        unreadMap.value[studentId] = unreadByStudent[studentId].totalUnread;
      });

      console.log('处理后的未读消息数据:', unreadByStudent);
    }
  } catch (error) {
    console.error('获取未读消息数据失败:', error);
  }
};

// 获取学生在线状态 - 改用 chatSSEService
const fetchStudentOnlineStatus = async (studentId) => {
  try {
    return await chatSSEService.isUserOnline(String(studentId));
  } catch (error) {
    console.error(`获取学生${studentId}在线状态出错:`, error);
    return false;
  }
};

// 获取班级学生数据
const fetchClassStudents = async (courseId) => {
  try {
    const res = await teacherService.getClassStudents(courseId);
    console.log(`获取课程${courseId}的学生数据:`, res);

    if (res.data && res.data.code === 0 && res.data.data) {
      // 先获取学号列表，然后通过学号获取用户ID
      const studentsWithUserIds = await Promise.all(
          res.data.data.map(async (student) => {
            try {
              // 通过学号获取用户ID
              const userResponse = await teacherService.getStudentByNumber(student.studentNumber);
              console.log(`获取学号${student.studentNumber}的用户信息:`, userResponse);

              let userId = student.studentNumber; // 默认使用学号作为ID

              if (userResponse.data && userResponse.data.code === 0 && userResponse.data.data) {
                // 根据实际API响应结构获取用户ID
                userId = userResponse.data.data.userId || userResponse.data.data.id;
              } else {
                console.log(`学号${student.studentNumber}获取用户ID失败，使用学号作为ID`);
              }

              return {
                id: userId, // 使用用户ID作为ID
                name: student.realName,
                description: `${student.courseName} - ${student.studentNumber}`,
                avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epicture.png',
                isOnline: false, // 初始设置为离线，稍后更新
                studentNumber: student.studentNumber, // 保留学号信息
                email: student.email,
                phone: student.phone,
                courseId: student.courseId,
                courseName: student.courseName,
                score: student.score,
                class_name: student.class_name
              };
            } catch (error) {
              console.error(`获取学号${student.studentNumber}的用户信息失败:`, error);
              return null;
            }
          })
      );

      // 过滤掉null值并批量获取在线状态
      const validStudents = studentsWithUserIds.filter(Boolean);
      const onlineStatusPromises = validStudents.map(async (student) => {
        const onlineStatus = await fetchStudentOnlineStatus(student.id);
        return { ...student, isOnline: onlineStatus };
      });

      return await Promise.all(onlineStatusPromises);
    } else {
      console.warn(`课程${courseId}获取学生数据失败`);
      return [];
    }
  } catch (error) {
    console.error(`获取课程${courseId}学生数据出错:`, error);
    return [];
  }
};

// 统一建立连接：页面 mount 后只连一次（用自己的 userId）
const connectSSEOnce = async () => {
  try {
    const currentUser = getCurrentUser();
    const userId = String(currentUser.id);
    sseUserId.value = userId;

    if (chatSSEService.isConnected(userId)) {
      connectionStatus.value = 'connected';
      return;
    }

    connectionStatus.value = 'connecting';

    await chatSSEService.connect(userId, {
      onConnected: () => (connectionStatus.value = 'connected'),
      onHeartbeat: () => (lastHeartbeatAt.value = Date.now()),
      onAiStreamMessage: (d) => console.log('AI流式消息', d),
      onChatMessage: (d) => handleIncomingMessageUnifiedForTeacher(d),
      onError: () => (connectionStatus.value = 'error'),
      onReconnect: () => (connectionStatus.value = 'reconnecting')
    });
  } catch (e) {
    console.error('SSE连接失败', e);
    connectionStatus.value = 'error';
  }
};

// 刷新所有学生数据（课程1和课程2）
const refreshAllStudents = async () => {
  try {
    loading.value = true;

    const userId = getCurrentUserId()
    const res = await teacherService.getTeacherInfo(userId)
    const teacherId = res.data.data.teacherId;
    const response = await teacherService.getAllCourse(teacherId);
    if(response.data && response.data.data) {
      course.value = response.data.data.map(item => ({
        id: item.courseId,
        name: item.courseName,
        semester: item.semester
      }))
    }

    // 生成每个课程的学生数据请求（返回Promise数组）
    const studentPromises = course.value.map(courseItem =>
        fetchClassStudents(courseItem.id)
    );

    // 等待所有请求完成，获取实际数据
    const allCourseStudents = await Promise.all(studentPromises);
    const allStudents = allCourseStudents.flat();

    if (allStudents.length > 0) {
      students.value = allStudents;
      ElMessage.success(`成功加载 ${allStudents.length} 名学生数据`);
    } else {
      ElMessage.warning('获取学生数据失败，暂无学生数据');
      students.value = [];
    }
  } catch (error) {
    console.error('获取所有学生数据出错:', error);
    ElMessage.warning('网络错误，暂无学生数据');
    students.value = [];
  } finally {
    loading.value = false;
  }
};



// 状态
const selectedStudent = ref(null);
const chatMessages = ref({});
const currentChatMessages = ref([]);
const unreadMap = ref({});
const isConnecting = ref(false);
const typingStatus = ref('');
const filteredStudents = computed(() => students.value);
const onlineStudents = computed(() => students.value.filter(s => s.isOnline));

// 获取或创建会话ID
const conversationIds = ref(new Map()); // 存储学生ID对应的会话ID

const getConversationId = (studentId) => {
  // 如果已有会话ID，直接返回
  if (conversationIds.value.has(studentId)) {
    return conversationIds.value.get(studentId);
  }

  // 使用学生用户ID作为会话ID
  const currentUser = getCurrentUser();
  const teacherId = Number(currentUser.id);

  if (!teacherId) {
    throw new Error('无法获取教师用户ID');
  }

  conversationIds.value.set(studentId, teacherId);
  return teacherId;
};

// 统一处理收到的消息（老师端）
const normalizeIncomingChat = (raw) => {
  const src = typeof raw === 'string' ? (() => { try { return JSON.parse(raw) } catch { return {} } })() : (raw || {});
  return {
    senderUserId: String(src.senderUserId ?? src.senderId ?? src.fromUserId ?? ''),
    receiverUserId: String(src.receiverUserId ?? src.receiverId ?? src.toUserId ?? ''),
    content: src.content ?? src.message ?? '',
    createdAt: src.createdAt ?? src.time ?? Date.now()
  };
};

const handleIncomingMessageUnifiedForTeacher = (raw) => {
  const currentUser = getCurrentUser();
  const me = String(currentUser.id);
  const { senderUserId, receiverUserId, content, createdAt } = normalizeIncomingChat(raw);

  if (!content) return;
  if (senderUserId === me) return; // 忽略自己发的

  // 老师端，来自"学生"的消息，落到学生ID对应的会话
  const studentId = senderUserId;
  const timeStr = new Date(createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

  if (!chatMessages.value[studentId]) chatMessages.value[studentId] = [];
  chatMessages.value[studentId].push({
    sender: 'student',
    content,
    time: timeStr
  });

  if (selectedStudent.value?.id === studentId) {
    currentChatMessages.value = chatMessages.value[studentId];
    nextTick(() => { scrollToBottom(); });
  } else {
    unreadMap.value[studentId] = (unreadMap.value[studentId] || 0) + 1;
    ElMessage.info(`收到来自${selectedStudent.value?.name || '学生'}的消息`);
  }
};

// 方法
const selectStudent = async (student) => {
  console.log('选中的学生对象:', student);
  selectedStudent.value = student;

  // 直接获取聊天记录，不自动生成欢迎消息
  currentChatMessages.value = chatMessages.value[student.id] || [];
  if (unreadMap.value[student.id]) {
    unreadMap.value[student.id] = 0;
  }

  // 建立与学生的连接并获取会话ID
  try {
    // 获取当前教师ID
    const currentUser = getCurrentUser();
    const teacherId = currentUser.id;

    if (!teacherId) {
      throw new Error('无法获取教师用户ID');
    }

    const connectionResult = await studentService.teacherChat.setConnection(student.id, teacherId);
    console.log('建立连接成功，连接ID：', connectionResult);

    // 存储连接ID
    if (connectionResult && connectionResult.data && connectionResult.data.data) {
      conversationIds.value.set(student.id, connectionResult.data.data);
    }
  } catch (error) {
    console.error('建立连接失败:', error);
    ElMessage.error(`连接${student.name}失败`);
  }
};

// 发送消息到后端
const sendMessageToBackend = async (studentId, content) => {
  try {
    const authStore = useAuthStore();
    const token = authStore.getToken;

    if (!token) {
      throw new Error('未找到认证token，请先登录');
    }

    // 验证参数
    if (!studentId) {
      throw new Error('学生ID不能为空');
    }

    // 获取当前用户信息
    const currentUser = getCurrentUser();

    // 验证用户信息
    if (!currentUser || !currentUser.id) {
      throw new Error('用户信息不完整，请重新登录');
    }

    // 验证不能给自己发消息
    const currentUserId = String(currentUser.id);
    const studentIdStr = String(studentId);

    if (currentUserId === studentIdStr) {
      throw new Error('不能给自己发送消息');
    }

    // 获取会话ID
    let conversationId = conversationIds.value.get(studentId);

    // 如果没有提供会话ID，使用教师用户ID作为会话ID
    if (!conversationId) {
      conversationId = Number(currentUser.id);
      console.log('使用教师用户ID作为会话ID:', conversationId);
    }

    const requestData = {
      conversationId: conversationId,
      content: content,
      messageType: 'text',
      fileUrl: null,
      useId: studentId // 接收者用户ID（学生ID）
    };

    console.log('发送消息请求:', requestData);

    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8108'}/studentteacher/chat/sentChatMemory`, {
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
    return { success: true, ...result };
  } catch (error) {
    console.error('调用后端发送消息API失败:', error);
    throw error;
  }
};

const sendMessage = async (messageContent) => {
  if (!selectedStudent.value) return;
  const studentId = selectedStudent.value.id;
  const currentTime = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

  // 先添加到本地消息列表
  if (!chatMessages.value[studentId]) {
    chatMessages.value[studentId] = [];
  }

  const newMessage = {
    sender: 'teacher',
    content: messageContent,
    time: currentTime,
  };

  // 添加到聊天记录
  chatMessages.value[studentId].push(newMessage);
  console.log("收到消息教师消息", newMessage);

  // 更新当前显示的消息列表
  currentChatMessages.value = chatMessages.value[studentId];

  try {
    // 调用后端API发送消息
    const response = await sendMessageToBackend(studentId, messageContent);

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

const loadChatHistory = () => {
  ElMessage.info('暂无历史记录');
};

const markMessagesAsRead = () => {
  if (selectedStudent.value && unreadMap.value[selectedStudent.value.id]) {
    unreadMap.value[selectedStudent.value.id] = 0;
  }
};

// 搜索查询
const searchQuery = ref('');

// 根据搜索查询过滤学生列表
const filteredStudentsBySearch = computed(() => {
  if (!searchQuery.value) {
    return filteredStudents.value;
  }
  const query = searchQuery.value.toLowerCase();
  return filteredStudents.value.filter(student =>
      student.name.toLowerCase().includes(query) ||
      student.description.toLowerCase().includes(query) ||
      student.courseName?.toLowerCase().includes(query)
  );
});

// 新消息内容
const newMessage = ref('');

// 滚动条引用
const chatMessagesScrollbar = ref(null);

// 历史对话详情对话框
const showHistoryDialog = ref(false);

// 监听选中的学生变化，更新当前对话消息
watch(selectedStudent, (newVal) => {
  if (newVal) {
    currentChatMessages.value = chatMessages.value[newVal.id] || [];
    // 切换学生后，滚动到底部
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

// 组件挂载时获取学生数据并建立SSE连接
onMounted(() => {
  refreshAllStudents();
  connectSSEOnce();
});

// 卸载时断开自己的连接
onUnmounted(() => {
  if (sseUserId.value) {
    chatSSEService.disconnect(sseUserId.value);
  }
});
</script>

<style lang="less" scoped>
// 引入参照的自定义色彩（与学生端相同的样式）
@primary: #2563eb;
@primary-light: #3b82f6;
@primary-dark: #1d4ed8;
@primary-bg: #eff6ff;
@primary-border: #dbeafe;
@secondary: #60a5fa;
@success: #10b981;
@warning: #f59e0b;
@danger: #ef4444;
@text-primary: #1e293b;
@text-secondary: #64748b;
@text-tertiary: #94a3b8;
@bg-white: #ffffff;
@bg-light: #f8fafc;
@border-light: #e2e8f0;
@shadow-sm: 0 2px 8px rgba(37, 99, 235, 0.1);
@shadow-md: 0 4px 16px rgba(37, 99, 235, 0.15);
@radius-sm: 6px;
@radius-md: 12px;
@radius-lg: 16px;
@transition: all 0.3s ease;
@breakpoint-md: 1024px;
@breakpoint-sm: 768px;

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

.student-selection-panel {
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
      letter-spacing: 0.5px;
      position: relative;
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
    // 增加样式优先级，确保搜索框样式不被覆盖
    :deep(.el-input__wrapper) {
      border-radius: 30px !important;
      padding: 10px 15px !important;
      box-shadow: 0 3px 10px rgba(37, 99, 235, 0.05) inset !important;
      border: 1px solid @primary-border !important;
      transition: @transition !important;
      background-color: @bg-white !important;
      
      &:hover {
        border-color: @secondary !important;
        box-shadow: 0 3px 12px rgba(37, 99, 235, 0.08) inset !important;
      }
      
      &.is-focus {
        border-color: @primary !important;
        box-shadow: 0 0 0 1px @primary inset, 0 3px 12px rgba(37, 99, 235, 0.1) !important;
      }
    }
    
    :deep(.el-input__inner) {
      font-size: 14px !important;
      color: @text-primary !important;
      &::placeholder {
        color: @text-tertiary !important;
        font-size: 14px !important;
      }
    }
    
    :deep(.el-input__prefix) {
      color: @text-secondary !important;
      font-size: 16px !important;
    }
    
    :deep(.el-input-group__append) {
      background-color: transparent !important;
      border: none !important;
      box-shadow: none !important;
      padding: 0 10px !important;
      display: flex !important;
      align-items: center !important;
      gap: 20px !important;
    }
  }
}

:deep(.el-scrollbar__wrap) {
  &::-webkit-scrollbar {
    width: 8px !important;
    height: 8px !important;
  }
  &::-webkit-scrollbar-thumb {
    background-color: rgba(37, 99, 235, 0.4) !important;
    border-radius: 4px !important;
    &:hover {
      background-color: rgba(37, 99, 235, 0.6) !important;
    }
  }
  &::-webkit-scrollbar-track {
    background-color: transparent !important;
  }
}

:deep(.el-empty) {
  :deep(.el-empty__image) {
    margin-bottom: 15px !important;
  }
  :deep(.el-empty__description p) {
    font-size: 15px !important;
    color: @text-secondary !important;
  }
}

// 响应式设计
@media (max-width: @breakpoint-md) {
  .student-selection-panel {
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

  .student-selection-panel {
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



.student-list-scrollbar {
  flex-grow: 1;
  height: calc(100% - 150px);
}

.student-list {
  padding-right: 15px;
}

.loading-container {
  padding: 20px;
  :deep(.el-skeleton) {
    .el-skeleton__item {
      margin-bottom: 15px !important;
    }
  }
}

.student-card {
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
    .student-name,
    .student-description {
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

    :deep(.el-avatar) {
      border: 3px solid @primary-border !important;
      box-shadow: 0 3px 10px rgba(37, 99, 235, 0.1) !important;
      transition: @transition !important;
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

  .student-info {
    flex-grow: 1;
    display: flex;
    flex-direction: column;

    .student-name {
      font-size: 20px;
      margin: 0 0 8px 0;
      color: @text-primary;
      font-weight: 600;
      letter-spacing: 0.2px;
    }

    .student-description {
      font-size: 14px;
      color: @text-secondary;
      margin: 0 0 10px 0;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }
}

// 其余样式与学生端相同，包括聊天面板、消息样式等
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

.no-student-selected {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-color: @primary-bg;
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

  &.is-student {
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

  &.is-teacher {
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

    :deep(.el-input__wrapper) {
      border-radius: 30px !important;
      padding: 12px 20px !important;
      box-shadow: 0 3px 12px rgba(37, 99, 235, 0.05) inset !important;
      border: 1px solid @primary-border !important;
      transition: @transition !important;

      &:hover {
        border-color: @secondary !important;
      }

      &.is-focus {
        border-color: @primary !important;
        box-shadow: 0 0 0 1px @primary inset, 0 3px 12px rgba(37, 99, 235, 0.1) !important;
      }
    }

    :deep(.el-input-group__append) {
      background-color: transparent !important;
      border: none !important;
      box-shadow: none !important;
      padding: 0 10px !important;
      display: flex !important;
      align-items: center !important;
      gap: 20px !important;
    }
  }
}

// 响应式设计
@media (max-width: @breakpoint-md) {
  .student-selection-panel {
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

  .student-selection-panel {
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