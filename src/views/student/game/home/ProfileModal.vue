<template>
  <div v-if="visible" class="profile-overlay" @click="handleClose">
    <div class="profile-container" @click.stop>
      <CanvasRenderer
          :width="600"
          :height="550"
          render-type="profile"
          canvas-class="profile-canvas"
      />
      <div class="profile-content">
        <h2 class="profile-title">角色信息</h2>
        
        <!-- 成功消息提示 -->
        <div v-if="successMessage" class="success-message">
          <span class="success-icon">✅</span>
          {{ successMessage }}
        </div>
        
        <!-- 错误消息提示 -->
        <div v-if="errorMessage" class="error-message">
          <span class="error-icon">❌</span>
          {{ errorMessage }}
        </div>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>正在加载角色信息...</p>
        </div>
        
        <!-- 错误状态 -->
        <div v-else-if="error" class="error-container">
          <div class="error-icon">⚠️</div>
          <p class="error-message">{{ error }}</p>
          <button @click="fetchPlayerInfo" class="retry-btn">重试</button>
        </div>
        
        <!-- 角色信息 -->
        <div v-else class="profile-info">
          <div class="info-item">
            <span class="info-label">游戏昵称</span>
            <span
                v-if="!isEditingNickname"
                @click="enterEditMode"
                class="info-value nickname-editable"
            >
              {{ playerInfo.nickname }}
            </span>
            <input
              v-else
              ref="nicknameInput"
              type="text"
              v-model="tempNickname"
              @blur="confirmEdit"
              @keyup.enter="confirmEdit"
              @keyup.esc="cancelEdit"
              @click.stop
              class="info-value nickname-input"
              maxlength="20"
              placeholder="请输入昵称"
            />
          </div>
          
          <div class="info-item">
            <span class="info-label">等级</span>
            <span class="info-value">{{ playerInfo.level }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">经验值</span>
            <span class="info-value">{{ playerInfo.exp }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">学生ID</span>
            <span class="info-value">{{ playerInfo.studentId }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">用户ID</span>
            <span class="info-value">{{ playerInfo.userId }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDate(playerInfo.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, ref, watch, nextTick, onMounted } from 'vue'
import CanvasRenderer from './CanvasRenderer.vue'
import { gameService } from "@/services/game.js";
import { userConfig } from '@/config/userConfig.js';

// 定义组件名称
defineOptions({
  name: 'ProfileModal'
})

// 响应式数据
const playerInfo = ref({
  nickname: '未设置',
  level: 1,
  exp: 0,
  createTime: '',
  gameName: '',
  studentId: 0,
  userId: 0
});

const loading = ref(false);
const error = ref(null);
const successMessage = ref('');
const errorMessage = ref('');

// 显示成功消息
const showSuccessMessage = (message) => {
  successMessage.value = message;
  setTimeout(() => {
    successMessage.value = '';
  }, 3000);
};

// 显示错误消息
const showErrorMessage = (message) => {
  errorMessage.value = message;
  setTimeout(() => {
    errorMessage.value = '';
  }, 3000);
};

// 获取角色信息
const fetchPlayerInfo = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    const response = await gameService.gameUser.getPlayerInfo(userConfig.studentId);
    
    if (response.data && response.data.code === 0) {
      const data = response.data.data;
      playerInfo.value = {
        nickname: data.gameName || '未设置',
        level: data.level || 1,
        exp: data.exp || 0,
        createTime: data.createTime || '',
        gameName: data.gameName || '',
        studentId: data.studentId || 0,
        userId: data.userId || 0
      };
      console.log('获取到的角色信息:', playerInfo.value);
    } else {
      throw new Error(response.data?.message || '获取角色信息失败');
    }
  } catch (err) {
    console.error('获取角色信息失败:', err);
    error.value = err.message || '获取角色信息失败';
  } finally {
    loading.value = false;
  }
};

// 定义props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  userProfile: {
    type: Object,
    default: () => ({
      nickname: '牢大',
      level: 2,
      totalExp: 120,
      towerLevel: 23,
      hp: 100,
      attack: 50,
      defense: 50,
    })
  }
})

// 定义emits（添加更新昵称事件）
const emit = defineEmits(['close', 'update:nickname'])

// 编辑状态管理
const isEditingNickname = ref(false) // 是否处于编辑状态
const tempNickname = ref(props.userProfile.nickname) // 临时存储编辑中的昵称
const nicknameInput = ref(null) // 输入框ref

// 日期格式化函数
const formatDate = (dateString) => {
  if (!dateString) return '未知';
  try {
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (error) {
    return '格式错误';
  }
};

// 监听弹窗显示状态，当显示时获取角色信息
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    fetchPlayerInfo();
  }
});

// 组件挂载时获取角色信息
onMounted(() => {
  if (props.visible) {
    fetchPlayerInfo();
  }
});

// 监听父组件传入的昵称变化，同步到临时变量
watch(
    () => props.userProfile.nickname,
    (newVal) => {
      tempNickname.value = newVal
    }
)

// 进入编辑模式
const enterEditMode = () => {
  isEditingNickname.value = true
  tempNickname.value = playerInfo.value.nickname
  // 下一帧自动聚焦输入框
  nextTick(() => {
    nicknameInput.value?.focus()
  })
}

// 取消编辑
const cancelEdit = () => {
  tempNickname.value = playerInfo.value.nickname
  isEditingNickname.value = false
}

// 确认编辑（失去焦点或回车时）
const confirmEdit = async () => {
  // 去除首尾空格
  const trimmedNickname = tempNickname.value.trim()
  
  // 输入验证
  if (!trimmedNickname) {
    showErrorMessage('昵称不能为空');
    tempNickname.value = playerInfo.value.nickname;
    isEditingNickname.value = false;
    return;
  }
  
  if (trimmedNickname.length > 20) {
    showErrorMessage('昵称长度不能超过20个字符');
    tempNickname.value = playerInfo.value.nickname;
    isEditingNickname.value = false;
    return;
  }
  
  // 验证：与原昵称不同才触发更新
  if (trimmedNickname !== playerInfo.value.nickname) {
    try {
      // 显示加载状态
      showSuccessMessage('正在更新昵称...');
      
      // 调用API更新昵称
      const response = await gameService.gameUser.updateGamePlayerName(
        trimmedNickname, 
        playerInfo.value.userId
      );
      
      if (response.data && response.data.code === 0) {
        // 更新成功
        console.log('昵称更新成功:', trimmedNickname);
        playerInfo.value.nickname = trimmedNickname;
        emit('update:nickname', trimmedNickname);
        
        // 显示成功提示
        showSuccessMessage('昵称更新成功！');
      } else {
        throw new Error(response.data?.message || '昵称更新失败');
      }
    } catch (error) {
      console.error('昵称更新失败:', error);
      // 显示错误提示
      showErrorMessage(error.message || '昵称更新失败，请重试');
      // 恢复原昵称
      tempNickname.value = playerInfo.value.nickname;
    }
  } else {
    // 昵称没有变化，直接退出编辑模式
    console.log('昵称没有变化');
  }
  
  // 退出编辑模式
  isEditingNickname.value = false;
}

// 处理关闭事件
const handleClose = () => {
  // 关闭时如果处于编辑状态，恢复原昵称
  if (isEditingNickname.value) {
    tempNickname.value = playerInfo.value.nickname
    isEditingNickname.value = false
  }
  emit('close')
}

// 获取标签映射
const getLabel = (key) => {
  const labelMap = {
    nickname: '昵称',
    level: '等级',
    totalExp: '总经验',
    towerLevel: '总塔数',
    hp: '生命值',
    attack: '攻击力',
    defense: '防御力'
  }
  return labelMap[key] || key
}
</script>

<style lang="less" scoped>
@bg-black: #000;
@white: #fff;
@border-radius-md: 10px;
@border-radius-lg: 15px;
@border-radius-sm: 4px;

.profile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(@bg-black, 0.8);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(5px);
}

.profile-container {
  position: relative;
  width: 600px;
  height: 550px;
  border-radius: @border-radius-md;
  overflow: hidden;
  box-shadow: 0 0 50px rgba(139, 69, 19, 0.8);

  .profile-canvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
  }

  .profile-content {
    position: relative;
    z-index: 2;
    padding: 50px;
    color: @white;
    text-shadow: 2px 2px 4px rgba(@bg-black, 0.8);
    background: rgba(@bg-black, 0.7);
    border-radius: @border-radius-md;
    border: 2px solid rgba(218, 165, 32, 0.5);

    .profile-title {
      text-align: center;
      font-size: 2.5em;
      margin-bottom: 40px;
      color: #daa520;
      text-shadow: 0 0 20px rgba(218, 165, 32, 0.8), 2px 2px 4px rgba(@bg-black, 0.9);
      font-weight: bold;
      letter-spacing: 3px;
      font-family: 'Times New Roman', serif;
      background: rgba(@bg-black, 0.8);
      padding: 15px 30px;
      border-radius: @border-radius-md;
      border: 1px solid rgba(218, 165, 32, 0.3);
    }

    .profile-info {
      display: flex;
      flex-direction: column;
      gap: 25px;
      max-height: 300px;
      overflow-y: auto;
      padding-right: 10px;

      // 自定义滚动条样式
      &::-webkit-scrollbar {
        width: 8px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(139, 69, 19, 0.3);
        border-radius: @border-radius-sm;
      }

      &::-webkit-scrollbar-thumb {
        background: #cd853f;
        border-radius: @border-radius-sm;

        &:hover {
          background: #daa520;
        }
      }

      .info-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 15px 20px;
        background: rgba(139, 69, 19, 0.6);
        border: 2px solid #cd853f;
        border-radius: @border-radius-lg;
        transition: all 0.3s ease;
        box-shadow: 0 2px 8px rgba(@bg-black, 0.5);

        &:hover {
          background: rgba(139, 69, 19, 0.8);
          border-color: #daa520;
          transform: translateX(5px);
          box-shadow: 0 5px 15px rgba(218, 165, 32, 0.5);
        }

        .info-label {
          font-size: 1.2em;
          color: #cd853f;
          font-weight: bold;
          text-shadow: 2px 2px 4px rgba(@bg-black, 0.9);
          background: rgba(@bg-black, 0.7);
          padding: 5px 10px;
          border-radius: @border-radius-sm;
          border: 1px solid rgba(205, 133, 63, 0.3);
        }

        .info-value {
          font-size: 1.3em;
          color: #daa520;
          font-weight: bold;
          text-shadow: 0 0 10px rgba(218, 165, 32, 0.6), 2px 2px 4px rgba(@bg-black, 0.9);
          font-family: 'Courier New', monospace;
          background: rgba(@bg-black, 0.7);
          padding: 5px 10px;
          border-radius: @border-radius-sm;
          border: 1px solid rgba(218, 165, 32, 0.3);
        }
      }
    }
  }
}

// 昵称编辑相关样式
.nickname-editable {
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 4px;
  transition: color 0.3s ease;

  &:hover {
    color: #fff;
  }
}

.nickname-input {
  background: rgba(@bg-black, 0.7);
  border: 1px solid #daa520;
  border-radius: @border-radius-sm;
  padding: 5px 10px;
  color: #daa520;
  font-size: 1.3em;
  font-weight: bold;
  font-family: 'Courier New', monospace;
  text-shadow: 0 0 10px rgba(218, 165, 32, 0.6), 2px 2px 4px rgba(@bg-black, 0.9);
  outline: none;
  width: auto;
  min-width: 120px;
  transition: all 0.3s ease;

  &:focus {
    box-shadow: 0 0 8px rgba(218, 165, 32, 0.8);
    border-color: #fff;
  }

  &::placeholder {
    color: rgba(218, 165, 32, 0.5);
    text-shadow: none;
  }
}

// 加载状态样式
.loading-container {
  text-align: center;
  padding: 60px 20px;
  color: @white;
  
  .loading-spinner {
    width: 50px;
    height: 50px;
    border: 4px solid rgba(139, 69, 19, 0.3);
    border-top: 4px solid #daa520;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 20px;
  }
  
  p {
    font-size: 1.2em;
    color: #cd853f;
  }
}

// 消息提示样式
.success-message,
.error-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 15px 20px;
  border-radius: @border-radius-md;
  color: @white;
  font-weight: bold;
  z-index: 10000;
  animation: slideInRight 0.3s ease-out;
  max-width: 300px;
  word-wrap: break-word;
  
  .success-icon,
  .error-icon {
    margin-right: 10px;
    font-size: 1.2em;
  }
}

.success-message {
  background: rgba(76, 175, 80, 0.9);
  border: 2px solid #4caf50;
  box-shadow: 0 4px 15px rgba(76, 175, 80, 0.4);
}

.error-message {
  background: rgba(244, 67, 54, 0.9);
  border: 2px solid #f44336;
  box-shadow: 0 4px 15px rgba(244, 67, 54, 0.4);
}

// 错误状态样式
.error-container {
  text-align: center;
  padding: 60px 20px;
  color: @white;
  
  .error-icon {
    font-size: 3em;
    margin-bottom: 20px;
    animation: shake 0.5s ease-in-out;
  }
  
  .error-message {
    font-size: 1.1em;
    color: #ff6b6b;
    margin-bottom: 20px;
  }
  
  .retry-btn {
    background: #cd853f;
    color: @white;
    border: 2px solid #daa520;
    padding: 10px 20px;
    border-radius: @border-radius-md;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      background: #daa520;
      transform: translateY(-2px);
    }
  }
}

// 响应式设计
@media (max-width: 900px) {
  .profile-container {
    width: 90vw;
    height: 85vh;
  }

  .profile-content {
    padding: 30px;

    .profile-title {
      font-size: 2em;
      margin-bottom: 30px;
    }

    .profile-info {
      gap: 20px;

      .info-item {
        padding: 12px 15px;

        .info-label {
          font-size: 1.1em;
        }

        .info-value {
          font-size: 1.2em;
        }
      }
    }
  }

  .nickname-input {
    font-size: 1.2em;
    min-width: 100px;
  }
}

// 动画关键帧
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

@keyframes slideInRight {
  0% {
    transform: translateX(100%);
    opacity: 0;
  }
  100% {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
