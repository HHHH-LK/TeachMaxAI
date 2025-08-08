<template>
  <div class="sse-usage-example">
    <h2>SSE服务使用示例</h2>
    
    <!-- 基本连接示例 -->
    <div class="example-section">
      <h3>1. 基本连接</h3>
      <el-button @click="basicConnect" type="primary">
        建立基本连接
      </el-button>
      <el-button @click="basicDisconnect" type="danger">
        断开连接
      </el-button>
      <p>状态: {{ basicConnectionStatus }}</p>
    </div>

    <!-- 消息处理示例 -->
    <div class="example-section">
      <h3>2. 消息处理</h3>
      <el-button @click="connectWithHandlers" type="success">
        建立带处理器的连接
      </el-button>
      <div class="message-log">
        <h4>消息日志:</h4>
        <div v-for="(msg, index) in messageLog" :key="index" class="log-item">
          <span class="timestamp">{{ formatTime(msg.timestamp) }}</span>
          <span class="type">{{ msg.type }}</span>
          <span class="content">{{ msg.content }}</span>
        </div>
      </div>
    </div>

    <!-- 在线状态检查示例 -->
    <div class="example-section">
      <h3>3. 在线状态检查</h3>
      <el-input v-model="checkUserId" placeholder="输入用户ID" style="width: 200px; margin-right: 10px;" />
      <el-button @click="checkOnlineStatus" type="info">
        检查在线状态
      </el-button>
      <p v-if="onlineStatus !== null">
        用户 {{ checkUserId }} 状态: {{ onlineStatus ? '在线' : '离线' }}
      </p>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import chatSSEService from '@/services/chatSSEService.js'

export default {
  name: 'SSEUsageExample',
  setup() {
    const basicConnectionStatus = ref('未连接')
    const messageLog = ref([])
    const checkUserId = ref('test-user-001')
    const onlineStatus = ref(null)

    // 基本连接示例
    const basicConnect = () => {
      const userId = 'example-user-001'
      
      chatSSEService.connect(userId, {
        onConnected: (data) => {
          basicConnectionStatus.value = '已连接'
          console.log('基本连接成功:', data)
        },
        onError: (error) => {
          basicConnectionStatus.value = '连接失败'
          console.error('基本连接失败:', error)
        }
      })
    }

    const basicDisconnect = () => {
      chatSSEService.disconnect('example-user-001')
      basicConnectionStatus.value = '已断开'
    }

    // 带消息处理器的连接示例
    const connectWithHandlers = () => {
      const userId = 'handler-user-001'
      
      chatSSEService.connect(userId, {
        onConnected: (data) => {
          addMessageLog('连接成功', `用户 ${userId} 连接成功`)
        },
        onAiStreamMessage: (data) => {
          addMessageLog('AI消息', `收到AI流式消息: ${JSON.stringify(data)}`)
        },
        onChatMessage: (data) => {
          addMessageLog('聊天消息', `收到聊天消息: ${JSON.stringify(data)}`)
        },
        onError: (error) => {
          addMessageLog('错误', `连接错误: ${error.message}`)
        },
        onReconnect: () => {
          addMessageLog('重连', '正在尝试重连...')
        }
      })
    }

    // 添加消息日志
    const addMessageLog = (type, content) => {
      messageLog.value.push({
        type,
        content,
        timestamp: new Date()
      })
    }

    // 检查在线状态
    const checkOnlineStatus = async () => {
      if (!checkUserId.value.trim()) {
        alert('请输入用户ID')
        return
      }

      try {
        const isOnline = await chatSSEService.isUserOnline(checkUserId.value)
        onlineStatus.value = isOnline
      } catch (error) {
        console.error('检查在线状态失败:', error)
        alert('检查在线状态失败')
      }
    }

    // 格式化时间
    const formatTime = (timestamp) => {
      return new Date(timestamp).toLocaleTimeString()
    }

    return {
      basicConnectionStatus,
      messageLog,
      checkUserId,
      onlineStatus,
      basicConnect,
      basicDisconnect,
      connectWithHandlers,
      checkOnlineStatus,
      formatTime
    }
  }
}
</script>

<style scoped>
.sse-usage-example {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.example-section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.example-section h3 {
  margin-top: 0;
  color: #333;
}

.message-log {
  margin-top: 15px;
}

.message-log h4 {
  margin-bottom: 10px;
  color: #666;
}

.log-item {
  display: flex;
  gap: 10px;
  margin-bottom: 5px;
  padding: 5px;
  background: #fff;
  border-radius: 4px;
  font-size: 12px;
}

.timestamp {
  color: #999;
  min-width: 80px;
}

.type {
  color: #409eff;
  font-weight: bold;
  min-width: 80px;
}

.content {
  color: #333;
  flex: 1;
}
</style> 