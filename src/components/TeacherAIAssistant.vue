<script setup>
import {ref, onMounted, watch, nextTick} from 'vue'
import Live2DComponent from './Live2DComponent.vue'
import VoiceRecognition from './VoiceRecognition.vue'
import {chatService, fileService} from '@/services/api'
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.min.css';


import { useSSEChat } from '@/services/useSSEChat';


const isOpen = ref(false)
const messages = ref([])
const newMessage = ref('')
const chatHistory = ref([])
const isAnimating = ref(true)
const ballPosition = ref({ x: 20, y: 20 })
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const modelPath = ref('/凯尔希live2d/凯尔希.model3.json')
const live2d = ref(null)
const live2dConfig = ref({
  width: 450,
  height: 400,
  scale: 0.128,
  position: { x: 100, y: 50 }
})

const toggleAssistant = () => {
  if (!isDragging.value) {
    isOpen.value = !isOpen.value
    if (isOpen.value && live2d.value) {
      live2d.value.restoreLive2D()
    }
  }
}

const startDrag = (event) => {
  isDragging.value = true
  const ball = event.target.closest('.floating-ball')
  const rect = ball.getBoundingClientRect()
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  }
}

const onDrag = (event) => {
  if (isDragging.value) {
    event.preventDefault()
    const x = event.clientX - dragOffset.value.x
    const y = event.clientY - dragOffset.value.y
    ballPosition.value = { x, y }
  }
}

const stopDrag = () => {
  isDragging.value = false
}

const deleteHistory = (index) => {
  chatHistory.value.splice(index, 1)
}

const clearAllHistory = () => {
  chatHistory.value = []
}

const uploadProgress = ref(0);
const isUploading = ref(false);

const handleFileUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  isUploading.value = true;
  uploadProgress.value = 0;

  try {
    const result = await fileService.uploadFile(file, (progress) => {
      uploadProgress.value = progress;
    });

    messages.value.push({
      type: 'user',
      content: `成功上传文件: ${file.name}`,
      timestamp: new Date().toLocaleTimeString(),
      fileInfo: {
        name: file.name,
        size: file.size,
        url: result.data.url
      }
    });
  } catch (error) {
    const errorMsg = error.response?.data?.message || '文件上传失败';
    messages.value.push({
      type: 'error',
      content: errorMsg,
      timestamp: new Date().toLocaleTimeString()
    });
  } finally {
    isUploading.value = false;
    event.target.value = '';
  }
};

// const { sendSSEMessage } = useSSEChat();

const isResponding = ref(false);

// 处理语音识别的文本
const handleVoiceText = (text) => {
  newMessage.value = text
}

onMounted(() => {
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
})

let currentStreamContent = '';

const sendMessage = async () => {
  if (!newMessage.value.trim() || isResponding.value) return;

  const content = newMessage.value;
  newMessage.value = '';
  const userId = 16;

  console.log('=== 开始发送消息 ===');
  console.log('问题:', content);

  messages.value.push({
    type: 'user',
    content: content,
    timestamp: new Date().toLocaleTimeString()
  });

  isResponding.value = true;

  let assistantMsg = {
    type: 'assistant',
    content: '', // 初始为空
    timestamp: new Date().toLocaleTimeString()
  };
  messages.value.push(assistantMsg);

  // 重置临时流式内容
  currentStreamContent = '';

  const scrollToBottom = () => {
    nextTick(() => {
      const container = document.querySelector('.messages');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    });
  };

  scrollToBottom();

  try {
    const { sendSSEMessage } = useSSEChat();

    await sendSSEMessage({
      userId,
      question: content,
      onMessage: (chunk) => {
        console.log('📨 收到消息块:', chunk);

        // 如果是"正在思考..."状态，直接显示
        if (chunk === '🤔 正在思考...') {
          assistantMsg.content = chunk; // 仅在思考时显示这个
        } else {
          // 逐字处理：如果遇到 ¥ 符号，替换为两次换行并删除原符号
          if (chunk === '¥') {
            currentStreamContent += '\n\n'; // 替换为两次换行
          } else {
            currentStreamContent += chunk; // 正常累积字符
          }

          // 实时显示的内容可以简单地就是累积的内容，或者加上一个指示符
          assistantMsg.content = currentStreamContent + '...'; // 可以加个...表示还在输入
        }

        // 仅更新Vue响应式数据以显示累积的文本
        const lastIndex = messages.value.length - 1;
        messages.value.splice(lastIndex, 1, { ...assistantMsg });
        scrollToBottom();
      },
      onComplete: () => {
        console.log('✅ 消息发送完成');
        // ====== 最终的换行处理逻辑 START ======
        const punctuationRegex = /[。！？.!?]/g; // 匹配中文句号、英文句号、中文问号、英文问号、中文叹号、英文叹号

        // 只在最终内容上执行一次替换
        let formattedContent = currentStreamContent.replace(punctuationRegex, (match) => {
          return match + '\n';
        });

        // 移除显示时的 "..." 标记，显示最终内容
        assistantMsg.content = formattedContent;
        // ====== 最终的换行处理逻辑 END ======

        isResponding.value = false;
        const lastIndex = messages.value.length - 1;
        messages.value.splice(lastIndex, 1, { ...assistantMsg }); // 再次更新以显示最终格式化后的内容
        scrollToBottom();
      },
      onError: (err) => {
        console.error('❌ 发送错误:', err);
        assistantMsg.content = `[错误: ${err.message}]`;
        const lastIndex = messages.value.length - 1;
        messages.value.splice(lastIndex, 1, { ...assistantMsg, type: 'error' });
        isResponding.value = false;
      }
    });

  } catch (error) {
    console.error('💥 发送失败:', error);
    assistantMsg.content = `[发送失败: ${error.message}]`;
    const lastIndex = messages.value.length - 1;
    messages.value.splice(lastIndex, 1, { ...assistantMsg, type: 'error' });
    isResponding.value = false;
  }

  console.log('=== 消息发送流程结束 ===');
};

const startNewChat = () => {
  if (messages.value.length > 0) {
    const firstQuestion = messages.value.find(m => m.type === 'user')?.content || '新对话';
    chatHistory.value.push({
      title: firstQuestion.substring(0, 20), // 截取前20个字符
      messages: [...messages.value],
      timestamp: new Date().toISOString()
    });
    messages.value = [];
  }
};

const loadChat = (index) => {
  // 确保索引有效
  if (index < 0 || index >= chatHistory.value.length) return;
  // 获取要加载的历史对话
  const history = chatHistory.value[index];
  // 先清空当前消息
  messages.value = [];
  // 使用nextTick确保DOM更新后再加载新消息
  nextTick(() => {
    // 深拷贝历史消息以避免引用问题
    messages.value = JSON.parse(JSON.stringify(history.messages));
    // 自动滚动到底部
    setTimeout(() => {
      const messagesContainer = document.querySelector('.messages');
      if (messagesContainer) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
      }
    }, 50);
  });
};

</script>

<template>
  <div class="floating-ball"
       @click="toggleAssistant"
       @mousedown="startDrag"
       :style="{
         left: `${ballPosition.x}px`,
         top: `${ballPosition.y}px`,
         right: 'auto',
         bottom: 'auto'
       }"
       :class="{ 'open': isOpen }">
    <div class="avatars" :class="{ 'animating': isAnimating }">
      🤖
    </div>
  </div>

  <div class="assistant-panel" :class="{ 'open': isOpen }">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>小T</h3>
      </div>
      <div class="sidebar-actions">
        <button @click="startNewChat" class="action-btn new-chat-btn">
          <span class="icon">＋</span>
          <span class="btn-text">新对话</span>
        </button>
      </div>
      <div class="chat-history">
        <div class="history-header">
          <h4>历史对话</h4>
          <button @click="clearAllHistory" class="clear-btn" title="清空历史">
            <span class="icon">🗑️</span>
          </button>
        </div>
        <div v-for="(chat, index) in chatHistory" :key="index" class="history-item">
          <span @click="loadChat(index)" class="history-title">
            {{ chat.title }}
            <span class="timestamp">
              {{ new Date(chat.timestamp).toLocaleDateString() }}
            </span>
          </span>
          <button class="delete-btn" @click="deleteHistory(index)">🗑️</button>
        </div>
      </div>
    </div>

    <div class="chat-container">
      <div class="live2d-container">
        <Live2DComponent
            ref="live2d"
            :model-path="modelPath"
            :width="live2dConfig.width"
            :height="live2dConfig.height"
            :scale="live2dConfig.scale"
            :position="live2dConfig.position"
        />
      </div>

      <div class="messages">
        <div v-for="(msg, index) in messages" :key="index"
             :class="['message', msg.type]">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ msg.timestamp }}</div>
        </div>
      </div>

      <div class="input-area">
        <div v-if="isUploading" class="upload-progress">
          <div class="progress-bar" :style="{ width: uploadProgress + '%' }"></div>
          <span>{{ uploadProgress }}%</span>
        </div>
        <input type="file" id="file-upload" @change="handleFileUpload" style="display: none">
        <label for="file-upload" class="upload-btn">📎</label>
        
        <!-- 语音识别组件 -->
        <VoiceRecognition @text-recognized="handleVoiceText" />
        
        <input v-model="newMessage" @keyup.enter="sendMessage"
               placeholder="输入消息..." type="text">
        <button @click="sendMessage">发送</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-progress {
  width: 100%;
  height: 20px;
  background: #eee;
  border-radius: 10px;
  margin-top: 10px;
  position: relative;
}

.progress-bar {
  height: 100%;
  background: var(--primary-green);
  border-radius: 10px;
  transition: width 0.3s ease;
}

.upload-progress span {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
}

.floating-ball {
  position: fixed;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(145deg, var(--light-green), var(--primary-green));
  cursor: move;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 9999;
  transition: transform 0.3s ease;
  user-select: none;
  touch-action: none;
  right: 20px;
  bottom: 20px;
  top: auto;
  left: auto;
}

.floating-ball:hover {
  transform: scale(1.1);
  background: var(--primary-green);
}

.floating-ball.open {
  transform: scale(0.8);
}

.avatars {
  font-size: 30px;
}

.avatars.animating {
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.assistant-panel {
  position: fixed;
  top: 0;
  right: -38vw; /* 初始位置在屏幕外 */
  width: 38vw; /* 40%屏幕宽度 */
  height: 100vh;
  background: white;
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.1);
  transition: right 0.3s ease;
  z-index: 3344;
  display: flex;
}

.assistant-panel.open {
  right: 0;
}

.sidebar {
  width: 30%; /* 侧边栏宽度比例 */
  min-width: 150px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--light-green);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: center;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  color: var(--text-dark);
}

.sidebar-actions {
  padding: 20px;
}

.action-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: white;
  color: #374151;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f3f4f6;
}

.new-chat-btn {
  background: var(--primary-green);
  color: white;
  border: none;
}

.new-chat-btn:hover {
  background: var(--dark-green);
}

.btn-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.history-header h4 {
  margin: 0;
  color: #6b7280;
}

.clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.clear-btn:hover {
  opacity: 1;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: white;
  border: 1px solid var(--light-green);
  border-radius: 6px;
  transition: all 0.2s;
  min-width: 0; /* 允许标题截断 */
}

.history-item:hover {
  background: var(--light-green);
}

.history-title {
  max-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  cursor: pointer;
  flex: 1;
  flex-direction: column;
}

.timestamp {
  font-size: 0.8em;
  color: #666;
  margin-top: 2px;
}

.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  opacity: 0.5;
  transition: opacity 0.2s;
}

.delete-btn:hover {
  opacity: 1;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: white;
  overflow: hidden;
}

/* live2d */
.live2d-container {
  width: 100%;
  max-height: 330px;
  margin: 0 auto;
  overflow: hidden;
  position: relative;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  height: calc(100vh - 30vh - 80px); /* 动态计算高度 */
}

.message {
  margin-bottom: 15px;
  max-width: 80%;
  padding: 10px;
  border-radius: 10px;
}

.message-content {
  white-space: pre-wrap !important;
  word-break: break-word;
  overflow-wrap: break-word;
  line-height: 1.5;
}


.message.user {
  margin-left: auto;
  background: var(--chat-user);
  color: var(--text-light);
}

.message.assistant {
  margin-right: auto;
  background: var(--chat-ai);
  color: var(--text-dark);
}

/* 流式输出 */
.message.assistant-streaming {
  position: relative;
}

.message.assistant-streaming::after {
  content: '...';
  animation: typing 1s infinite;
  position: absolute;
  right: -20px;
  bottom: 2px;
}

@keyframes typing {
  0%, 100% { opacity: 0.3 }
  50% { opacity: 1 }
}

.message-time {
  font-size: 0.8em;
  margin-top: 5px;
  opacity: 0.7;
}

.input-area {
  padding: 20px;
  border-top: 1px solid var(--light-green);
  display: flex;
  gap: 10px;
  position: relative;
}

.input-area input[type="text"] {
  flex: 1;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
}

.input-area button {
  padding: 12px 24px;
  background: var(--primary-green);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.input-area button:hover {
  background: var(--dark-green);
}

.upload-btn {
  padding: 12px;
  cursor: pointer;
  font-size: 20px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.upload-btn:hover {
  opacity: 1;
}

.icon {
  font-size: 16px;
}
</style>
