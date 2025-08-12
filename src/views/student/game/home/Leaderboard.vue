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
                v-for="tower in 8"
                :key="tower"
                class="tower-tab horror-tower"
                :class="{ active: selectedTower === tower.toString() }"
                @click="selectTower(tower.toString())"
            >
              <div class="tab-icon">💀</div>
              <div class="tab-text">{{ tower }}塔</div>
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
      <div class="scroll-container">
        <div class="scroll-content" :style="{ transform: `translateY(${scrollOffset}px)` }">
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
                <span class="stat">{{ player.exp }}经验</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';

// 响应式变量
const selectedTower = ref('all');
const canScrollLeft = ref(false);
const canScrollRight = ref(true);
const scrollOffset = ref(0);
const scrollInterval = ref(null);
const towerTabs = ref(null);
const leaderboardList = ref(null);

// 存储事件监听器的引用，用于清理
const eventListeners = ref({
  touchstart: null,
  touchmove: null,
  touchend: null
});

// 排行榜数据
const leaderboardData = [
  // 总塔排行榜数据
  { name: '暗影之王', level: 15, towerLevel: 8, exp: 2500, rank: 1, totalRank: 1 },
  { name: '血月女巫', level: 14, towerLevel: 7, exp: 2300, rank: 2, totalRank: 2 },
  { name: '亡灵法师', level: 13, towerLevel: 7, exp: 2100, rank: 3, totalRank: 3 },
  { name: '深渊领主', level: 12, towerLevel: 6, exp: 1900, rank: 4, totalRank: 4 },
  { name: '混沌使者', level: 11, towerLevel: 6, exp: 1700, rank: 5, totalRank: 5 },
  { name: '暗夜行者', level: 10, towerLevel: 5, exp: 1500, rank: 6, totalRank: 6 },
  { name: '诅咒术士', level: 9, towerLevel: 5, exp: 1300, rank: 7, totalRank: 7 },
  { name: '恐惧骑士', level: 8, towerLevel: 4, exp: 1100, rank: 8, totalRank: 8 },
  { name: '阴影刺客', level: 7, towerLevel: 4, exp: 900, rank: 9, totalRank: 9 },
  { name: '死亡使者', level: 6, towerLevel: 3, exp: 700, rank: 10, totalRank: 10 },

  // 1塔到8塔数据省略...
  { name: '新手勇者', level: 5, towerLevel: 1, exp: 300, rank: 1, totalRank: 25 },
  { name: '见习法师', level: 4, towerLevel: 1, exp: 250, rank: 2, totalRank: 26 },
  { name: '初级战士', level: 3, towerLevel: 1, exp: 200, rank: 3, totalRank: 27 },
  { name: '进阶法师', level: 7, towerLevel: 2, exp: 500, rank: 1, totalRank: 20 },
  { name: '熟练战士', level: 6, towerLevel: 2, exp: 450, rank: 2, totalRank: 21 },
  { name: '初级射手', level: 5, towerLevel: 2, exp: 400, rank: 3, totalRank: 22 },
  { name: '高级法师', level: 9, towerLevel: 3, exp: 800, rank: 1, totalRank: 15 },
  { name: '精英战士', level: 8, towerLevel: 3, exp: 750, rank: 2, totalRank: 16 },
  { name: '熟练射手', level: 7, towerLevel: 3, exp: 700, rank: 3, totalRank: 17 },
  { name: '大师法师', level: 11, towerLevel: 4, exp: 1200, rank: 1, totalRank: 12 },
  { name: '传奇战士', level: 10, towerLevel: 4, exp: 1100, rank: 2, totalRank: 13 },
  { name: '神射手', level: 9, towerLevel: 4, exp: 1000, rank: 3, totalRank: 14 },
  { name: '至尊法师', level: 13, towerLevel: 5, exp: 1600, rank: 1, totalRank: 10 },
  { name: '战神', level: 12, towerLevel: 5, exp: 1500, rank: 2, totalRank: 11 },
  { name: '箭神', level: 11, towerLevel: 5, exp: 1400, rank: 3, totalRank: 12 },
  { name: '法神', level: 14, towerLevel: 6, exp: 2000, rank: 1, totalRank: 7 },
  { name: '武神', level: 13, towerLevel: 6, exp: 1900, rank: 2, totalRank: 8 },
  { name: '弓神', level: 12, towerLevel: 6, exp: 1800, rank: 3, totalRank: 9 },
  { name: '法王', level: 15, towerLevel: 7, exp: 2400, rank: 1, totalRank: 3 },
  { name: '武王', level: 14, towerLevel: 7, exp: 2300, rank: 2, totalRank: 4 },
  { name: '弓王', level: 13, towerLevel: 7, exp: 2200, rank: 3, totalRank: 5 },
  { name: '暗影之王', level: 15, towerLevel: 8, exp: 2500, rank: 1, totalRank: 1 },
  { name: '血月女巫', level: 14, towerLevel: 8, exp: 2400, rank: 2, totalRank: 2 },
  { name: '亡灵法师', level: 13, towerLevel: 8, exp: 2300, rank: 3, totalRank: 3 }
];

// 计算属性：过滤排行榜数据
const filteredLeaderboardData = computed(() => {
  if (selectedTower.value === 'all') {
    return leaderboardData
        .filter(player => player.totalRank)
        .sort((a, b) => a.totalRank - b.totalRank)
        .slice(0, 10);
  } else {
    const towerLevel = parseInt(selectedTower.value);
    return leaderboardData
        .filter(player => player.towerLevel === towerLevel)
        .sort((a, b) => a.rank - b.rank)
        .slice(0, 10);
  }
});

// 方法
const selectTower = (tower) => {
  selectedTower.value = tower;
  console.log(`切换到${tower === 'all' ? '总塔' : tower + '塔'}排行榜`);
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

// 生命周期钩子
onMounted(() => {
  nextTick(() => {
    updateScrollState();
    addTouchSupport();
    startAutoScroll();
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

.leaderboard-list {
  max-height: 300px;
  overflow: hidden;
  position: relative;

  .scroll-container {
    height: 100%;
    overflow: hidden;
  }

  .scroll-content {
    transition: transform 0.05s linear;
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
</style>
