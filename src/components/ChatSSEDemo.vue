<template>
  <div class="chat-sse-demo">
    <div class="connection-panel">
      <h3>SSE连接演示</h3>
      
      <!-- 连接控制 -->
      <div class="connection-controls">
        <el-input 
          v-model="userId" 
          placeholder="请输入用户ID" 
          style="width: 200px; margin-right: 10px;"
        />
        <el-button 
          type="primary" 
          @click="connectSSE" 
          :disabled="isConnected"
        >
          建立连接
        </el-button>
        <el-button 
          type="danger" 
          @click="disconnectSSE" 
          :disabled="!isConnected"
        >
          断开连接
        </el-button>
      </div>

      <!-- 连接状态 -->
      <div class="connection-status">
        <el-tag :type="isConnected ? 'success' : 'info'">
          {{ isConnected ? '已连接' : '未连接' }}
        </el-tag>
        <span v-if="isConnected" class="user-id">用户ID: {{ userId }}</span>
      </div>

      <!-- 在线用户统计 -->
      <div class="online-stats">
        <el-button @click="getOnlineCount" size="small">
          获取在线用户数
        </el-button>
        <span v-if="onlineCount !== null" class="online-count">
          当前在线用户: {{ onlineCount }}
        </span>
      </div>

      <!-- 检查用户在线状态 -->
      <div class="user-status-check">
        <el-input 
          v-model="checkUserId" 
          placeholder="检查用户ID" 
          style="width: 150px; margin-right: 10px;"
        />
        <el-button @click="checkUserOnline" size="small">
          检查在线状态
        </el-button>
        <span v-if="userOnlineStatus !== null" class="user-status">
          {{ checkUserId }}: {{ userOnlineStatus ? '在线' : '离线' }}
        </span>
      </div>
    </div>

    <!-- 消息显示区域 -->
    <div class="message-panel">
      <h4>接收到的消息</h4>
      <div class="message-container">
        <div 
          v-for="(message, index) in messages" 
          :key="index" 
          class="message-item"
          :class="message.type"
        >
          <div class="message-header">
            <span class="message-type">{{ getMessageTypeText(message.type) }}</span>
            <span class="message-time">{{ formatTime(message.timestamp) }}</span>
          </div>
          <div class="message-content">
            <pre>{{ JSON.stringify(message.data, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </div>

    <!-- 错误信息 -->
    <div v-if="errorMessage" class="error-panel">
      <el-alert 
        :title="errorMessage" 
        type="error" 
        show-icon 
        closable
        @close="errorMessage = ''"
      />
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import chatSSEService from '@/services/chatSSEService.js'

export default {
  name: 'ChatSSEDemo',
  setup() {
    const userId = ref('test-user-001')
    const checkUserId = ref('')
    const isConnected = ref(false)
    const onlineCount = ref(null)
    const userOnlineStatus = ref(null)
    const messages = ref([])
    const errorMessage = ref('')

    // 建立SSE连接
    const connectSSE = () => {
      if (!userId.value.trim()) {
        errorMessage.value = '请输入用户ID'
        return
      }

      try {
        chatSSEService.connect(userId.value, {
          onConnected: (data) => {
            console.log('SSE连接成功:', data)
            messages.value.push({
              type: 'connected',
              data: { message: data },
              timestamp: new Date()
            })
            isConnected.value = true
            errorMessage.value = ''
          },
          onAiStreamMessage: (data) => {
            console.log('收到AI流式消息:', data)
            messages.value.push({
              type: 'ai_stream_message',
              data: data,
              timestamp: new Date()
            })
          },
          onChatMessage: (data) => {
            console.log('收到聊天消息:', data)
            messages.value.push({
              type: 'chat_message',
              data: data,
              timestamp: new Date()
            })
          },
          onError: (error) => {
            console.error('SSE连接错误:', error)
            errorMessage.value = `连接错误: ${error.message || '未知错误'}`
            isConnected.value = false
          },
          onReconnect: () => {
            console.log('SSE重连中...')
            messages.value.push({
              type: 'reconnect',
              data: { message: '正在重连...' },
              timestamp: new Date()
            })
          }
        })
      } catch (error) {
        console.error('建立SSE连接失败:', error)
        errorMessage.value = `建立连接失败: ${error.message}`
      }
    }

    // 断开SSE连接
    const disconnectSSE = () => {
      chatSSEService.disconnect(userId.value)
      isConnected.value = false
      messages.value.push({
        type: 'disconnected',
        data: { message: '连接已断开' },
        timestamp: new Date()
      })
    }

    // 获取在线用户数
    const getOnlineCount = async () => {
      try {
        const count = await chatSSEService.getOnlineCount()
        onlineCount.value = count
      } catch (error) {
        console.error('获取在线用户数失败:', error)
        errorMessage.value = `获取在线用户数失败: ${error.message}`
      }
    }

    // 检查用户在线状态
    const checkUserOnline = async () => {
      if (!checkUserId.value.trim()) {
        errorMessage.value = '请输入要检查的用户ID'
        return
      }

      try {
        const isOnline = await chatSSEService.isUserOnline(checkUserId.value)
        userOnlineStatus.value = isOnline
      } catch (error) {
        console.error('检查用户在线状态失败:', error)
        errorMessage.value = `检查用户在线状态失败: ${error.message}`
      }
    }

    // 获取消息类型文本
    const getMessageTypeText = (type) => {
      const typeMap = {
        connected: '连接成功',
        ai_stream_message: 'AI流式消息',
        chat_message: '聊天消息',
        reconnect: '重连',
        disconnected: '断开连接'
      }
      return typeMap[type] || type
    }

    // 格式化时间
    const formatTime = (timestamp) => {
      return new Date(timestamp).toLocaleTimeString()
    }

    // 组件挂载时检查连接状态
    onMounted(() => {
      isConnected.value = chatSSEService.isConnected(userId.value)
    })

    // 组件卸载时断开连接
    onUnmounted(() => {
      if (isConnected.value) {
        chatSSEService.disconnect(userId.value)
      }
    })

    return {
      userId,
      checkUserId,
      isConnected,
      onlineCount,
      userOnlineStatus,
      messages,
      errorMessage,
      connectSSE,
      disconnectSSE,
      getOnlineCount,
      checkUserOnline,
      getMessageTypeText,
      formatTime
    }
  }
}
</script>

<style scoped>
.chat-sse-demo {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.connection-panel {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.connection-controls {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}

.connection-status {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-id {
  color: #666;
  font-size: 14px;
}

.online-stats {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.online-count {
  color: #409eff;
  font-weight: bold;
}

.user-status-check {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-status {
  color: #67c23a;
  font-weight: bold;
}

.message-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
}

.message-container {
  max-height: 400px;
  overflow-y: auto;
}

.message-item {
  margin-bottom: 15px;
  padding: 10px;
  border-radius: 6px;
  border-left: 4px solid;
}

.message-item.connected {
  background: #f0f9ff;
  border-left-color: #409eff;
}

.message-item.ai_stream_message {
  background: #f0f9ff;
  border-left-color: #67c23a;
}

.message-item.chat_message {
  background: #f0f9ff;
  border-left-color: #e6a23c;
}

.message-item.reconnect {
  background: #fff7e6;
  border-left-color: #e6a23c;
}

.message-item.disconnected {
  background: #fef0f0;
  border-left-color: #f56c6c;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.message-type {
  font-weight: bold;
  color: #333;
}

.message-time {
  color: #999;
  font-size: 12px;
}

.message-content {
  background: #fafafa;
  padding: 8px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  overflow-x: auto;
}

.error-panel {
  margin-top: 20px;
}
</style> 