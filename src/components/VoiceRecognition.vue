<template>
  <div class="voice-recognition">
    <!-- 语音识别按钮 -->
    <button 
      @click="toggleRecording" 
      class="voice-btn"
      :class="{ 'recording': isRecording }"
      :title="isRecording ? '点击停止录音' : '点击开始语音输入'"
      :disabled="!isSupported"
    >
      <span v-if="!isRecording">🎤</span>
      <span v-else class="recording-indicator">🔴</span>
    </button>
    
    <!-- 语音识别状态显示 -->
    <div v-if="isListening" class="voice-status">
      <span class="listening-text">正在听...</span>
      <span v-if="speechText" class="speech-text">{{ speechText }}</span>
    </div>
    
    <!-- 不支持语音识别的提示 -->
    <div v-if="!isSupported" class="unsupported-tip">
      您的浏览器不支持语音识别功能
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  onTextRecognized: {
    type: Function,
    default: (text) => {}
  }
})

const isRecording = ref(false)
const isListening = ref(false)
const isSupported = ref(false)
const recognition = ref(null)
const speechText = ref('')

// 初始化语音识别
const initSpeechRecognition = () => {
  if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    recognition.value = new SpeechRecognition()
    
    recognition.value.continuous = true  // 改为true，允许手动控制
    recognition.value.interimResults = true
    recognition.value.lang = 'zh-CN' // 设置为中文
    
    recognition.value.onstart = () => {
      isRecording.value = true
      isListening.value = true
      speechText.value = ''
      console.log('语音识别已开始')
    }
    
    recognition.value.onresult = (event) => {
      let finalTranscript = ''
      let interimTranscript = ''
      
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript
        if (event.results[i].isFinal) {
          finalTranscript += transcript
        } else {
          interimTranscript += transcript
        }
      }
      
      speechText.value = finalTranscript + interimTranscript
    }
    
    recognition.value.onerror = (event) => {
      console.error('语音识别错误:', event.error)
      isRecording.value = false
      isListening.value = false
      if (event.error === 'no-speech') {
        console.log('没有检测到语音')
        // 不显示alert，让用户可以继续尝试
      } else if (event.error === 'audio-capture') {
        alert('无法访问麦克风，请检查权限设置')
      } else if (event.error === 'aborted') {
        console.log('语音识别被中止')
        // 这是正常的，不需要显示错误
      } else {
        console.error('语音识别失败:', event.error)
        // 只在非用户主动停止的情况下显示错误
        if (event.error !== 'aborted') {
          alert('语音识别失败，请重试')
        }
      }
    }
    
    recognition.value.onend = () => {
      // 只有在手动停止时才重置状态
      if (!isRecording.value) {
        isListening.value = false
        if (speechText.value.trim()) {
          props.onTextRecognized(speechText.value)
          speechText.value = ''
        }
      }
    }
    
    isSupported.value = true
  } else {
    console.warn('浏览器不支持语音识别功能')
    isSupported.value = false
  }
}

// 开始录音
const startRecording = () => {
  if (!recognition.value) {
    alert('您的浏览器不支持语音识别功能')
    return
  }
  
  try {
    // 重置状态
    speechText.value = ''
    recognition.value.start()
  } catch (error) {
    console.error('启动语音识别失败:', error)
    alert('启动语音识别失败，请重试')
    isRecording.value = false
    isListening.value = false
  }
}

// 停止录音
const stopRecording = () => {
  if (recognition.value && isRecording.value) {
    console.log('正在停止录音...')
    isRecording.value = false  // 先设置状态
    isListening.value = false  // 立即更新UI状态
    try {
      recognition.value.stop()
    } catch (error) {
      console.error('停止录音时出错:', error)
    }
  }
}

// 切换录音状态
const toggleRecording = () => {
  if (isRecording.value) {
    console.log('手动停止录音')
    stopRecording()
  } else {
    console.log('开始录音')
    startRecording()
  }
}

onMounted(() => {
  initSpeechRecognition()
})

onUnmounted(() => {
  if (recognition.value && isRecording.value) {
    recognition.value.stop()
  }
})
</script>

<style scoped>
.voice-recognition {
  position: relative;
  display: inline-block;
}

.voice-btn {
  padding: 12px;
  cursor: pointer;
  font-size: 20px;
  background: none;
  border: none;
  border-radius: 50%;
  transition: all 0.3s ease;
  opacity: 0.7;
}

.voice-btn:hover:not(:disabled) {
  opacity: 1;
  background: rgba(0, 0, 0, 0.1);
}

.voice-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.voice-btn.recording {
  background: rgba(255, 0, 0, 0.2);
  animation: pulse 1.5s infinite;
}

.recording-indicator {
  animation: blink 1s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

.voice-status {
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 200px;
  text-align: center;
  z-index: 1000;
}

.listening-text {
  color: #ff6b6b;
  font-weight: bold;
}

.speech-text {
  color: #fff;
  font-style: italic;
  word-break: break-word;
}

.unsupported-tip {
  position: absolute;
  top: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 0, 0, 0.8);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}
</style> 