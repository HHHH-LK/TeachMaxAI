<template>
  <div class="leaderboard-container">
    <div class="leaderboard-header">
      <h3 class="title">⚔️ 排行榜</h3>
      <div class="tower-filter">
        <div class="scroll-container">
          <div class="scroll-indicator left" @click="scrollLeft">
            <div class="scroll-arrow">‹</div>
          </div>

          <div class="tower-tabs" ref="towerTabs">
            <div
                class="tower-tab total-tower"
                :class="{ active: selectedTower === 'all' }"
                @click="selectTower('all')"
            >
              <div class="tab-icon">⚰️</div>
              <div class="tab-text">总塔</div>
              <div class="tab-glow"></div>
            </div>

            <div
                v-for="tower in studentTowers"
                :key="tower.towerId"
                class="tower-tab horror-tower"
                :class="{ active: selectedTower === tower.towerId.toString() }"
                @click="selectTower(tower.towerId.toString())"
            >
              <div class="tab-icon">💀</div>
              <div class="tab-text">{{ tower.name.split('_')[0] }}</div>
              <div class="tab-glow"></div>
            </div>
          </div>

          <div class="scroll-indicator right" @click="scrollRight">
            <div class="scroll-arrow">›</div>
          </div>
        </div>
      </div>
    </div>

    <div class="leaderboard-list" ref="leaderboardList">
      <div class="scroll-container" :class="{ 'few-data': filteredLeaderboardData.length <= 5 }">
        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <div class="loading-text">正在加载排行榜数据...</div>
        </div>
        
        <!-- 错误状态 -->
        <div v-else-if="error" class="error-state">
          <div class="error-icon">⚠️</div>
          <div class="error-text">{{ error }}</div>
          <button class="retry-button" @click="fetchTotalRanking">重试</button>
        </div>
        
        <!-- 空数据状态 -->
        <div v-else-if="filteredLeaderboardData.length === 0" class="empty-state">
          <div class="empty-icon">📊</div>
          <div class="empty-text">
            {{ selectedTower === 'all' ? '暂无总榜数据' : getTowerDisplayName(selectedTower) }}
          </div>
          <div class="empty-subtext">请稍后再试或联系管理员</div>
        </div>
        
        <!-- 排行榜数据 -->
        <div v-else class="scroll-content" :style="{ transform: `translateY(${scrollOffset}px)` }">
          <div
              v-for="player in filteredLeaderboardData"
              :key="`${player.towerLevel}-${player.rank}`"
              class="player-row"
              :class="getRankClass(player.rank)"
          >
            <div class="rank-info">
              <span class="rank-number">{{ player.rank }}</span>
              <span class="rank-medal" v-if="player.rank <= 3">
                {{ getRankMedal(player.rank) }}
              </span>
            </div>

            <div class="player-info">
              <div class="player-name">{{ player.name }}</div>
              <div class="player-stats">
                <span class="stat">Lv.{{ player.level }}</span>
                <span class="stat">塔{{ player.towerLevel }}层</span>
<!--                <span class="stat">{{ player.exp }}经验</span>-->
              </div>
            </div>
          </div>
          
          <!-- 数据少时的底部装饰 -->
          <div v-if="filteredLeaderboardData.length <= 5" class="bottom-decoration">
            <div class="decoration-line"></div>
            <div class="decoration-text">排行榜</div>
            <div class="decoration-line"></div>
          </div>
        </div>
      </div>
    </div>
    <!-- 我的总榜信息栏（仅在总塔时显示） -->
    <div class="my-info-bar" v-if="selectedTower === 'all'">
      <div class="my-info-title">我的总榜</div>

      <div v-if="myInfoLoading" class="my-info-loading">
        <span class="spinner"></span>
        <span>正在获取我的总榜信息...</span>
      </div>

      <div v-else-if="myInfoError" class="my-info-error">
        <span class="error-text">获取失败：{{ myInfoError }}</span>
        <button class="retry-button" @click="fetchUserTotalInfo">重试</button>
      </div>

      <div v-else-if="myTotalInfo" class="my-info-content">
        <div class="my-info-row">
          <div class="my-rank">
            <span class="label">排名</span>
            <span class="value">#{{ myTotalInfo.rank ?? '-' }}</span>
          </div>
          <div class="my-name" :title="myTotalInfo.name">{{ myTotalInfo.name || '—' }}</div>
        </div>
        <div class="my-meta-row">
          <span class="meta">最高塔层 {{ myTotalInfo.maxFloor ?? 0 }}</span>
        </div>
      </div>

      <div v-else class="my-info-empty">
        暂无我的总榜数据
      </div>
    </div>

    <!-- 我的该塔排行信息栏（仅在具体塔时显示） -->
    <div class="my-info-bar" v-if="selectedTower !== 'all'">
      <div class="my-info-title">我的{{ currentTowerName }}排行</div>

      <div v-if="myTowerLoading" class="my-info-loading">
        <span class="spinner"></span>
        <span>正在获取我的该塔排行信息...</span>
      </div>

      <div v-else-if="myTowerError" class="my-info-error">
        <span class="error-text">获取失败：{{ myTowerError }}</span>
        <button class="retry-button" @click="fetchUserTowerInfo(selectedTower)">重试</button>
      </div>

      <div v-else-if="myTowerInfo" class="my-info-content">
        <div class="my-info-row">
          <div class="my-rank">
            <span class="label">排名</span>
            <span class="value">#{{ myTowerInfo.rank ?? '-' }}</span>
          </div>
          <div class="my-name" :title="myTowerInfo.name">{{ myTowerInfo.name || '—' }}</div>
        </div>
        <div class="my-meta-row">
          <span class="meta">该塔层数 {{ myTowerInfo.floor ?? 0 }}</span>
        </div>
      </div>

      <div v-else class="my-info-empty">
        暂无我的该塔排行数据
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { gameService } from "@/services/game.js";
import userConfig from "@/config/userConfig.js";

// 响应式变量
const selectedTower = ref('all');
const canScrollLeft = ref(false);
const canScrollRight = ref(true);
const scrollOffset = ref(0);
const scrollInterval = ref(null);
const towerTabs = ref(null);
const leaderboardList = ref(null);

// 新增：数据加载状态和错误处理
const isLoading = ref(false);
const error = ref(null);
const leaderboardData = ref([]);
const studentTowers = ref([]); // 存储学生的塔信息

// 我的总榜信息
const myTotalInfo = ref(null);
const myInfoLoading = ref(false);
const myInfoError = ref(null);

// 塔内排行信息
const myTowerInfo = ref(null);
const myTowerLoading = ref(false);
const myTowerError = ref(null);

// 当前塔显示名（用于标题）
const currentTowerName = computed(() => {
  if (selectedTower.value === 'all') return '总塔';
  const t = studentTowers.value.find(x => x.towerId.toString() === selectedTower.value);
  return t ? t.name.split('_')[0] : `塔${selectedTower.value}`;
});


// 存储事件监听器的引用，用于清理
const eventListeners = ref({
  touchstart: null,
  touchmove: null,
  touchend: null
});

// 获取学生的塔信息
const fetchStudentTowers = async () => {
  try {
    const studentId = userConfig.studentId;
    console.log(`开始获取学生${studentId}的塔信息...`);
    
    const response = await gameService.tower.getTowerByStudentId(studentId);
    console.log("获取到学生塔信息:", response);
    
    if (response.data && response.data.success && response.data.data) {
      const towers = response.data.data;
      
      // 检查塔信息的完整性
      const validTowers = towers.filter(tower => {
        if (!tower.towerId || !tower.courseId || !tower.name) {
          console.warn(`塔信息不完整:`, tower);
          return false;
        }
        return true;
      });
      
      if (validTowers.length > 0) {
        studentTowers.value = validTowers;
        console.log("处理后的有效塔信息:", studentTowers.value);
      } else {
        console.warn("没有有效的塔信息，使用默认数据");
        studentTowers.value = [
          { towerId: 1, courseId: 1, name: 'Java程序设计_学习塔' },
          { towerId: 2, courseId: 2, name: '数据结构与算法_学习塔' },
          { towerId: 3, courseId: 3, name: '高等数学A_学习塔' }
        ];
      }
    } else {
      console.warn("获取塔信息失败:", response);
      // 如果获取失败，使用默认的塔信息
      studentTowers.value = [
        { towerId: 1, courseId: 1, name: 'Java程序设计_学习塔' },
        { towerId: 2, courseId: 2, name: '数据结构与算法_学习塔' },
        { towerId: 3, courseId: 3, name: '高等数学A_学习塔' }
      ];
    }
  } catch (err) {
    console.error("获取学生塔信息失败:", err);
    // 使用默认塔信息作为备选
    studentTowers.value = [
      { towerId: 1, courseId: 1, name: 'Java程序设计_学习塔' },
      { towerId: 2, courseId: 2, name: '数据结构与算法_学习塔' },
      { towerId: 3, courseId: 3, name: '高等数学A_学习塔' }
    ];
  }
};

// 获取总榜数据的方法
const fetchTotalRanking = async () => {
  try {
    isLoading.value = true;
    error.value = null;
    
    const response = await gameService.towerRanking.getTotalRanking();
    console.log("获取到总榜数据:", response);
    
    if (response.data && response.data.success && response.data.data) {
      // 处理返回的数据格式
      const rawData = response.data.data;
      
      // 转换数据格式以匹配组件需求
      const processedData = rawData.map((item, index) => ({
        name: item.studentName || '未知玩家',
        level: item.studentLevel || 1,
        towerLevel: item.maxTowerFloorNo || 0,
        // exp: Math.floor((item.studentLevel || 1) * 100 + Math.random() * 200), // 根据等级计算经验
        rank: index + 1,
        totalRank: index + 1,
        studentId: item.studentId
      }));
      
      leaderboardData.value = processedData;
      console.log("处理后的总榜数据:", processedData);
    } else {
      console.warn("API 返回数据格式异常:", response);
      // 如果数据格式异常，使用默认数据
      leaderboardData.value = getDefaultLeaderboardData();
    }
  } catch (err) {
    console.error("获取总榜数据失败:", err);
    error.value = "获取排行榜数据失败，请稍后重试";
    // 使用默认数据作为备选
    leaderboardData.value = getDefaultLeaderboardData();
  } finally {
    isLoading.value = false;
  }
};

// 获取“我的总榜”信息
const fetchUserTotalInfo = async () => {
  try {
    myInfoLoading.value = true;
    myInfoError.value = null;
    const studentId = userConfig.studentId;

    // 如果方法挂在别处，请按实际命名空间调整：
    // 例如：gameService.ranking.getUserTotalTowerInfo
    const res = await gameService.towerRanking.getUserTotalTowerInfo(studentId);
    console.log("获取到“我的总榜”信息:", res);

    // 兼容 success/code 两种返回
    const ok = res?.data?.success === true || res?.data?.code === 0;
    if (!ok || !res?.data?.data) {
      throw new Error(res?.data?.msg || '接口返回异常');
    }

    const d = res.data.data;
    myTotalInfo.value = {
      name: d.gameUserName || '我',
      maxFloor: d.grade || 0,
      rank: d.sortNumber ?? null,
      studentId: d.studentId
    };
  } catch (e) {
    console.error('获取我的总榜信息失败:', e);
    myInfoError.value = e?.message || '获取失败，请稍后重试';
    myTotalInfo.value = null;
  } finally {
    myInfoLoading.value = false;
  }
};

// 获取“我的该塔排行”信息
const fetchUserTowerInfo = async (towerId) => {
  try {
    myTowerLoading.value = true;
    myTowerError.value = null;
    myTowerInfo.value = null;

    const studentId = userConfig.studentId;
    const res = await gameService.towerRanking.getUserTowerSortUserInfo(studentId, towerId);
    console.log('获取到“我的该塔排行”信息:', res);

    const ok = res?.data?.success === true || res?.data?.code === 0;
    if (!ok || !res?.data?.data) {
      throw new Error(res?.data?.msg || '接口返回异常');
    }

    const d = res.data.data;
    // 字段命名参考：gameUserName、grade（层数/成绩）、sortNumber（排名）
    myTowerInfo.value = {
      name: d.gameUserName || '我',
      floor: d.grade ?? 0,
      rank: d.sortNumber ?? null,
      studentId: d.studentId
    };
  } catch (e) {
    console.error('获取我的该塔排行失败:', e);
    myTowerError.value = e?.message || '获取失败，请稍后重试';
    myTowerInfo.value = null;
  } finally {
    myTowerLoading.value = false;
  }
};

// 获取指定塔的排行榜数据
const fetchTowerRanking = async (towerId) => {
  try {
    isLoading.value = true;
    error.value = null;
    
    console.log(`开始获取塔${towerId}的排行榜数据...`);
    console.log('当前可用的塔信息:', studentTowers.value);
    
    // 根据塔ID找到对应的课程ID
    const towerInfo = studentTowers.value.find(tower => tower.towerId.toString() === towerId);
    if (!towerInfo) {
      console.warn(`未找到塔ID ${towerId} 的信息`);
      error.value = `未找到塔${towerId}的信息`;
      return;
    }
    
    console.log(`找到塔信息:`, towerInfo);
    console.log(`使用课程ID: ${towerInfo.courseId} 获取排行榜数据`);
    
    const response = await gameService.towerRanking.getTowerRankingByCourse(towerInfo.courseId);
    console.log(`获取到${towerInfo.name}排行榜数据:`, response);
    
    if (response.data && response.data.success && response.data.data) {
      const rawData = response.data.data;
      console.log(`原始排行榜数据:`, rawData);
      
      const processedData = rawData.map((item, index) => ({
        name: item.studentName || '未知玩家',
        level: item.studentLevel || 1,
        towerLevel: parseInt(towerId),
        // exp: Math.floor((item.studentLevel || 1) * 100 + Math.random() * 200),
        rank: index + 1,
        totalRank: null, // 分榜没有总排名
        studentId: item.studentId
      }));
      
      console.log(`处理后的排行榜数据:`, processedData);
      
      // 更新对应塔的数据
      updateTowerData(towerId, processedData);
      console.log(`塔${towerId}数据更新完成`);
    } else {
      console.warn(`塔${towerId}排行榜数据格式异常:`, response);
      error.value = `获取${towerInfo.name}排行榜数据失败`;
    }
  } catch (err) {
    console.error(`获取塔${towerId}数据失败:`, err);
    error.value = `获取塔${towerId}排行榜失败`;
  } finally {
    isLoading.value = false;
  }
};

// 更新指定塔的数据
const updateTowerData = (towerId, towerData) => {
  console.log(`开始更新塔${towerId}的数据...`);
  console.log(`更新前的总数据:`, leaderboardData.value);
  
  // 过滤掉现有的该塔数据，然后添加新数据
  const filteredData = leaderboardData.value.filter(item => 
    !item.towerLevel || item.towerLevel !== parseInt(towerId)
  );
  
  console.log(`过滤后的数据:`, filteredData);
  console.log(`要添加的新数据:`, towerData);
  
  leaderboardData.value = [...filteredData, ...towerData];
  
  console.log(`更新后的总数据:`, leaderboardData.value);
  console.log(`塔${towerId}数据更新完成`);
};

// 默认排行榜数据（当API失败时使用）
const getDefaultLeaderboardData = () => [
  { name: '暂无数据', level: 1, towerLevel: 1, rank: 1, totalRank: 1 }
];

// 计算属性：过滤排行榜数据
const filteredLeaderboardData = computed(() => {
  if (selectedTower.value === 'all') {
    return leaderboardData.value
        .filter(player => player.totalRank)
        .sort((a, b) => a.totalRank - b.totalRank);
  } else {
    const towerLevel = parseInt(selectedTower.value);
    return leaderboardData.value
        .filter(player => player.towerLevel === towerLevel)
        .sort((a, b) => a.rank - b.rank);
  }
});

// 方法
const selectTower = async (tower) => {
  console.log(`切换到${tower === 'all' ? '总塔' : `塔${tower}`}排行榜`);
  selectedTower.value = tower;

  if (tower !== 'all') {
    console.log(`准备获取塔${tower}的排行榜与我的排行信息...`);
    await Promise.all([fetchTowerRanking(tower), fetchUserTowerInfo(tower)]);
  } else {
    await fetchUserTotalInfo();
    console.log('切换到总榜，显示总排行榜数据');
  }
};

const scrollLeft = () => {
  const container = towerTabs.value;
  if (container) {
    container.scrollBy({
      left: -200,
      behavior: 'smooth'
    });
    updateScrollState();
  }
};

const scrollRight = () => {
  const container = towerTabs.value;
  if (container) {
    container.scrollBy({
      left: 200,
      behavior: 'smooth'
    });
    updateScrollState();
  }
};

const updateScrollState = () => {
  nextTick(() => {
    const container = towerTabs.value;
    if (container) {
      canScrollLeft.value = container.scrollLeft > 0;
      canScrollRight.value = container.scrollLeft < (container.scrollWidth - container.clientWidth);
    }
  });
};

const addTouchSupport = () => {
  const container = towerTabs.value;
  if (!container) return;

  let startX = 0;
  let scrollLeft = 0;

  // 定义事件处理函数并存储引用
  eventListeners.value.touchstart = (e) => {
    startX = e.touches[0].pageX - container.offsetLeft;
    scrollLeft = container.scrollLeft;
  };

  eventListeners.value.touchmove = (e) => {
    if (!startX) return;
    const x = e.touches[0].pageX - container.offsetLeft;
    const walk = (x - startX) * 2;
    container.scrollLeft = scrollLeft - walk;
  };

  eventListeners.value.touchend = () => {
    startX = 0;
    updateScrollState();
  };

  // 添加事件监听
  container.addEventListener('touchstart', eventListeners.value.touchstart);
  container.addEventListener('touchmove', eventListeners.value.touchmove);
  container.addEventListener('touchend', eventListeners.value.touchend);
};

// 清理事件监听的函数
const removeTouchEvents = () => {
  const container = towerTabs.value;
  if (container && eventListeners.value) {
    if (eventListeners.value.touchstart) {
      container.removeEventListener('touchstart', eventListeners.value.touchstart);
    }
    if (eventListeners.value.touchmove) {
      container.removeEventListener('touchmove', eventListeners.value.touchmove);
    }
    if (eventListeners.value.touchend) {
      container.removeEventListener('touchend', eventListeners.value.touchend);
    }
  }
};

const getRankClass = (rank) => {
  if (rank === 1) return 'rank-gold';
  if (rank === 2) return 'rank-silver';
  if (rank === 3) return 'rank-bronze';
  return 'rank-normal';
};

const getRankMedal = (rank) => {
  if (rank === 1) return '🥇';
  if (rank === 2) return '🥈';
  if (rank === 3) return '🥉';
  return '';
};

const startAutoScroll = () => {
  scrollInterval.value = setInterval(() => {
    scrollOffset.value -= 1;

    if (Math.abs(scrollOffset.value) >= 300) {
      scrollOffset.value = 0;
    }
  }, 50);
};

const stopAutoScroll = () => {
  if (scrollInterval.value) {
    clearInterval(scrollInterval.value);
    scrollInterval.value = null;
  }
};

// 获取塔的显示名称
const getTowerDisplayName = (towerId) => {
  if (towerId === 'all') return '暂无总榜数据';
  const tower = studentTowers.value.find(t => t.towerId.toString() === towerId);
  return tower ? `暂无${tower.name.split('_')[0]}排行榜数据` : `暂无塔${towerId}排行榜数据`;
};

// 生命周期钩子
onMounted(() => {
  nextTick(() => {
    updateScrollState();
    addTouchSupport();
    startAutoScroll();
    fetchTotalRanking();     // 总榜
    fetchStudentTowers();    // 塔信息
    fetchUserTotalInfo();    // 我的总榜
  });
});

// 在setup顶层使用onBeforeUnmount
onBeforeUnmount(() => {
  stopAutoScroll();
  removeTouchEvents(); // 在这里清理事件监听
});
</script>

<style lang="less" scoped>
/* 样式部分保持不变 */
.leaderboard-container {
  top: 20px;
  left: 20px;
  z-index: 1000;
  width: 280px;
  background: linear-gradient(135deg, rgba(15, 0, 20, 0.85), rgba(25, 0, 35, 0.85), rgba(10, 0, 15, 0.85));
  border: 3px solid rgba(75, 0, 100, 0.8);
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.8), 0 0 30px rgba(75, 0, 100, 0.3);
  backdrop-filter: blur(20px);
  position: relative;
  isolation: isolate;
}

.leaderboard-header {
  text-align: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid rgba(75, 0, 100, 0.6);
  position: relative;

  .title {
    color: #8a2be2;
    font-size: 18px;
    font-weight: bold;
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.9);
    font-family: 'Times New Roman', serif;
    letter-spacing: 1px;
    margin-bottom: 10px;
  }
}

.tower-filter {
  margin-top: 12px;

  .scroll-container {
    position: relative;
    display: flex;
    align-items: center;
    background: rgba(15, 0, 20, 0.4);
    border-radius: 8px;
    border: 1px solid rgba(75, 0, 100, 0.4);
    padding: 8px 0;

    .scroll-indicator {
      top: 50%;
      transform: translateY(-50%);
      width: 24px;
      height: 24px;
      background: linear-gradient(135deg, rgba(75, 0, 100, 0.9), rgba(65, 0, 85, 0.9));
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      z-index: 10;
      transition: all 0.3s ease;
      border: 2px solid rgba(75, 0, 100, 0.6);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: -100%;
        width: 100%;
        height: 100%;
        background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
        transition: left 0.6s ease;
      }

      &:hover {
        background: linear-gradient(135deg, rgba(75, 0, 100, 1), rgba(65, 0, 85, 1));
        transform: translateY(-50%) scale(1.1);
        box-shadow: 0 0 12px rgba(75, 0, 100, 0.6), 0 4px 12px rgba(0, 0, 0, 0.4);
        border-color: rgba(75, 0, 100, 0.8);

        &::before {
          left: 100%;
        }
      }

      &:active {
        transform: translateY(-50%) scale(0.95);
        box-shadow: 0 0 8px rgba(75, 0, 100, 0.4);
      }

      &.left {
        top: 12px;
        left: 4px;
      }

      &.right {
        top: 12px;
        right: 4px;
      }

      .scroll-arrow {
        color: #ffffff;
        font-size: 18px;
        font-weight: bold;
        text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
        line-height: 1;
        transition: all 0.2s ease;
        filter: drop-shadow(0 0 4px rgba(75, 0, 100, 0.4));
      }
    }

    .tower-tabs {
      display: flex;
      gap: 6px;
      padding: 0 32px;
      overflow-x: auto;
      scroll-behavior: smooth;
      scrollbar-width: none;
      -ms-overflow-style: none;

      &::-webkit-scrollbar {
        display: none;
      }

      &::before,
      &::after {
        content: '';
        position: absolute;
        top: 0;
        bottom: 0;
        width: 40px;
        pointer-events: none;
        z-index: 5;
      }

      &::before {
        left: 0;
        background: linear-gradient(90deg, rgba(25, 0, 25, 0.8), transparent);
      }

      &::after {
        right: 0;
        background: linear-gradient(90deg, transparent, rgba(25, 0, 25, 0.8));
      }
    }
  }

  .tower-tab {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 8px 6px;
    min-width: 50px;
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    background: rgba(25, 0, 25, 0.4);
    border: 1px solid transparent;
    overflow: hidden;

    &:hover {
      transform: translateY(-2px);
      background: rgba(75, 0, 130, 0.3);
      border-color: rgba(147, 112, 219, 0.5);
      box-shadow: 0 4px 12px rgba(147, 112, 219, 0.3);

      .tab-icon {
        transform: scale(1.1) rotate(5deg);
        filter: drop-shadow(0 0 8px rgba(147, 112, 219, 0.6));
      }

      .tab-text {
        color: #e6e6fa;
        text-shadow: 0 0 8px rgba(147, 112, 219, 0.5);
      }
    }

    &.active {
      background: linear-gradient(135deg, rgba(147, 112, 219, 0.4), rgba(138, 43, 226, 0.4));
      border-color: #9370db;
      box-shadow: 0 0 15px rgba(147, 112, 219, 0.5), inset 0 0 10px rgba(147, 112, 219, 0.2);
      transform: translateY(-1px);

      .tab-icon {
        color: #ffd700;
        filter: drop-shadow(0 0 10px rgba(255, 215, 0, 0.6));
        animation: iconPulse 2s ease-in-out infinite;
      }

      .tab-text {
        color: #ffd700;
        text-shadow: 0 0 8px rgba(255, 215, 0, 0.5);
        font-weight: bold;
      }
    }

    .tab-icon {
      font-size: 16px;
      margin-bottom: 4px;
      transition: all 0.3s ease;
      filter: drop-shadow(0 0 4px rgba(147, 112, 219, 0.4));
    }

    .tab-text {
      font-size: 10px;
      color: #9370db;
      text-align: center;
      font-weight: 500;
      transition: all 0.3s ease;
    }
  }
}

@keyframes iconPulse {
  0%, 100% { transform: scale(1.1) rotate(5deg); }
  50% { transform: scale(1.2) rotate(10deg); }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.leaderboard-list {
  max-height: 300px;
  overflow: hidden;
  position: relative;

  .scroll-container {
    height: 100%;
    overflow: hidden;

    &.few-data {
      max-height: 200px; /* 当数据少时，容器高度减小 */
      
      .player-row {
        margin-bottom: 8px; /* 数据少时增加行间距 */
        
        &:last-child {
          margin-bottom: 0; /* 最后一行不需要底部间距 */
        }
      }
    }
  }

  .scroll-content {
    transition: transform 0.05s linear;
  }

  .loading-state,
  .error-state,
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    color: #e6e6fa;
    font-size: 14px;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
    background: rgba(15, 0, 20, 0.5);
    border-radius: 6px;
    border: 1px solid rgba(75, 0, 100, 0.6);
    box-shadow: inset 0 0 10px rgba(75, 0, 100, 0.3);
    backdrop-filter: blur(10px);
    margin-bottom: 10px;

    .loading-spinner {
      border: 4px solid rgba(147, 112, 219, 0.3);
      border-top: 4px solid #9370db;
      border-radius: 50%;
      width: 30px;
      height: 30px;
      animation: spin 1s linear infinite;
      margin-bottom: 10px;
    }

    .loading-text {
      font-style: italic;
    }

    .error-icon,
    .empty-icon {
      font-size: 24px;
      margin-bottom: 10px;
    }

    .error-text {
      color: #ff6b6b;
      margin-bottom: 15px;
      text-align: center;
    }

    .empty-subtext {
      color: #9370db;
      font-size: 12px;
      text-align: center;
      opacity: 0.8;
    }

    .retry-button {
      background: linear-gradient(135deg, #9370db, #8a2be2);
      color: #ffffff;
      padding: 8px 15px;
      border-radius: 6px;
      border: none;
      font-size: 14px;
      font-weight: bold;
      cursor: pointer;
      transition: all 0.3s ease;
      box-shadow: 0 2px 8px rgba(75, 0, 100, 0.4);
      text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
      filter: drop-shadow(0 0 4px rgba(75, 0, 100, 0.4));

      &:hover {
        background: linear-gradient(135deg, #8a2be2, #7a00d4);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(75, 0, 100, 0.6);
      }

      &:active {
        transform: translateY(0);
        box-shadow: 0 2px 4px rgba(75, 0, 100, 0.3);
      }
    }
  }

  .bottom-decoration {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 15px;
    padding: 15px 0;
    position: relative;
    z-index: 1;

    .decoration-line {
      width: 80px;
      height: 2px;
      background: linear-gradient(90deg, transparent, rgba(147, 112, 219, 0.6), transparent);
      margin: 0 15px;
      border-radius: 1px;
      box-shadow: 0 0 8px rgba(147, 112, 219, 0.3);
    }

    .decoration-text {
      color: #9370db;
      font-size: 11px;
      font-weight: bold;
      text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
      letter-spacing: 2px;
      opacity: 0.8;
      background: rgba(15, 0, 20, 0.6);
      padding: 4px 12px;
      border-radius: 12px;
      border: 1px solid rgba(147, 112, 219, 0.3);
    }
  }
}

.player-row {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 6px;
  transition: all 0.3s ease;
  border-left: 4px solid transparent;
  background: rgba(15, 0, 20, 0.5);

  &:hover {
    background: rgba(45, 0, 60, 0.6);
    transform: translateX(3px);
    box-shadow: 0 2px 8px rgba(75, 0, 100, 0.4);
  }

  &:last-child {
    margin-bottom: 0; /* 最后一行不需要底部间距 */
  }

  &.rank-gold {
    border-left-color: #8a2be2;
    background: rgba(75, 0, 100, 0.4);
  }

  &.rank-silver {
    border-left-color: #9932cc;
    background: rgba(65, 0, 85, 0.4);
  }

  &.rank-bronze {
    border-left-color: #8b008b;
    background: rgba(55, 0, 75, 0.4);
  }

  &.rank-normal {
    border-left-color: rgba(75, 0, 100, 0.6);
    background: rgba(25, 0, 35, 0.3);
  }
}

.rank-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 12px;
  min-width: 30px;

  .rank-number {
    color: #8a2be2;
    font-weight: bold;
    font-size: 14px;
    text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.9);
  }

  .rank-medal {
    font-size: 12px;
    margin-top: 2px;
  }
}

.player-info {
  flex: 1;
  min-width: 0;

  .player-name {
    color: #d8bfd8;
    font-weight: bold;
    font-size: 14px;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .player-stats {
    display: flex;
    gap: 8px;

    .stat {
      color: #e6e6fa;
      font-size: 12px;
      background: rgba(45, 0, 60, 0.7);
      padding: 3px 8px;
      border-radius: 4px;
      border: 1px solid rgba(75, 0, 100, 0.6);
    }
  }
}

.leaderboard-container::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(45deg, #8b008b, #9370db, #8b008b);
  border-radius: 10px;
  z-index: -1;
  opacity: 0.3;
}

@media (max-width: 768px) {
  .leaderboard-container {
    width: 250px;
    left: 10px;
    top: 20px;
  }

  .tower-filter .scroll-container {
    padding: 6px 0;

    .scroll-indicator {
      width: 20px;
      height: 20px;

      .scroll-arrow {
        font-size: 16px;
      }
    }
  }
}
.my-info-bar {
  margin-top: 10px;
  padding: 10px;
  background: rgba(15, 0, 20, 0.5);
  border: 1px solid rgba(75, 0, 100, 0.6);
  border-radius: 6px;
  box-shadow: inset 0 0 10px rgba(75, 0, 100, 0.3);
  color: #e6e6fa;
}

.my-info-title {
  font-size: 14px;
  font-weight: bold;
  color: #8a2be2;
  margin-bottom: 8px;
  text-shadow: 0 0 6px rgba(147, 112, 219, 0.4);
}

.my-info-loading,
.my-info-error,
.my-info-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.my-info-loading .spinner {
  border: 3px solid rgba(147, 112, 219, 0.3);
  border-top: 3px solid #9370db;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  animation: spin 1s linear infinite;
}

.my-info-error .error-text {
  color: #ff6b6b;
}

.my-info-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.my-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.my-rank .label {
  font-size: 12px;
  color: #9370db;
  margin-right: 6px;
}

.my-rank .value {
  font-weight: 800;
  color: #ffd700;
  text-shadow: 0 0 6px rgba(255, 215, 0, 0.4);
}

.my-name {
  color: #d8bfd8;
  font-weight: 600;
  max-width: 150px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: right;
}

.my-meta-row {
  display: flex;
  gap: 8px;
}

.my-meta-row .meta {
  color: #e6e6fa;
  font-size: 12px;
  background: rgba(45, 0, 60, 0.7);
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid rgba(75, 0, 100, 0.6);
}

.retry-button {
  background: linear-gradient(135deg, #9370db, #8a2be2);
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  border: none;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(75, 0, 100, 0.4);
}

.retry-button:hover {
  background: linear-gradient(135deg, #8a2be2, #7a00d4);
  transform: translateY(-1px);
}
</style>
