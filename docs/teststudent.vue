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
                <span v-if="connectionStatusMap.get(selectedStudent.id)"
                      :class="'connection-indicator ' + connectionStatusMap.get(selectedStudent.id)">
                  {{ getConnectionStatusText(connectionStatusMap.get(selectedStudent.id)) }}
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
import { ref, watch, nextTick, computed, onUnmounted, onMounted } from 'vue';
import { ElContainer, ElAside, ElMain, ElAvatar, ElScrollbar, ElInput, ElButton, ElEmpty, ElTag, ElDropdown, ElDropdownMenu, ElDropdownItem, ElIcon, ElSkeleton, ElDescriptions, ElDescriptionsItem } from 'element-plus';
import {
  Search as ElIconSearch,
  Promotion as ElIconPromotion,
  Paperclip as ElIconPaperclip,
  MoreFilled,
  Refresh as ElIconRefresh,
} from '@element-plus/icons-vue';
import { useStudentChat } from '@/composables/useStudentChat.js';
import teacherAvatar from '@/assets/teacher-avatar.png';
import {teacherService, isOnline, studentService} from "@/services/api.js";
import chatSSEService from '@/services/chatSSEService.js';
import teacherChatApiService from '@/services/teacherChatApiService.js';
import { getCurrentUser } from '@/utils/userUtils.js';
import { ElMessage } from 'element-plus';

// 学生数据状态
const students = ref([]);
const loading = ref(false);

const teacherId = String(getCurrentUser().id)   // 当前教师 id（只连一次）

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

// 获取学生在线状态
const fetchStudentOnlineStatus = async (studentId) => {
  try {
    const res = await isOnline(studentId);
    // console.log(`获取学生${studentId}在线状态:`, res);

    if (res.data) {
      return res.data.data === true;
    } else {
      // console.warn(`获取学生${studentId}在线状态失败`);
      return false;
    }
  } catch (error) {
    // console.error(`获取学生${studentId}在线状态出错:`, error);
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
                // console.log(`学号${student.studentNumber}对应的用户ID:`, userId);
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
            }
          })
      );

      // 批量获取在线状态
      const onlineStatusPromises = studentsWithUserIds.map(async (student) => {
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

// 刷新所有学生数据（课程1和课程2）
const refreshAllStudents = async () => {
  try {
    loading.value = true;

    // 同时获取两个课程的学生数据和未读消息数据
    const [course1Students, course2Students] = await Promise.all([
      fetchClassStudents(1),
      fetchClassStudents(2),
      fetchUnreadMessages() // 获取未读消息数据
    ]);

    // 合并两个课程的学生数据
    const allStudents = [...course1Students, ...course2Students];

    if (allStudents.length > 0) {
      // console.log('加载的学生数据:', allStudents);
      students.value = allStudents;
      ElMessage.success(`成功加载 ${allStudents.length} 名学生数据`);
    } else {
      ElMessage.warning('获取学生数据失败，使用模拟数据');
      students.value = mockStudents.value;
    }
  } catch (error) {
    console.error('获取所有学生数据出错:', error);
    ElMessage.warning('网络错误，使用模拟数据');
    students.value = mockStudents.value;
  } finally {
    loading.value = false;
  }
};



// 页面加载时获取学生数据
refreshAllStudents();

onMounted(async () => {
  await refreshAllStudents()            // 拉学生列表
  await chatSSEService.connect(teacherId, {
    onConnected: () => console.log('教师端 SSE 已连接'),
    onChatMessage: handleIncomingMessage,
    onError: (e) => console.error('教师端 SSE 错误', e)
  })
})

// 模拟学生数据作为备用
const mockStudents = ref([
  {
    id: 16,
    name: '张小明',
    description: '武术特长生，对传统文化有浓厚兴趣。',
    avatar: 'https://cdn.pixabay.com/photo/2020/07/01/12/58/icon-5359553_1280.pngs',
    isOnline: true,
    tags: ['Java', '数据结构与算法'],
  },
  {
    id: 2,
    name: '张伟',
    description: '计算机科学专业，擅长编程和算法。',
    avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epicture.pngs',
    isOnline: false,
    tags: ['计算机', '编程', '算法'],
  },
  {
    id: 3,
    name: '王芳',
    description: '艺术设计学院学生，热爱绘画和创意。',
    avatar: 'https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpegs',
    isOnline: false,
    tags: ['艺术', '设计', '绘画'],
  },
]);

// 状态
const selectedStudent = ref(null);
const chatMessages = ref({});
const currentChatMessages = ref([]);
const unreadMap = ref({});
const isConnecting = ref(false);
const connectionStatus = ref('disconnected');
const typingStatus = ref('');
const filteredStudents = computed(() => students.value);
const onlineStudents = computed(() => students.value.filter(s => s.isOnline));

// SSE连接管理
const sseConnections = ref(new Map()); // 存储每个学生的SSE连接
const connectionStatusMap = ref(new Map()); // 存储每个学生的连接状态

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

// 方法
const selectStudent = async (student) => {
  console.log('选中的学生对象:', student);
  console.log('学生ID:', student?.id);
  console.log('学生姓名:', student?.name);

  selectedStudent.value = student;

  // 如果该学生没有聊天记录，初始化一个默认的欢迎消息
  if (!chatMessages.value[student.id]) {
    chatMessages.value[student.id] = [
      {
        sender: 'teacher',
        content: `你好，${student.name}！我是你的老师，有什么问题可以随时问我。`,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
    ];
  }

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

  // 建立与该学生的SSE连接
  // await connectToStudentSSE(student.id);
};

// 建立与学生的SSE连接
const connectToStudentSSE = async (studentId) => {
  try {
    // 如果已经连接，先断开
    if (sseConnections.value.has(studentId)) {
      // disconnectFromStudentSSE(studentId);
    }

    // 获取当前教师ID
    const currentUser = getCurrentUser();
    const teacherId = currentUser.id;

    if (!teacherId) {
      throw new Error('无法获取教师用户ID');
    }

    console.log(`正在建立与学生${studentId}的SSE连接，教师ID: ${teacherId}...`);
    connectionStatusMap.value.set(studentId, 'connecting');

    await chatSSEService.connect(teacherId.toString(), {
      onConnected: (data) => {
        console.log(`与学生${studentId}的SSE连接已建立:`, data);
        connectionStatusMap.value.set(studentId, 'connected');
        sseConnections.value.set(studentId, true);
      },
      onChatMessage: (data) => {
        console.log(`收到来自学生${studentId}的消息:`, data);
        handleIncomingMessage(studentId, data);
      },
      onError: (error) => {
        console.error(`与学生${studentId}的SSE连接错误:`, error);
        connectionStatusMap.value.set(studentId, 'error');
        sseConnections.value.delete(studentId);
      },
      onReconnect: () => {
        console.log(`正在重连学生${studentId}...`);
        connectionStatusMap.value.set(studentId, 'reconnecting');
      }
    });
  } catch (error) {
    console.error(`建立与学生${studentId}的SSE连接失败:`, error);
    connectionStatusMap.value.set(studentId, 'error');
    ElMessage.error(`无法连接到学生${studentId}`);
  }
};

// 断开与学生的SSE连接
const disconnectFromStudentSSE = (studentId) => {
  try {
    // 获取当前教师ID
    const currentUser = getCurrentUser();
    const teacherId = currentUser.id;

    if (teacherId) {
      chatSSEService.disconnect(teacherId.toString());
    }
    sseConnections.value.delete(studentId);
    connectionStatusMap.value.delete(studentId);
    console.log(`已断开与学生${studentId}的SSE连接`);
  } catch (error) {
    console.error(`断开与学生${studentId}的SSE连接失败:`, error);
  }
};

// 处理收到的消息
function handleIncomingMessage(data) {
  // data.senderUserId = 学生 id
  // data.receiverUserId = 教师 id
  const studentId = String(data.senderUserId)
  const msg = {
    sender: 'student',
    content: data.content || data.message,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  pushLocalMessage(studentId, msg)

  // 未读计数
  if (selectedStudent.value?.id !== studentId) {
    unreadMap.value[studentId] = (unreadMap.value[studentId] || 0) + 1
  } else {
    nextTick(scrollToBottom)
  }
}

/* 工具：把消息压入本地 */
function pushLocalMessage(studentId, msg) {
  if (!chatMessages.value[studentId]) chatMessages.value[studentId] = []
  chatMessages.value[studentId].push(msg)
  if (selectedStudent.value?.id === studentId) {
    currentChatMessages.value = chatMessages.value[studentId]
  }
}

// 发送消息到后端
const sendMessageToBackend = async (studentId, content) => {
  try {
    // 获取会话ID
    const conversationId = conversationIds.value.get(studentId);
    if (!conversationId) {
      throw new Error('未找到会话ID，请先建立连接');
    }

    const result = await teacherChatApiService.sendMessage(studentId, content, 'text', null, conversationId);
    return result;
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
  console.log("收到消息教师消息" + newMessage)

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
      student.courseName.toLowerCase().includes(query)
  );
});

// 新消息内容
const newMessage = ref('');

// 滚动条引用
const chatMessagesScrollbar = ref(null);

// 历史对话详情对话框
const showHistoryDialog = ref(false);

// 监听选中的学生变化，更新当前对话消息
watch(selectedStudent, async (newVal, oldVal) => {
  if (newVal) {
    currentChatMessages.value = chatMessages.value[newVal.id] || [];
    // 切换学生后，滚动到底部
    nextTick(() => {
      scrollToBottom();
    });
  } else {
    currentChatMessages.value = [];
  }

  // 断开旧学生的连接，连接新学生
  if (oldVal && oldVal.id !== newVal?.id) {
    // disconnectFromStudentSSE(oldVal.id);
  }
}, { immediate: true });

// 发送消息的包装函数
const handleSendMessage = async () => {
  const content = newMessage.value.trim()
  if (!content || !selectedStudent.value) return

  const studentId = selectedStudent.value.id
  const msg = {
    sender: 'teacher',
    content,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  pushLocalMessage(studentId, msg)
  newMessage.value = ''
  nextTick(scrollToBottom)

  try {
    await teacherChatApiService.sendMessage(studentId, content, 'text', null,
        conversationIds.value.get(studentId))
  } catch (e) {
    ElMessage.error('发送失败：' + e.message)
  }
}

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
onUnmounted(() => chatSSEService.disconnectAll())
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

.student-list-scrollbar {
  flex-grow: 1;
  height: calc(100% - 150px);
}

.student-list {
  padding-right: 15px;
}

.loading-container {
  padding: 20px;
  /deep/ .el-skeleton {
    .el-skeleton__item {
      margin-bottom: 15px;
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
    .student-description,
    .student-tags .el-tag {
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

.no-student-selected {
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

  .message-list {
    display: flex;
    flex-direction: column;

    .message-item {
      width: 100%;

      .message-content {
        max-width: 80%;

        .message-bubble {
          max-width: 100%;

          p {
            word-break: break-word;
            overflow-wrap: break-word;
            white-space: pre-wrap;
          }
        }
      }
    }
  }
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
  box-shadow: 0 0 4px rgba(0,0,0,0.15);
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