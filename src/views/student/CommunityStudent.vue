<template>
  <div class="box">
    <chat-background />
  </div>

  <el-scrollbar class="content" height="100%" native="true">
    <!-- 标题 -->
    <div class="title-word">
      <h1 class="title">社区交流</h1>
      <h2 class="sub-title">分享你的想法</h2>
    </div>

    <!-- 标签页组件 -->
    <div class="tab">
      <div class="container">
        <div class="tabs">
          <input
            type="radio"
            id="radio-1"
            name="tabs"
            :checked="activeTab === 'community'"
            @click="setActiveTab('community')"
          />
          <label class="tab" for="radio-1">社区</label>

          <input
            type="radio"
            id="radio-2"
            name="tabs"
            :checked="activeTab === 'my-posts'"
            @click="setActiveTab('my-posts')"
          />
          <label class="tab" for="radio-2">我的帖子</label>

          <span class="glider"></span>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="community" v-if="activeTab === 'community'">
      <community-content />
    </div>
    <div class="my-posts" v-else-if="activeTab === 'my-posts'">
      <my-community />
    </div>
  </el-scrollbar>
</template>

<script setup>
import { ref, watch } from "vue";
import ChatBackground from "@/components/student/chat/ChatBackground.vue";
import CommunityContent from "@/components/student/chat/CommunityContent.vue";
import MyCommunity from "@/components/student/chat/MyCommunity.vue";
import { ElMessage } from "element-plus";

// 当前激活 tab
const activeTab = ref("community");

const setActiveTab = (tab) => {
  activeTab.value = tab;
};
</script>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap");
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "Inter", "PingFang SC", "Microsoft YaHei", sans-serif;
}

:root {
  --primary-color: #a1a7c3;
  --primary-light: #eef2ff;
  --secondary-color: #b2abbf;
  --text-primary: #2c3e50;
  --text-secondary: #7ff8c8d;
  --text-light: #b0b0b0;
  --background: #f8f9fa;
  --card-bg: #ffffff;
  --border-radius: 16px;
  --shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  --transition: all 0.3s ease;
}

body {
  background-color: var(--background);
  color: var(--text-primary);
  overflow: hidden;
}

.my-posts{
  display: flex;
  justify-content: center;
  align-content: center;
}


.box {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4edf5 100%);
  z-index: 1;
}

.content {
  position: relative;
  max-height: 100vh;
  padding: 0 20px;
  overflow: auto;
  z-index: 2;
}

.title-word {
  text-align: center;
  margin: 40px 0 28px;
  padding: 0 16px;
}

.title {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  background: linear-gradient(
    90deg,
    var(--primary-color),
    var(--secondary-color)
  );
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  letter-spacing: -0.5px;
}

.sub-title {
  font-size: 18px;
  font-weight: 400;
  color: var(--text-secondary);
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.6;
}

.tab-container {
  max-width: 800px;
  margin: 0 auto 32px;
  padding: 0 16px;
}

.tabs {
  display: flex;
  position: relative;
  background-color: var(--card-bg);
  box-shadow: var(--shadow);
  padding: 8px;
  border-radius: var(--border-radius);
  overflow: hidden;
}

input[type="radio"] {
  display: none;
}

.tab {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  flex: 1;
  min-width: 0;
  font-size: 18px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  color: var(--text-secondary);
  position: relative;
  z-index: 2;
  border-radius: 12px;
  margin: 0 auto; /* Ensures the tabs are centered */
  padding-bottom: 1rem;
}

.tab:hover {
  color: var(--primary-color);
}

input[type="radio"]:checked + label {
  color: white;
}

.glider {
  position: absolute;
  height: 56px;
  background: linear-gradient(
    100deg,
    var(--primary-color),
    var(--secondary-color)
  );
  z-index: 1;
  border-radius: 12px;
  transition: transform 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  box-shadow: 0 4px 12px rgba(197, 208, 255, 0.3);
}

input[id="radio-1"]:checked ~ .glider {
  transform: translateX(0);
}

input[id="radio-2"]:checked ~ .glider {
  transform: translateX(100%);
}

.tab-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px 40px;
}

.community-content,
.my-community {
  background: var(--card-bg);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: 32px;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.placeholder {
  text-align: center;
  color: var(--text-light);
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 24px;
  color: var(--primary-light);
}

.placeholder-text {
  font-size: 18px;
  max-width: 400px;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .title {
    font-size: 26px;
  }

  .sub-title {
    font-size: 16px;
  }

  .tab {
    font-size: 16px;
    height: 48px;
  }

  .glider {
    height: 48px;
  }

  .community-content,
  .my-community {
    padding: 24px 16px;
  }

  .placeholder-icon {
    font-size: 48px;
  }

  .placeholder-text {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .content {
    padding: 0 12px;
  }

  .title-word {
    margin: 32px 0 24px;
  }

  .title {
    font-size: 22px;
  }

  .sub-title {
    font-size: 14px;
  }

  .tab {
    font-size: 15px;
    height: 44px;
    padding: 0 8px;
  }

  .glider {
    height: 44px;
  }

  .tab-container {
    margin-bottom: 24px;
  }
}
</style>
