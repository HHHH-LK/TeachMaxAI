<template>
  <div class="game-container">
    <!-- 开始闯关按钮 -->
    <StartButton 
      :game-started="gameStarted" 
      @start-game="startGame" 
    />

    <!-- 退出游戏按钮 -->
    <ExitButton @exit-game="exitGame" />

    <!-- 个人中心按钮 -->
    <ProfileCenter @open-profile="openProfile" />

    <!-- 背包按钮 -->
    <Backpack @toggle-inventory="toggleInventory" />

    <!-- 排行榜展示 -->
    <Leaderboard />

    <!-- 退出确认对话框 -->
    <ExitConfirmModal 
      :visible="showExitConfirm" 
      @close="closeExitConfirm" 
      @confirm="confirmExit" 
    />

    <!-- 个人中心信息弹窗 -->
    <ProfileModal 
      :visible="showProfile" 
      :user-profile="userProfile" 
      @close="closeProfile" 
    />

    <!-- 道具查看框 -->
    <InventoryModal 
      :visible="showInventory" 
      :items="items" 
      @close="closeInventory" 
      @select-item="selectItem" 
    />
  </div>
</template>

<script>
import StartButton from './home/StartButton.vue'
import ExitButton from './home/ExitButton.vue'
import ProfileCenter from './home/ProfileCenter.vue'
import Backpack from './home/Backpack.vue'
import Leaderboard from './home/Leaderboard.vue'
import ExitConfirmModal from './home/ExitConfirmModal.vue'
import ProfileModal from './home/ProfileModal.vue'
import InventoryModal from './home/InventoryModal.vue'

import img1 from './assets/攻击道具（无背景）.png'
import img2 from './assets/防御道具（无背景）.png'
import img3 from './assets/回血道具（无背景）.png'

export default {
  name: 'Game',
  components: {
    StartButton,
    ExitButton,
    ProfileCenter,
    Backpack,
    Leaderboard,
    ExitConfirmModal,
    ProfileModal,
    InventoryModal
  },
  data() {
    return {
      gameStarted: false,
      showInventory: false,
      showProfile: false,
      showExitConfirm: false,
      userProfile: {
        nickname: '牢大',
        level: 2,
        totalExp: 120,
        towerLevel: 3,
        hp: 100,
        attack: 50,
        defense: 50,
      },
      items: [
        {
          name: '火焰剑',
          image: img1,
          description: '增加50点攻击力的火焰剑',
          attack: 50,
          type: 'attack',
          quantity: 3
        },
        {
          name: '钢铁盾',
          image: img2,
          description: '提高40点防御力的坚固盾牌',
          defense: 40,
          type: 'defense',
          quantity: 2
        },
        {
          name: '治疗药水',
          image: img3,
          description: '恢复100点生命值的药水',
          heal: 100,
          type: 'heal',
          quantity: 5
        },
      ],
      selectedItem: null,
      // 添加定时器管理
      gameTimer: null,
      // 添加错误状态
      hasError: false,
      errorMessage: ''
    }
  },

  mounted() {
    try {
      // 初始化游戏状态
      this.initGame();
    } catch (error) {
      console.error('游戏初始化失败:', error);
      this.handleError(error);
    }
  },
  
  beforeUnmount() {
    // 清理定时器和资源
    this.cleanup();
  },
  
  // 添加错误处理
  errorCaptured(err, vm, info) {
    console.error('组件错误:', err, vm, info);
    this.handleError(err);

    // 阻止错误继续传播
    return false;
  },
  
  methods: {
    // 初始化游戏
    initGame() {
      try {
        // 重置游戏状态
        this.gameStarted = false;
        this.showInventory = false;
        this.showProfile = false;
        this.showExitConfirm = false;
        this.selectedItem = null;
        
        console.log('游戏初始化完成');
      } catch (error) {
        console.error('游戏初始化失败:', error);
        throw error;
      }
    },
    
    // 清理资源
    cleanup() {
      try {
        // 清理定时器
        if (this.gameTimer) {
          clearTimeout(this.gameTimer);
          this.gameTimer = null;
        }
        
        // 关闭所有弹窗
        this.showInventory = false;
        this.showProfile = false;
        this.showExitConfirm = false;
        
        // 清理选中的道具
        this.selectedItem = null;
        
        console.log('游戏资源清理完成');
      } catch (error) {
        console.error('清理资源时出错:', error);
      }
    },
    
    // 错误处理
    handleError(error) {
      this.hasError = true;
      this.errorMessage = error.message || '游戏运行出错';
      console.error('游戏错误:', error);
    },
    
    // 重置错误状态
    resetError() {
      this.hasError = false;
      this.errorMessage = '';
    },
    
    toggleInventory() {
      try {
        this.showInventory = !this.showInventory;
      } catch (error) {
        console.error('切换背包失败:', error);
        this.handleError(error);
      }
    },
    
    closeInventory() {
      try {
        this.showInventory = false;
      } catch (error) {
        console.error('关闭背包失败:', error);
        this.handleError(error);
      }
    },
    
    selectItem(item) {
      try {
        this.selectedItem = item;
        console.log('选中道具:', item.name);
      } catch (error) {
        console.error('选择道具失败:', error);
        this.handleError(error);
      }
    },

    startGame() {
      try {
        this.gameStarted = true;
        
        // 添加视觉反馈
        this.$nextTick(() => {
          // 可以在这里添加游戏开始的过渡动画
          this.gameTimer = setTimeout(() => {
            // 游戏开始后的逻辑，例如加载关卡、初始化玩家等效果
            console.log('游戏开始！');
            // this.loadGameLevel();
            // 暂定30s
          }, 3000);
        });
      } catch (error) {
        console.error('开始游戏失败:', error);
        this.handleError(error);
      }
    },

    openProfile() {
      try {
        // 打开个人中心的逻辑
        console.log('打开个人中心');
        this.showProfile = true;
      } catch (error) {
        console.error('打开个人中心失败:', error);
        this.handleError(error);
      }
    },

    closeProfile() {
      try {
        this.showProfile = false;
      } catch (error) {
        console.error('关闭个人中心失败:', error);
        this.handleError(error);
      }
    },

    exitGame() {
      try {
        // 退出游戏的逻辑
        console.log('退出游戏');
        this.showExitConfirm = true;
      } catch (error) {
        console.error('退出游戏失败:', error);
        this.handleError(error);
      }
    },

    closeExitConfirm() {
      try {
        this.showExitConfirm = false;
      } catch (error) {
        console.error('关闭退出确认失败:', error);
        this.handleError(error);
      }
    },

    confirmExit() {
      try {
        // 确认退出的逻辑
        console.log('确认退出');
        this.showExitConfirm = false;
        
        // 清理资源
        this.cleanup();
        
        // 暂时不做处理逻辑
        // this.$router.go(-1);
      } catch (error) {
        console.error('确认退出失败:', error);
        this.handleError(error);
      }
    }
  }
}
</script>

<style lang="less" scoped>
.game-container {
  width: 100vw;
  height: 100vh;
  background-image: url('./assets/背景（无水印）.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}
</style>
