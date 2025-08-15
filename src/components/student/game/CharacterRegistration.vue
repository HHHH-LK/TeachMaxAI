<template>
  <div class="character-registration-overlay" v-if="visible">
    <div class="registration-modal">
      <div class="modal-header">
        <h2>欢迎来到神秘之塔</h2>
        <p>请创建您的游戏角色</p>
      </div>
      
      <div class="modal-body">
         <form @submit.prevent="handleSubmit" class="registration-form">
           <div class="form-group">
             <input
               id="nickname"
               v-model="formData.gameName"
               type="text"
               placeholder="请输入您的游戏昵称"
               required
               maxlength="20"
             />
           </div>

           <!-- 角色属性展示 -->
           <div class="character-stats">
             <div class="stat-item">
               <div class="stat-icon">⚔️</div>
               <div class="stat-info">
                 <div class="stat-label">等级</div>
                 <div class="stat-value">{{ characterStats.level }}</div>
               </div>
             </div>
             <div class="stat-item">
               <div class="stat-icon">✨</div>
               <div class="stat-info">
                 <div class="stat-label">经验值</div>
                 <div class="stat-value">{{ characterStats.exp }}</div>
               </div>
             </div>
           </div>

           <!-- 神秘符文装饰 -->
           <div class="mystical-runes">
             <div class="rune rune-left">✧</div>
             <div class="rune rune-right">✧</div>
             <div class="rune rune-top">✧</div>
             <div class="rune rune-bottom">✧</div>
           </div>
         </form>
       </div>
      
      <div class="modal-footer">
        <button 
          type="button" 
          class="btn btn-secondary" 
          @click="handleCancel"
          :disabled="isSubmitting"
        >
          取消
        </button>
        <button 
          type="submit" 
          class="btn btn-primary" 
          @click="handleSubmit"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? '创建中...' : '创建角色' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { gameService } from '@/services/game.js'
import { userConfig } from '@/config/userConfig.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['registration-success', 'registration-cancel'])

const isSubmitting = ref(false)

const formData = reactive({
  gameName: ''
})

const characterStats = reactive({
  level: 1,
  exp: 0
})

const handleSubmit = async () => {
  if (!formData.gameName) {
    alert('请输入游戏昵称')
    return
  }

  isSubmitting.value = true
  
  try {
    const characterData = {
      userId: userConfig.userId,
      gameName: formData.gameName,
      level: 1,
      exp: 0,
      createTime: new Date().toISOString(),
      studentId: userConfig.studentId
    }

    const response = await gameService.gameUser.createPlayer(characterData)
    
    if (response.data && response.data.success) {
      emit('registration-success', response.data)
    } else {
      throw new Error(response.data?.message || '创建角色失败')
    }
  } catch (error) {
    console.error('创建角色失败:', error)
    alert('创建角色失败，请重试')
  } finally {
    isSubmitting.value = false
  }
}

const handleCancel = () => {
  emit('registration-cancel')
}

const resetForm = () => {
  formData.gameName = ''
}

// 暴露重置方法给父组件
defineExpose({
  resetForm
})
</script>

<style scoped>
.character-registration-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.registration-modal {
  background: linear-gradient(135deg, #0a0a0a 0%, #1a0a1a 30%, #2d0a2d 60%, #1a0a1a 100%);
  border: 2px solid #4a1a4a;
  border-radius: 15px;
  padding: 25px;
  width: 500px;
  height: 400px;
  overflow: hidden;
  box-shadow: 
    0 0 30px rgba(74, 26, 74, 0.6),
    inset 0 0 50px rgba(45, 10, 45, 0.8),
    0 0 100px rgba(74, 26, 74, 0.4),
    0 0 200px rgba(26, 10, 26, 0.5);
  color: #d8bfd8;
  position: relative;
  backdrop-filter: blur(10px);
}

.registration-modal::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(45deg, #4a1a4a, #6a2a6a, #8a2be2, #6a2a6a, #4a1a4a);
  border-radius: 17px;
  z-index: -1;
  animation: borderGlow 3s ease-in-out infinite;
}

.modal-header {
  text-align: center;
  margin-bottom: 20px;
}

.modal-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.modal-header p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.modal-body {
  margin-bottom: 20px;
}

.registration-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 600;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
}

.form-group input,
.form-group select {
  padding: 12px 16px;
  border: 2px solid rgba(74, 26, 74, 0.6);
  border-radius: 10px;
  background: rgba(26, 10, 26, 0.3);
  color: #d8bfd8;
  font-size: 16px;
  transition: all 0.3s ease;
}

.form-group input::placeholder {
  color: rgba(216, 191, 216, 0.5);
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #6a2a6a;
  background: rgba(74, 26, 74, 0.2);
  box-shadow: 0 0 0 3px rgba(74, 26, 74, 0.5);
}

/* 角色属性展示样式 */
.character-stats {
  display: flex;
  justify-content: space-around;
  margin: 18px 0;
  padding: 15px;
  background: linear-gradient(135deg, rgba(26, 10, 26, 0.9), rgba(45, 10, 45, 0.8));
  border: 1px solid #4a1a4a;
  border-radius: 10px;
  position: relative;
  box-shadow: 
    inset 0 0 20px rgba(74, 26, 74, 0.5),
    0 0 15px rgba(45, 10, 45, 0.6);
}

.character-stats::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, transparent, rgba(74, 26, 74, 0.2), transparent);
  border-radius: 10px;
  animation: shimmer 2s ease-in-out infinite;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(135deg, rgba(10, 10, 10, 0.9), rgba(26, 10, 26, 0.8));
  border: 1px solid #4a1a4a;
  border-radius: 8px;
  min-width: 110px;
  justify-content: center;
  position: relative;
  overflow: hidden;
  box-shadow: 
    inset 0 0 15px rgba(74, 26, 74, 0.4),
    0 0 10px rgba(74, 26, 74, 0.5);
}

.stat-item::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(74, 26, 74, 0.3), transparent);
  transform: rotate(45deg);
  animation: lightSweep 3s ease-in-out infinite;
}

.stat-icon {
  font-size: 20px;
  filter: drop-shadow(0 0 5px rgba(212, 175, 55, 0.8));
  animation: iconFloat 2s ease-in-out infinite;
}

.stat-info {
  text-align: center;
}

.stat-label {
  font-size: 11px;
  color: #dda0dd;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 3px;
  font-weight: 600;
  text-shadow: 0 0 5px rgba(221, 160, 221, 0.5);
}

.stat-value {
  font-size: 18px;
  color: #9370db;
  font-weight: bold;
  text-shadow: 0 0 10px rgba(147, 112, 219, 0.6);
  font-family: 'Courier New', monospace;
}

/* 神秘符文装饰 */
.mystical-runes {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1;
}

.rune {
  position: absolute;
  color: #4a1a4a;
  font-size: 18px;
  opacity: 0.8;
  animation: runeGlow 4s ease-in-out infinite;
  filter: drop-shadow(0 0 8px rgba(74, 26, 74, 0.8));
}

.rune-left {
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
}

.rune-right {
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
}

.rune-top {
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
}

.rune-bottom {
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
}



.modal-footer {
  display: flex;
  justify-content: space-between;
  gap: 15px;
}

.btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #4a1a4a, #6a2a6a);
  color: #d8bfd8;
  border: 2px solid #6a2a6a;
  box-shadow: 0 4px 15px rgba(74, 26, 74, 0.7);
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(74, 26, 74, 0.9);
  background: linear-gradient(135deg, #6a2a6a, #4a1a4a);
}

.btn-secondary {
  background: rgba(26, 10, 26, 0.8);
  color: #b8860b;
  border: 2px solid #4a1a4a;
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(74, 26, 74, 0.4);
  border-color: #6a2a6a;
  color: #d8bfd8;
}

@media (max-width: 600px) {
  .registration-modal {
    width: 90%;
    height: auto;
    min-height: 400px;
    padding: 20px;
    margin: 20px;
  }
  
  .modal-footer {
    flex-direction: column;
  }
}

/* 动画关键帧 */
@keyframes borderGlow {
  0%, 100% {
    opacity: 0.8;
  }
  50% {
    opacity: 1;
  }
}

@keyframes shimmer {
  0%, 100% {
    transform: translateX(-100%);
  }
  50% {
    transform: translateX(100%);
  }
}

@keyframes lightSweep {
  0%, 100% {
    transform: rotate(45deg) translateX(-100%);
  }
  50% {
    transform: rotate(45deg) translateX(100%);
  }
}

@keyframes iconFloat {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-3px);
  }
}

@keyframes runeGlow {
  0%, 100% {
    opacity: 0.8;
    text-shadow: 0 0 5px rgba(74, 26, 74, 0.8);
  }
  50% {
    opacity: 1;
    text-shadow: 0 0 20px rgba(74, 26, 74, 1), 0 0 30px rgba(106, 42, 106, 0.8);
  }
}
</style>
