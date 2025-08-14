<template>
  <div class="fight-container">
    <!-- 开场动画组件 -->
    <StartAnimation
        :show="showStartAnimation"
        @animation-end="handleStartAnimationEnd"
    />

    <!-- 开始战斗动画 -->
    <StartBattleAnimation
        :show="showBattleAnimation"
        @animation-end="handleBattleAnimationEnd"
    />

    <!-- 战斗主界面 (仅在动画结束后显示) -->
    <div v-if="showBattleContent">
      <div class="fight-header">
        <FightHeader />
      </div>

      <div class="fight-main">
        <FightMain
            :main-health="mainHealth"
            :main-max-health="mainMaxHealth"
            :enemy-health="enemyHealth"
            :enemy-max-health="enemyMaxHealth"
            @player-hit="handlePlayerHit"
            @enemy-hit="handleEnemyHit"
        />
      </div>

      <div class="fight-footer">
        <div class="card-fight">
          <FightFooter />
        </div>
      </div>

      <!-- 背包按钮组件 -->
      <Backpack @toggle-inventory="toggleInventory" />

      <!-- 道具库模态框 -->
      <InventoryModal
          :visible="inventoryVisible"
          :items="inventoryItems"
          @close="closeInventory"
          @use-item="handleUseItem"
      />

      <!-- 道具使用特效 -->
      <ItemEffect
          :visible="effectVisible"
          :effect-type="effectType"
          :damage="effectDamage"
          :defense="effectDefense"
          :heal="effectHeal"
          :position="effectPosition"
          @close="effectVisible = false"
      />

      <!-- 战败界面 -->
      <DefeatScreen :show="showDefeatScreen" @continue="handleDefeatContinue" />

      <!-- 奖励面板 -->
      <RewardPanel
          :show="showRewardPanel"
          :health-value="mainHealth"
          :max-health="mainMaxHealth"
          :rewards="availableRewards"
          @close="handleRewardClose"
          @skip="handleRewardSkip"
          @next-level="handleRewardNextLevel"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import Backpack from "@/components/fight/Bag.vue";
import InventoryModal from "@/components/fight/Bags.vue";
import ItemEffect from "@/components/fight/ItemEffect.vue";
import FightHeader from "@/components/fight/CharacterHead.vue";
import FightMain from "@/components/fight/Human.vue";
import FightFooter from "@/components/fight/Card.vue";
import DefeatScreen from "@/components/fight/DefeatScreen.vue";
import RewardPanel from "@/components/fight/RewardPanel.vue";
import StartBattleAnimation from "@/components/fight/StartBattleAnimation.vue";
import StartAnimation from "@/components/fight/Animation.vue";

import img1 from "/Image/Attach.png";
import img2 from "/Image/Defense.png";
import img3 from "/Image/Blood.png";

// 动画控制状态
const showStartAnimation = ref(true);       // 开场动画显示状态
const showBattleAnimation = ref(false);      // 开始战斗动画显示状态
const showBattleContent = ref(false);        // 战斗内容显示状态

// 血量状态
const mainMaxHealth = ref(100); // 主角最大血量
const enemyMaxHealth = ref(150); // 敌人最大血量
const mainHealth = ref(100); // 主角当前血量
const enemyHealth = ref(150); // 敌人当前血量
const showExitModal = ref(false);

// 处理开场动画结束
const handleStartAnimationEnd = () => {
  console.log("开场动画结束，播放战斗开始动画");
  showStartAnimation.value = false;
  showBattleAnimation.value = true;
};

// 处理战斗动画结束
const handleBattleAnimationEnd = () => {
  console.log("战斗动画结束，进入战斗");
  showBattleAnimation.value = false;
  showBattleContent.value = true;
};

// 监听敌人血量变化
watch(enemyHealth, (newValue) => {
  if (newValue <= 0) {
    // 当敌人血量降到0时显示奖励面板
    setTimeout(() => {
      showRewardPanel.value = true;
    }, 3000);
  }
});

// 战败界面显示状态
const showDefeatScreen = ref(false);

// 奖励面板显示状态
const showRewardPanel = ref(false);

// 可用的奖励选项
const availableRewards = ref([]);

// 道具库显示状态
const inventoryVisible = ref(false);
// 道具库物品
const inventoryItems = ref([
  {
    name: "火焰剑",
    image: img1,
    description: "增加50点攻击力的火焰剑",
    attack: 50,
    type: "attack",
    quantity: 3,
  },
  {
    name: "钢铁盾",
    image: img2,
    description: "提高40点防御力的坚固盾牌",
    defense: 40,
    type: "defense",
    quantity: 2,
  },
  {
    name: "治疗药水",
    image: img3,
    description: "恢复100点生命值的药水",
    heal: 100,
    type: "heal",
    quantity: 5,
  },
]);

// 道具特效状态
const effectVisible = ref(false);
const effectType = ref("");
const effectPosition = ref({ x: 50, y: 50 });
const effectDamage = ref(0);
const effectDefense = ref(0);
const effectHeal = ref(0);

// 监听主角血量变化
watch(mainHealth, (newValue) => {
  if (newValue <= 0) {
    // 当血量降到0或以下时显示战败界面
    setTimeout(() => {
      showDefeatScreen.value = true;
    }, 3000);
    console.log("玩家血量归零，显示战败界面");
  }
});

// 处理战败继续事件
const handleDefeatContinue = () => {
  // 重置游戏状态
  showDefeatScreen.value = false;
  mainHealth.value = mainMaxHealth.value;
  enemyHealth.value = enemyMaxHealth.value;
  console.log("玩家选择继续，重置游戏状态");
};

// 处理奖励面板关闭
const handleRewardClose = () => {
  showRewardPanel.value = false;
  console.log("奖励面板关闭");
};

// 处理跳过奖励
const handleRewardSkip = () => {
  showRewardPanel.value = false;
  console.log("玩家跳过奖励");
};

// 处理进入下一层
const handleRewardNextLevel = () => {
  showRewardPanel.value = false;
  console.log("玩家进入下一层");
  resetGameForNextLevel();
};

// 重置游戏进入下一层
const resetGameForNextLevel = () => {
  // 恢复玩家血量
  mainHealth.value = mainMaxHealth.value;

  // 增强下一层的敌人（血量增加20%）
  const nextLevelMaxHealth = Math.floor(enemyMaxHealth.value * 1.2);
  enemyMaxHealth.value = nextLevelMaxHealth;
  enemyHealth.value = nextLevelMaxHealth;
  console.log(`进入下一层，敌人最大血量增加至: ${nextLevelMaxHealth}`);
};

// 切换道具库显示
const toggleInventory = () => {
  inventoryVisible.value = !inventoryVisible.value;
};

// 关闭道具库
const closeInventory = () => {
  inventoryVisible.value = false;
};

// 处理道具使用
const handleUseItem = (item) => {
  console.log("使用道具:", item);
  closeInventory();

  if (item.attack) {
    effectType.value = "attack-buff";
    effectDamage.value = item.attack;
    effectPosition.value = { x: 25, y: 50 };
    enemyHealth.value = Math.max(0, enemyHealth.value - item.attack);
  } else if (item.defense) {
    effectType.value = "defense";
    effectDefense.value = item.defense;
    effectPosition.value = { x: 25, y: 50 };
  } else if (item.heal) {
    effectType.value = "heal";
    effectHeal.value = item.heal;
    effectPosition.value = { x: 25, y: 50 };
    mainHealth.value = Math.min(
        mainMaxHealth.value,
        mainHealth.value + item.heal
    );
    console.log(`使用治疗药水，当前生命值: ${mainHealth.value}`);
  }

  effectVisible.value = true;
  setTimeout(() => {
    effectVisible.value = false;
  }, 2000);

  const itemIndex = inventoryItems.value.findIndex((i) => i.name === item.name);
  if (itemIndex !== -1) {
    inventoryItems.value[itemIndex].quantity--;
    if (inventoryItems.value[itemIndex].quantity <= 0) {
      inventoryItems.value.splice(itemIndex, 1);
    }
  }
};

// 处理玩家受击事件
const handlePlayerHit = (damage) => {
  console.log(`玩家受到 ${damage} 点伤害`);
  mainHealth.value = Math.max(0, mainHealth.value - damage);
};

// 处理敌人受击事件
const handleEnemyHit = (damage) => {
  console.log(`敌人受到 ${damage} 点伤害`);
  enemyHealth.value = Math.max(0, enemyHealth.value - damage);
};
</script>

<style scoped>
.fight-container {
  width: 100%;
  height: 100%;
  position: fixed;
  top: 0;
  left: 0;
  overflow: hidden;
  z-index: 2;
}

/* 顶部区域 */
.fight-header {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 50px;
  z-index: 10000;
}

.fight-main {
  z-index: 0;
}

.fight-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  min-height: 100px;
  z-index: 100;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  padding-bottom: 80px;
}

.card-fight {
  pointer-events: auto;
}

.reward-panel {
  z-index: 20000 !important;
}
</style>