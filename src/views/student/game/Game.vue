<template>
  <div class="game-container">
    <!-- 开始闯关按钮 -->
<!--    <StartButton-->
<!--      :game-started="gameStarted"-->
<!--      @start-game="startGame"-->
<!--    />-->

    <!-- 退出游戏按钮 -->
    <ExitButton @exit-game="exitGame" />

    <!-- 个人中心按钮 -->
    <ProfileCenter @open-profile="openProfile" />

    <!-- 背包按钮 -->
    <Backpack @toggle-inventory="toggleInventory" />

    <!-- 排行榜展示 -->
    <Leaderboard />

    <!-- 塔选择器 -->
    <TowerSelector @tower-selected="onTowerSelected" />

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

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue';
import StartButton from './home/StartButton.vue';
import ExitButton from './home/ExitButton.vue';
import ProfileCenter from './home/ProfileCenter.vue';
import Backpack from './home/Backpack.vue';
import Leaderboard from './home/Leaderboard.vue';
import TowerSelector from './home/TowerSelector.vue';
import ExitConfirmModal from './home/ExitConfirmModal.vue';
import ProfileModal from './home/ProfileModal.vue';
import InventoryModal from './home/InventoryModal.vue';

import img1 from './assets/攻击道具（无背景）.png';
import img2 from './assets/防御道具（无背景）.png';
import img3 from './assets/回血道具（无背景）.png';

// 响应式状态
const gameStarted = ref(false);
const showInventory = ref(false);
const showProfile = ref(false);
const showExitConfirm = ref(false);
const selectedItem = ref(null);
const gameTimer = ref(null);
const hasError = ref(false);
const errorMessage = ref('');

// 复杂响应式对象
const userProfile = reactive({
  nickname: '牢大',
  level: 2,
  totalExp: 120,
  towerLevel: 3,
  hp: 100,
  attack: 50,
  defense: 50,
});

const items = reactive([
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
]);

// 初始化游戏
const initGame = () => {
  try {
    // 重置游戏状态
    gameStarted.value = false;
    showInventory.value = false;
    showProfile.value = false;
    showExitConfirm.value = false;
    selectedItem.value = null;

    console.log('游戏初始化完成');
  } catch (error) {
    console.error('游戏初始化失败:', error);
    throw error;
  }
};

// 清理资源
const cleanup = () => {
  try {
    // 清理定时器
    if (gameTimer.value) {
      clearTimeout(gameTimer.value);
      gameTimer.value = null;
    }

    // 关闭所有弹窗
    showInventory.value = false;
    showProfile.value = false;
    showExitConfirm.value = false;

    // 清理选中的道具
    selectedItem.value = null;

    console.log('游戏资源清理完成');
  } catch (error) {
    console.error('清理资源时出错:', error);
  }
};

// 错误处理
const handleError = (error) => {
  hasError.value = true;
  errorMessage.value = error.message || '游戏运行出错';
  console.error('游戏错误:', error);
};

// 重置错误状态
const resetError = () => {
  hasError.value = false;
  errorMessage.value = '';
};

// 切换背包显示
const toggleInventory = () => {
  try {
    showInventory.value = !showInventory.value;
  } catch (error) {
    console.error('切换背包失败:', error);
    handleError(error);
  }
};

// 关闭背包
const closeInventory = () => {
  try {
    showInventory.value = false;
  } catch (error) {
    console.error('关闭背包失败:', error);
    handleError(error);
  }
};

// 选择道具
const selectItem = (item) => {
  try {
    selectedItem.value = item;
    console.log('选中道具:', item.name);
  } catch (error) {
    console.error('选择道具失败:', error);
    handleError(error);
  }
};

// 开始游戏
const startGame = () => {
  try {
    gameStarted.value = true;

    // 添加视觉反馈
    nextTick(() => {
      // 可以在这里添加游戏开始的过渡动画
      gameTimer.value = setTimeout(() => {
        // 游戏开始后的逻辑
        console.log('游戏开始！');
        // this.loadGameLevel();
      }, 3000);
    });
  } catch (error) {
    console.error('开始游戏失败:', error);
    handleError(error);
  }
};

// 打开个人中心
const openProfile = () => {
  try {
    console.log('打开个人中心');
    showProfile.value = true;
  } catch (error) {
    console.error('打开个人中心失败:', error);
    handleError(error);
  }
};

// 关闭个人中心
const closeProfile = () => {
  try {
    showProfile.value = false;
  } catch (error) {
    console.error('关闭个人中心失败:', error);
    handleError(error);
  }
};

// 退出游戏
const exitGame = () => {
  try {
    console.log('退出游戏');
    showExitConfirm.value = true;
  } catch (error) {
    console.error('退出游戏失败:', error);
    handleError(error);
  }
};

// 关闭退出确认
const closeExitConfirm = () => {
  try {
    showExitConfirm.value = false;
  } catch (error) {
    console.error('关闭退出确认失败:', error);
    handleError(error);
  }
};

// 确认退出
const confirmExit = () => {
  try {
    console.log('确认退出');
    showExitConfirm.value = false;

    // 清理资源
    cleanup();

    // 暂时不做处理逻辑
    // router.go(-1);
  } catch (error) {
    console.error('确认退出失败:', error);
    handleError(error);
  }
};

// 塔选择事件处理
const onTowerSelected = (tower) => {
  try {
    console.log('选择的塔:', tower);
    // 这里可以添加选择塔后的逻辑
  } catch (error) {
    console.error('处理塔选择失败:', error);
    handleError(error);
  }
};

// 生命周期钩子
onMounted(() => {
  try {
    // 初始化游戏状态
    initGame();
  } catch (error) {
    console.error('游戏初始化失败:', error);
    handleError(error);
  }
});

onBeforeUnmount(() => {
  // 清理定时器和资源
  cleanup();
});
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
