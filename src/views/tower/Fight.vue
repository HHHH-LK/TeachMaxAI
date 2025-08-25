<template>
  <div class="fight-container">
    <!-- 开场动画组件 -->
    <StartAnimation
      :show="showStartAnimation"
      :captions="towerFloorStory"
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
        <FightHeader
          :currentRound="round"
          :towerLevelCur="towerLevelCur"
          :playerLevel="playerLevel"
          :currentExp="playerExp"
          :maxExp="expToNextLevel"
        />
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
          <FightFooter
            :current-round="current_round"
            :current-floor="towerFloorId"
            @damage="handleDamage"
          />
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

  <!-- 卡牌组件 -->
  <Card
    v-if="showCard"
    :current-round="current_round"
    :current-floor="towerFloorId"
    @damage="handleDamage"
  />

  <!-- 卡牌效果模态框 -->
  <CardEffectModal
    v-if="showCardEffect"
    :difficulty="cardDifficulty"
    :question-bank="questionBank"
    :current-floor="towerFloorId"
    @close="closeCardEffect"
    @correct-answer="handleCorrectAnswer"
    @wrong-answer="handleWrongAnswer"
    @damage="handleDamage"
  />

  <!-- 挑战模态框 -->
  <ChallengeModal
    v-if="showChallenge"
    :question="currentQuestion"
    :answers="currentAnswers"
    :correct-answers="correctAnswers"
    :time-limit="timeLimit"
    :question-type="questionType"
    :question-id="selectedQuestionRef?.id"
    :floor-id="towerFloorId"
    :change-count="changeCount"
    @close="closeChallenge"
    @completed="handleChallengeCompleted"
    @give-up="handleChallengeGiveUp"
    @damage="handleDamage"
  />

  <!-- 升级进度组件 -->
  <ExpProgressAnimation
    v-if="showExpAnimation"
    :currentExp="playerExp"
    :expToNextLevel="expToNextLevel"
    :currentLevel="playerLevel"
    :expGain="expGainValue"
    :usedExp="usedExp"
    :visible="showExpAnimation"
    @level-up="handleLevelUp"
    class="exp-progress-overlay"
  />
</template>

<script setup>
import { ref, watch, onMounted, computed } from "vue";
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
import ExpProgressAnimation from "@/components/fight/ExpProgressAnimation.vue";
import { gameService } from "@/services/game";
import { useRoute, useRouter } from "vue-router";
import userConfig from "@/config/userConfig";

import img1 from "/Image/Attach.png";
import img2 from "/Image/Defense.png";
import img3 from "/Image/Blood.png";
import { ElMessage } from "element-plus";

// 动画控制状态
const showStartAnimation = ref(true); // 开场动画显示状态
const showBattleAnimation = ref(false); // 开始战斗动画显示状态
const showBattleContent = ref(false); // 战斗内容显示状态

// 血量状态
const mainMaxHealth = ref(0); // 主角最大血量
const enemyMaxHealth = ref(0); // 敌人最大血量
const mainHealth = ref(1); // 主角当前血量
const enemyHealth = ref(1); // 敌人当前血量
const showExitModal = ref(false);
const playerLevel = ref(1);
const playerExp = ref(0);
const usedExp = ref(0);
const expToNextLevel = ref(100);

const showExpAnimation = ref(false); // 控制经验动画显示
const expGainValue = ref(0); // 获得的经验值

// 字幕数据
const towerFloorStory = ref(""); // 存储从后端获取的字幕数据

// 获取当前路由
const route = useRoute();
const router = useRouter();

// 获取当前楼层ID
const towerFloorId = computed(() => {
  const floorId = route.query.floorId;

  console.log("当前楼层", floorId);
  // 如果查询参数中有 floorId，则使用它
  if (floorId) {
    return parseInt(floorId);
  }
  // 否则使用默认值
  return 25;
});

const towerLevelCur = computed(() => {
  const towerLevel = route.query.towerLevel;

  console.log("当前层数", towerLevel);
  // 如果查询参数中有 floorId，则使用它
  if (towerLevel) {
    return parseInt(towerLevel);
  }
  // 否则使用默认值
  return 1;
});

// 获取玩家信息
const fetchPlayerInfo = async () => {
  try {
    const infoResponse = await gameService.gameUser.getPlayerInfo(
      userConfig.studentId
    );
    if (infoResponse.data && infoResponse.data.success) {
      const playerData = infoResponse.data.data;
      playerLevel.value = playerData.level || 1;
      playerExp.value = playerData.exp || 0;

      // 获取当前等级所需经验
      const expResponse = await gameService.fighting.getExp(playerLevel.value);
      if (expResponse.data && expResponse.data.success) {
        expToNextLevel.value = expResponse.data.data;
      }

      if (!usedExp.value) {
        usedExp.value = playerData.exp;
      }
    } else {
      throw new Error(infoResponse.data.message || "获取玩家信息失败");
    }
  } catch (error) {
    console.error("获取玩家信息失败:", error);
  }
};

const handleLevelUp = () => {
  console.log("玩家升级了!");
  fetchPlayerInfo();

  // 获取新等级所需经验
  gameService.fighting.getExp(playerLevel.value).then((response) => {
    if (response.data && response.data.success) {
      expToNextLevel.value = response.data.data;
    }
  });
};

// 获取Boss信息的方法
const fetchBossInfo = async () => {
  try {
    const response = await gameService.monster.getBossInfo(towerFloorId.value);

    // 检查响应是否成功
    if (response.data.success) {
      enemyMaxHealth.value = response.data.data.hp;
      console.log("Boss信息加载成功:", enemyMaxHealth);
    } else {
      console.error("获取Boss信息失败:", response.data.message);
      enemyMaxHealth.value = 100;
    }
  } catch (error) {
    console.error("获取Boss信息异常:", error);
  }
};

// 获取楼层故事数据
const fetchTowerFloorStory = async () => {
  try {
    const response = await gameService.tower.getTowerFloorStoryByTowerFloorId(
      towerFloorId.value
    );

    if (response.data && response.data.success) {
      towerFloorStory.value = response.data.data || "";
      console.log("获取楼层故事成功:", towerFloorStory.value);
    } else {
      console.error("获取楼层故事失败:", response.data.message);
      // 使用默认故事
      towerFloorStory.value = `在总塔的第${towerFloorId.value}层，传说中这里是"初始之厅"，一片由纯粹数据构成的空间...`;
    }
  } catch (error) {
    console.error("获取楼层故事失败:", error);
    // 使用默认故事
    towerFloorStory.value = `在总塔的第${towerFloorId.value}层，传说中这里是"初始之厅"，一片由纯粹数据构成的空间...`;
  }
};

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
    fetchRewards().then(() => {
      // 获取奖励数据后显示奖励面板
      setTimeout(() => {
        showRewardPanel.value = true;
      }, 3000);
    });
  }
});

const getReward = async () => {};

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

const ITEM_IMAGE_MAPPING = {
  火焰剑: img1,
  烈焰之刃: img1,
  火系武器: img1,
  钢铁盾: img2,
  圣光盾: img2,
  防御装备: img2,
  治疗药水: img3,
  恢复药剂: img3,
  生命药水: img3,
};

// 根据物品名称获取对应的图片
const getItemImage = (itemName) => {
  // 精确匹配
  if (ITEM_IMAGE_MAPPING[itemName]) {
    return ITEM_IMAGE_MAPPING[itemName];
  }

  // 模糊匹配
  const matchedKey = Object.keys(ITEM_IMAGE_MAPPING).find(
    (key) => itemName.includes(key) || key.includes(itemName)
  );

  return matchedKey ? ITEM_IMAGE_MAPPING[matchedKey] : img1; // 默认返回img1
};

// 获取学生物品数据
const fetchStudentItems = async () => {
  try {
    const response = await gameService.items.getStudentItems(
      userConfig.studentId
    );

    console.log("用户", userConfig.studentId, "道具", response.data.data);
    if (response.data.success) {
      inventoryItems.value = response.data.data.map((item) => ({
        id: item.itemId,
        name: item.name,
        image: getItemImage(item.name),
        effectValue: item.effectValue,
        description: item.description,
        type: item.type,
        rarity: item.rarity,
      }));
    } else {
      error.value = response.data.message || "获取物品失败";
    }

    for (let i = 0; i < inventoryItems.value.length; i++) {
      const responseQuantity = await gameService.items.getItemQuantity(
        inventoryItems.value[i].id,
        userConfig.studentId
      );
      inventoryItems.value[i].quantity = responseQuantity.data.data;
    }

    console.log("用户道具", inventoryItems.value);
  } catch (error) {
    console.error("获取物品数据失败:", error);
  }
};

const round = ref();

//获取回合数量
const fetchRound = async () => {
  const response = await gameService.fighting.getCurrentBattleLog(
    towerFloorId.value,
    current_round.value,
    userConfig.studentId
  );
  console.log("战斗日志", response.data.data);
  if (response.data.data) {
    round.value = response.data.data.totalTurns;
    console.log("round", round.value);
  }
};

// 道具特效状态
const effectVisible = ref(false);
const effectType = ref("");
const effectPosition = ref({ x: 25, y: 50 });
const effectDamage = ref(0);
const effectDefense = ref(0);
const effectHeal = ref(0);

const UserHp = ref();
const current_round = ref();
const changeCount = ref();

// 获取战斗进度（角色血量和boss血量）
const getBattleProgress = async () => {
  if (!current_round.value) return;

  const responseCount = await gameService.fighting.getUserChangeCount(
    userConfig.studentId,
    towerFloorId.value
  );

  const count = responseCount.data.data;
  changeCount.value = responseCount.data.data;

  console.log("挑战次数", count);
  try {
    const response = await gameService.fighting.getGameUserHPandbossHP(
      userConfig.studentId,
      current_round.value
    );

    if (response.data && response.data.success) {
      console.log("获取战斗进度成功:", response.data);
      enemyHealth.value = response.data.data.BOSS_HP;
      if (!UserHp.value) {
        UserHp.value = response.data.data.GAME_USER_HP;
        mainMaxHealth.value = UserHp.value;
        mainHealth.value = UserHp.value;
      } else {
        mainHealth.value = response.data.data.GAME_USER_HP;
      }
    } else {
      console.error("获取战斗进度失败:", response.data?.message || "未知错误");
    }
  } catch (error) {
    console.error("获取战斗进度异常:", error);
  }
};

// 新增的获取奖励方法
const fetchRewards = async () => {
  try {
    console.log("正在获取奖励数据...");
    const response = await gameService.fighting.getAward(
      userConfig.studentId,
      towerFloorId.value
    );

    if (response.data?.success) {
      console.log("奖励数据获取成功:", response.data.data);

      // 处理奖励数据格式
      const rewards = [];

      expGainValue.value = response.data.data.exp;
      // 添加经验值奖励
      if (response.data.data.exp) {
        rewards.push(`经验值 + ${response.data.data.exp}`);
      }

      // 添加物品奖励
      if (response.data.data.item && response.data.data.item.length > 0) {
        response.data.data.item.forEach((item) => {
          rewards.push(`${item.name} x 1`);
        });
      }

      // 如果没有奖励，添加默认提示
      if (rewards.length === 0) {
        rewards.push("未获得任何奖励");
      }

      console.log("rewards", rewards);
      availableRewards.value = rewards;
    } else {
      console.error(
        "获取奖励失败:",
        response.data?.message || "未返回成功状态"
      );
      availableRewards.value = ["奖励获取失败"];
    }
  } catch (error) {
    console.error("获取奖励异常:", error);
    availableRewards.value = ["奖励获取异常"];
  }
};

// 监听主角血量变化
watch(mainHealth, (newValue) => {
  if (newValue <= 0) {
    setTimeout(() => {
      showDefeatScreen.value = true;
    }, 3000);
  }
});

// 处理战败继续事件
const handleDefeatContinue = () => {
  // 重置游戏状态
  showDefeatScreen.value = false;
  router.push("/game");
};

// 处理奖励面板关闭
const handleRewardClose = async () => {
  try {
    // 调用接口获取结果
    const response = await gameService.fighting.getResult(
      towerFloorId.value, // 当前楼层ID
      userConfig.studentId,
      current_round.value
    );

    if (response.data?.success) {
      console.log("获取楼层结果成功:", response.data.data);
      if (response.data.data == 1) {
        showRewardPanel.value = false;
        console.log("战斗胜利");
      } else if (resultData.value == 3) {
        console.log("获取处理数据为", resultData);
      }
    } else {
      console.error("获取楼层结果失败:", response.data?.message);
      // 即使失败也继续进入下一层
    }
  } catch (error) {
    console.error("获取楼层结果异常:", error);
    // 即使异常也继续进入下一层
  }
  router.push("/game");
  showRewardPanel.value = false;
  console.log("奖励面板关闭");
};

// 处理跳过奖励（忽略）
const handleRewardSkip = () => {
  showRewardPanel.value = false;
  console.log("玩家跳过奖励");
};

const handleRewardNextLevel = async () => {
  console.log("玩家准备进入下一层");

  try {
    // 调用接口获取结果
    const response = await gameService.fighting.getResult(
      towerFloorId.value, // 当前楼层ID
      userConfig.studentId,
      current_round.value
    );

    if (response.data?.success) {
      console.log("获取楼层结果成功:", response.data.data);

      if (response.data.data == 1) {
        showRewardPanel.value = false;
        console.log("战斗胜利");

        // 显示经验进度动画
        showExpAnimation.value = true;

        fetchPlayerInfo();

        // 检查是否升级
        if (playerExp.value >= expToNextLevel.value) {
          handleLevelUp();
        }

        // 延迟3秒后进入下一层
        setTimeout(() => {
          enterNextLevel();
        }, 5000);
        //测试所以注释
      } else if (resultData.value == 3) {
        console.log("获取处理数据为", resultData);
      }
    } else {

      showExpAnimation.value = true;
      console.error("获取楼层结果失败:", response.data?.message);
      fetchPlayerInfo();
      ElMessage.error(response.data?.message);
      router.push("/game");
      window.location.reload();
    }
  } catch (error) {
    console.error("获取楼层结果异常:", error);
    // 即使异常也继续进入下一层
    showExpAnimation.value = true;
    fetchPlayerInfo();
    ElMessage.error(response.data?.message);
  }
};

// 进入下一层
const enterNextLevel = async () => {
  try {
    // 增加楼层ID

    const newFloorId = towerFloorId.value + 1;
    const newLevel = parseInt(route.query.towerLevel) + 1;

    // 更新状态
    towerFloorId.value = newFloorId;

    const params = {
      floorId: newFloorId,
      towerId: route.query.towerId,
      courseId: route.query.courseId,
      towerName: route.query.towerName,
      towerLevel: newLevel,
      timestamp: Date.now(), // 添加时间戳强制刷新
    };

    console.log("进入新楼层:", params);

    // 使用 router.replace 而不是 push，因为当前页面已经是 /test
    await router.replace({
      path: "/test",
      query: params,
    });

    window.location.reload();
  } catch (error) {
    console.error("进入下一层失败:", error);
    // 处理错误，例如返回主菜单或显示错误信息
    ElMessage.error(`进入下一层失败: ${error.message}`);
    router.push("/game");
  }
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
const handleUseItem = async (item) => {
  console.log("使用道具:", item);
  closeInventory();

  const useItemDate = {
    itemId: item.id,
    studentId: userConfig.studentId,
    floorId: towerFloorId.value,
    changeCount: changeCount.value,
    max_HP: mainMaxHealth.value,
  };

  console.log("changCount", changeCount.value);
  const response = await gameService.items.useItem(useItemDate);
  console.log("使用道具", response.data);

  getBattleProgress();

  if (item.type === "attack") {
    effectType.value = "attack-buff";
    effectDamage.value = item.attack;
    effectDefense.value = 0;
    effectHeal.value = 0;
  } else if (item.type === "defense") {
    effectType.value = "defense";
    effectDamage.value = 0;
    effectDefense.value = item.defense;
    effectHeal.value = 0;
  } else if (item.type === "heal") {
    effectType.value = "heal";
    effectDamage.value = 0;
    effectDefense.value = 0;
    effectHeal.value = item.effectValue;
  }

  // 显示特效
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
const handlePlayerHit = (damageInfo) => {
  if (damageInfo.target === "player") {
    console.log(`玩家受到 ${damageInfo.damage} 点伤害`);
    mainHealth.value = Math.max(0, mainHealth.value - damageInfo.damage);
    getBattleProgress();
    fetchRound();
  }
};

// 处理敌人受击事件
const handleEnemyHit = (damageInfo) => {
  if (damageInfo.target === "boss") {
    console.log(`敌人受到 ${damageInfo.damage} 点伤害`);
    enemyHealth.value = Math.max(0, enemyHealth.value - damageInfo.damage);
    getBattleProgress();
    fetchRound();
  }
};

// 统一的伤害处理方法
const handleDamage = (damageInfo) => {
  if (damageInfo.target === "player") {
    handlePlayerHit(damageInfo);
  } else if (damageInfo.target === "boss") {
    handleEnemyHit(damageInfo);
  }
};

// 开始战斗
const startFighting = async () => {
  try {
    console.log(
      "开始战斗，楼层ID:",
      towerFloorId.value,
      "学生ID:",
      userConfig.studentId
    );
    const response = await gameService.fighting.startFighting(
      towerFloorId.value,
      userConfig.studentId
    );
    if (response.data && response.data.success) {
      console.log("开始战斗成功:", response.data);
      current_round.value = response.data.data;
      console.log("现在的挑战日志为", current_round);
      // getBattleProgress();
    } else {
      console.error("开始战斗失败:", response.data?.message || "未知错误");
    }
  } catch (error) {
    console.error("开始战斗异常:", error);
  }
};

// 组件挂载时获取楼层故事
onMounted(() => {
  fetchTowerFloorStory();
  fetchBossInfo();
  fetchPlayerInfo();

  fetchStudentItems();
  startFighting()
    .then(() => {
      fetchRound();
      getBattleProgress();
    })
    .catch((error) => {
      console.error("启动战斗失败:", error);
    });
});
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
  user-select: none;
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

.exp-progress-overlay {
  z-index: 30000;
}
</style>
