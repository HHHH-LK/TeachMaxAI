<script setup>
import {ref, onMounted, nextTick} from 'vue'
import Live2DComponent from './Live2DComponent.vue'
import VoiceRecognition from './VoiceRecognition.vue'
import {fileService} from '@/services/api'
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

// 添加等待动画状态
const showThinkingAnimation = ref(false)

// 语音朗读相关状态
const isSpeaking = ref(false)
const speechRate = ref(1.0)
const speechVolume = ref(1.0)
const currentSpeechUtterance = ref(null)
const isVoiceEnabled = ref(true)
const speechQueue = ref([])
const isProcessingSpeech = ref(false)

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
  showThinkingAnimation.value = true; // 显示等待动画

  let assistantMsg = {
    type: 'assistant',
    content: '', // 初始为空
    timestamp: new Date().toLocaleTimeString(),
    isThinking: true // 标记为思考状态
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

        // 收到第一个有效内容时，隐藏等待动画
        if (chunk !== '🤔 正在思考...' && showThinkingAnimation.value) {
          showThinkingAnimation.value = false;
          assistantMsg.isThinking = false;
        }

        // 如果是"正在思考..."状态，保持等待动画
        if (chunk === '🤔 正在思考...') {
          assistantMsg.content = '';
          assistantMsg.isThinking = true;
        } else {
          // 逐字处理：如果遇到 ¥ 符号，替换为两次换行并删除原符号
          if (chunk === '¥') {
            currentStreamContent += '\n\n'; // 替换为两次换行
          } else {
            currentStreamContent += chunk; // 正常累积字符
          }

          // 实时显示的内容
          assistantMsg.content = currentStreamContent;
          assistantMsg.isThinking = false;

          // 语音朗读（流式输出时实时朗读）
          if (isVoiceEnabled.value && chunk.trim() && !chunk.includes('¥')) {
            // 对于流式输出，我们只朗读完整的句子或短语
            const cleanChunk = chunk.replace(/[。！？.!?]/g, '');
            if (cleanChunk.length > 2) { // 只朗读有意义的文本片段
              speakText(cleanChunk);
            }
          }
        }

        // 仅更新Vue响应式数据以显示累积的文本
        const lastIndex = messages.value.length - 1;
        messages.value.splice(lastIndex, 1, { ...assistantMsg });
        scrollToBottom();
      },
      onComplete: () => {
        console.log('✅ 消息发送完成');
        showThinkingAnimation.value = false; // 隐藏等待动画

        // ====== 最终的换行处理逻辑 START ======
        const punctuationRegex = /[。！？.!?]/g; // 匹配中文句号、英文句号、中文问号、英文问号、中文叹号、英文叹号

        // 只在最终内容上执行一次替换
        let formattedContent = currentStreamContent.replace(punctuationRegex, (match) => {
          return match + '\n';
        });

        // 显示最终内容
        assistantMsg.content = formattedContent;
        assistantMsg.isThinking = false;
        // ====== 最终的换行处理逻辑 END ======

        // 完成时朗读完整内容
        if (isVoiceEnabled.value && formattedContent.trim()) {
          speakText(formattedContent);
        }

        isResponding.value = false;
        const lastIndex = messages.value.length - 1;
        messages.value.splice(lastIndex, 1, { ...assistantMsg }); // 再次更新以显示最终格式化后的内容
        scrollToBottom();
      },
      onError: (err) => {
        console.error('❌ 发送错误:', err);
        showThinkingAnimation.value = false; // 隐藏等待动画
        assistantMsg.content = `[错误: ${err.message}]`;
        assistantMsg.isThinking = false;
        const lastIndex = messages.value.length - 1;
        messages.value.splice(lastIndex, 1, { ...assistantMsg, type: 'error' });
        isResponding.value = false;
      }
    });

  } catch (error) {
    console.error('💥 发送失败:', error);
    showThinkingAnimation.value = false; // 隐藏等待动画
    assistantMsg.content = `[发送失败: ${error.message}]`;
    assistantMsg.isThinking = false;
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

// 语音朗读方法
const speakText = (text) => {
  if (!isVoiceEnabled.value || !text.trim()) return;

  // 将文本添加到队列
  speechQueue.value.push(text);

  // 如果没有在处理语音，开始处理
  if (!isProcessingSpeech.value) {
    processSpeechQueue();
  }
};

const processSpeechQueue = () => {
  if (speechQueue.value.length === 0 || !isVoiceEnabled.value) {
    isProcessingSpeech.value = false;
    return;
  }

  isProcessingSpeech.value = true;
  const text = speechQueue.value.shift();

  const utterance = new SpeechSynthesisUtterance(text);
  utterance.rate = speechRate.value;
  utterance.volume = speechVolume.value;
  utterance.lang = 'zh-CN';

  utterance.onstart = () => {
    isSpeaking.value = true;
  };

  utterance.onend = () => {
    isSpeaking.value = false;
    currentSpeechUtterance.value = null;
    // 继续处理队列中的下一个
    setTimeout(() => {
      processSpeechQueue();
    }, 100);
  };

  utterance.onerror = (event) => {
    console.error('语音朗读错误:', event);
    isSpeaking.value = false;
    currentSpeechUtterance.value = null;
    // 继续处理队列中的下一个
    setTimeout(() => {
      processSpeechQueue();
    }, 100);
  };

  currentSpeechUtterance.value = utterance;
  speechSynthesis.speak(utterance);
};

const stopSpeaking = () => {
  if (currentSpeechUtterance.value) {
    speechSynthesis.cancel();
    isSpeaking.value = false;
    currentSpeechUtterance.value = null;
  }
  // 清空语音队列
  speechQueue.value = [];
  isProcessingSpeech.value = false;
};

const toggleVoice = () => {
  isVoiceEnabled.value = !isVoiceEnabled.value;
  if (!isVoiceEnabled.value) {
    stopSpeaking();
  }
};

const updateSpeechRate = (rate) => {
  speechRate.value = rate;
  if (isSpeaking.value && currentSpeechUtterance.value) {
    stopSpeaking();
    speakText(currentSpeechUtterance.value.text);
  }
};

const updateSpeechVolume = (volume) => {
  speechVolume.value = volume;
  if (isSpeaking.value && currentSpeechUtterance.value) {
    stopSpeaking();
    speakText(currentSpeechUtterance.value.text);
  }
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
        <!-- 语音控制区域 -->
        <div class="voice-controls">
          <div class="control-header">
            <h4>语音设置</h4>
            <button @click="toggleVoice" class="voice-toggle-btn" :class="{ 'active': isVoiceEnabled }">
              <span class="icon">{{ isVoiceEnabled ? '🔊' : '🔇' }}</span>
            </button>
          </div>

          <div class="control-item">
            <label>语速</label>
            <input type="range" min="0.5" max="2.0" step="0.1"
                   v-model="speechRate" @input="updateSpeechRate($event.target.value)"
                   class="control-slider">
            <span class="control-value">{{ speechRate }}</span>
          </div>

          <div class="control-item">
            <label>音量</label>
            <input type="range" min="0" max="1" step="0.1"
                   v-model="speechVolume" @input="updateSpeechVolume($event.target.value)"
                   class="control-slider">
            <span class="control-value">{{ speechVolume }}</span>
          </div>
          <button v-if="isSpeaking" @click="stopSpeaking" class="action-btn stop-voice-btn">
            <span class="icon">⏹️</span>
            <span class="btn-text">停止朗读</span>
          </button>
        </div>

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
          <!-- 等待动画组件 -->
          <div v-if="msg.isThinking" class="thinking-animation">
            <div class="thinking-container">
              <!-- 粒子效果 -->
              <div class="particles">
                <div class="particle particle-1"></div>
                <div class="particle particle-2"></div>
                <div class="particle particle-3"></div>
                <div class="particle particle-4"></div>
                <div class="particle particle-5"></div>
                <div class="particle particle-6"></div>
              </div>
              <!-- 旋转齿轮 -->
              <div class="gear-container">
                <div class="gear gear-1">
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                </div>
                <div class="gear gear-2">
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                  <div class="gear-tooth"></div>
                </div>
              </div>
              <!-- 波浪效果 -->
              <div class="wave-container">
                <div class="wave wave-1"></div>
                <div class="wave wave-2"></div>
                <div class="wave wave-3"></div>
              </div>
              <!-- 中心光晕 -->
              <div class="center-glow"></div>
            </div>
            <div class="thinking-text">小V正在思考中...</div>
            <!-- 进度条 -->
            <div class="progress-container">
              <div class="progress-bar-thinking"></div>
            </div>
          </div>
          <!-- 正常消息内容 -->
          <div v-else class="message-content">{{ msg.content }}</div>
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

/* 语音控制样式 */
.voice-controls {
  margin-bottom: 20px;
  padding: 15px;
  background: rgba(76, 175, 80, 0.05);
  border-radius: 8px;
  border: 1px solid rgba(76, 175, 80, 0.2);
}

.control-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.control-header h4 {
  margin: 0;
  color: var(--text-dark);
  font-size: 14px;
}

.voice-toggle-btn {
  background: none;
  border: 1px solid var(--light-green);
  border-radius: 4px;
  padding: 6px 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.voice-toggle-btn.active {
  background: var(--primary-green);
  color: white;
}

.voice-toggle-btn:hover {
  background: var(--light-green);
}

.control-item {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.control-item label {
  font-size: 12px;
  color: var(--text-dark);
  font-weight: 500;
}

.control-slider {
  width: 100%;
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  outline: none;
  -webkit-appearance: none;
}

.control-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  background: var(--primary-green);
  border-radius: 50%;
  cursor: pointer;
}

.control-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  background: var(--primary-green);
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

.control-value {
  font-size: 11px;
  color: #6b7280;
  text-align: right;
}

.stop-voice-btn {
  background: #ef4444;
  color: white;
  border: none;
  margin-top: 10px;
}

.stop-voice-btn:hover {
  background: #dc2626;
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

/* 等待动画样式 */
.thinking-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.05), rgba(76, 175, 80, 0.1));
  border-radius: 16px;
  border: 1px solid rgba(76, 175, 80, 0.2);
  box-shadow: 0 8px 32px rgba(76, 175, 80, 0.1);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

.thinking-animation::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(76, 175, 80, 0.1), transparent);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

.thinking-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 80px;
}

/* 粒子效果 */
.particles {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: var(--primary-green);
  border-radius: 50%;
  animation: particle-float 3s ease-in-out infinite;
}

.particle-1 {
  top: 10%;
  left: 20%;
  animation-delay: 0s;
}

.particle-2 {
  top: 20%;
  right: 15%;
  animation-delay: 0.5s;
}

.particle-3 {
  bottom: 30%;
  left: 10%;
  animation-delay: 1s;
}

.particle-4 {
  bottom: 20%;
  right: 25%;
  animation-delay: 1.5s;
}

.particle-5 {
  top: 50%;
  left: 5%;
  animation-delay: 2s;
}

.particle-6 {
  top: 40%;
  right: 5%;
  animation-delay: 2.5s;
}

@keyframes particle-float {
  0%, 100% {
    transform: translateY(0px) scale(1);
    opacity: 0.3;
  }
  50% {
    transform: translateY(-20px) scale(1.2);
    opacity: 1;
  }
}

/* 中心光晕 */
.center-glow {
  position: absolute;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(76, 175, 80, 0.3) 0%, transparent 70%);
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
}

/* 齿轮动画 */
.gear-container {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  width: 100%;
  height: 100%;
}

.gear {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gear-1 {
  animation: rotate 3s linear infinite;
  position: absolute;
  top: 10px;
  left: -40px;
}

.gear-2 {
  animation: rotate-reverse 2s linear infinite;
  position: absolute;
  top: 20px;
  right: -40px;
}

.gear-tooth {
  position: absolute;
  width: 4px;
  height: 12px;
  background: white;
  border-radius: 2px;
  transform-origin: 50% 20px;
}

.gear-1 .gear-tooth:nth-child(1) { transform: rotate(0deg) translateY(-20px); }
.gear-1 .gear-tooth:nth-child(2) { transform: rotate(45deg) translateY(-20px); }
.gear-1 .gear-tooth:nth-child(3) { transform: rotate(90deg) translateY(-20px); }
.gear-1 .gear-tooth:nth-child(4) { transform: rotate(135deg) translateY(-20px); }
.gear-1 .gear-tooth:nth-child(5) { transform: rotate(180deg) translateY(-20px); }
.gear-1 .gear-tooth:nth-child(6) { transform: rotate(225deg) translateY(-20px); }
.gear-1 .gear-tooth:nth-child(7) { transform: rotate(270deg) translateY(-20px); }
.gear-1 .gear-tooth:nth-child(8) { transform: rotate(315deg) translateY(-20px); }

.gear-2 .gear-tooth:nth-child(1) { transform: rotate(0deg) translateY(-18px); }
.gear-2 .gear-tooth:nth-child(2) { transform: rotate(60deg) translateY(-18px); }
.gear-2 .gear-tooth:nth-child(3) { transform: rotate(120deg) translateY(-18px); }
.gear-2 .gear-tooth:nth-child(4) { transform: rotate(180deg) translateY(-18px); }
.gear-2 .gear-tooth:nth-child(5) { transform: rotate(240deg) translateY(-18px); }
.gear-2 .gear-tooth:nth-child(6) { transform: rotate(300deg) translateY(-18px); }

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes rotate-reverse {
  from { transform: rotate(0deg); }
  to { transform: rotate(-360deg); }
}

/* 波浪效果 */
.wave-container {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 4px;
}

.wave {
  width: 4px;
  height: 20px;
  background: linear-gradient(to top, var(--primary-green), var(--dark-green));
  border-radius: 2px;
  animation: wave 1.5s ease-in-out infinite;
}

.wave-1 {
  animation-delay: 0s;
}

.wave-2 {
  animation-delay: 0.2s;
}

.wave-3 {
  animation-delay: 0.4s;
}

@keyframes wave {
  0%, 100% {
    height: 20px;
    opacity: 0.6;
  }
  50% {
    height: 40px;
    opacity: 1;
  }
}

.thinking-text {
  font-size: 14px;
  color: var(--text-dark);
  font-weight: 500;
  opacity: 0.8;
  animation: thinking-pulse 2s ease-in-out infinite;
  text-align: center;
}

@keyframes thinking-pulse {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}

/* 进度条样式 */
.progress-container {
  width: 100%;
  height: 4px;
  background: rgba(76, 175, 80, 0.2);
  border-radius: 2px;
  overflow: hidden;
  margin-top: 8px;
}

.progress-bar-thinking {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-green), var(--dark-green));
  border-radius: 2px;
  animation: progress-loading 2s ease-in-out infinite;
  box-shadow: 0 0 8px rgba(76, 175, 80, 0.5);
}

@keyframes progress-loading {
  0% {
    width: 0%;
    opacity: 0.8;
  }
  50% {
    width: 70%;
    opacity: 1;
  }
  100% {
    width: 100%;
    opacity: 0.8;
  }
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
