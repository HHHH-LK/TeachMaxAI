<template>
  <div class="chapter-list-container">
    <h2>章节目录</h2>
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>正在加载章节数据...</p>
    </div>
    <ul v-else class="chapter-list">
      <li v-for="chapter in chapters" :key="chapter.id" class="chapter-item">
        <div class="chapter-container" @click="toggleChapter(chapter)">
          <div class="chapter-header">
            <h3>{{ chapter.title }}</h3>
            <el-icon :class="{ expanded: chapter.expanded }" class="expand-icon"><ArrowRight /></el-icon>
          </div>
          <p>{{ chapter.description }}</p>
          <transition name="fade">
            <div v-if="chapter.expanded" class="knowledge-point-list">
              <transition-group name="list" tag="div">
                <div v-for="point in getKnowledgePoints(chapter.id)" :key="point.id" class="knowledge-point-item">
                  <div class="point-header">
                    <span class="point-title">{{ point.title }}</span>
                  </div>
                  <p class="point-description">{{ point.description }}</p>
                </div>
              </transition-group>
            </div>
          </transition>
        </div>
      </li>
    </ul>
    <div v-if="!loading && chapters.length === 0" class="empty-container">
      <p>暂无章节数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { ArrowRight, Loading } from '@element-plus/icons-vue';
import { teacherService } from '@/services/api.js';
import { ElMessage } from 'element-plus';

// 定义props
const props = defineProps({
  courseId: {
    type: [String, Number],
    required: true
  }
});

const chapters = ref([]);
const allKnowledgePoints = ref({});
const loading = ref(false);

// 模拟数据函数
const getMockChapters = (courseId) => {
  // 根据课程ID返回不同的章节数据
  const courseChapters = {
    1: [ // Java
      { id: 1, title: '第一章：Java基础语法', description: 'Java语言基础语法和概念', expanded: false },
      { id: 2, title: '第二章：面向对象编程', description: 'Java面向对象编程思想和实践', expanded: false },
      { id: 3, title: '第三章：异常处理', description: 'Java异常处理机制', expanded: false },
      { id: 4, title: '第四章：集合框架', description: 'Java集合框架详解', expanded: false },
      { id: 5, title: '第五章：IO流操作', description: '文件和流的处理', expanded: false },
    ],
    2: [ // 数据结构与算法实验
      { id: 1, title: '第一章：线性表 ', description: '数组、链表等线性数据结构', expanded: false },
      { id: 2, title: '第二章：栈和队列', description: '栈和队列的实现与应用', expanded: false },
      { id: 3, title: '第三章：树和二叉树', description: '树的概念和二叉树操作', expanded: false },
      { id: 4, title: '第四章：图论基础', description: '图的表示和基本算法', expanded: false },
      // { id: 5, title: '第五章：项目实战', description: '完成一个综合项目，巩固所学内容。', expanded: false },
    ],
    3: [ //
      { id: 1, title: '第一章：软件工程概述', description: '介绍软件工程的基本概念和发展历程。', expanded: false },
      { id: 2, title: '第二章：需求工程', description: '讲解需求获取、分析和规格说明的方法。', expanded: false },
      { id: 3, title: '第三章：软件设计', description: '深入探讨软件架构设计和详细设计。', expanded: false },
      { id: 4, title: '第四章：软件测试', description: '通过实际案例，展示软件测试的方法和技术。', expanded: false },
      { id: 5, title: '第五章：软件维护', description: '完成软件维护和演化的综合内容。', expanded: false },
    ],
    4: [ // Web开发技术
      { id: 1, title: '第一章：Web基础', description: '介绍Web开发的基本概念和技术栈。', expanded: false },
      { id: 2, title: '第二章：HTML与CSS', description: '讲解HTML结构和CSS样式的设计。', expanded: false },
      { id: 3, title: '第三章：JavaScript编程', description: '深入探讨JavaScript语言特性和DOM操作。', expanded: false },
      { id: 4, title: '第四章：前端框架', description: '通过实际案例，展示Vue、React等框架的应用。', expanded: false },
      { id: 5, title: '第五章：后端开发', description: '完成Node.js、数据库等后端技术的综合应用。', expanded: false },
    ],
    5: [ // 人工智能导论
      { id: 1, title: '第一章：AI概述', description: '介绍人工智能的基本概念和发展历史。', expanded: false },
      { id: 2, title: '第二章：机器学习基础', description: '讲解机器学习的基本原理和算法。', expanded: false },
      { id: 3, title: '第三章：深度学习', description: '深入探讨神经网络和深度学习技术。', expanded: false },
      { id: 4, title: '第四章：自然语言处理', description: '通过实际案例，展示NLP技术的应用。', expanded: false },
      { id: 5, title: '第五章：计算机视觉', description: '完成图像识别和视觉AI的综合应用。', expanded: false },
    ],
    6: [ // 数据库系统实验
      { id: 1, title: '第一章：数据库基础', description: '介绍数据库的基本概念和关系模型。', expanded: false },
      { id: 2, title: '第二章：SQL语言', description: '讲解SQL查询语言和数据库操作。', expanded: false },
      { id: 3, title: '第三章：数据库设计', description: '深入探讨数据库设计和规范化理论。', expanded: false },
      { id: 4, title: '第四章：事务管理', description: '通过实际案例，展示事务处理和并发控制。', expanded: false },
      { id: 5, title: '第五章：数据库优化', description: '完成数据库性能优化和调优的综合内容。', expanded: false },
    ]
  };

  return {
    data: courseChapters[courseId] || courseChapters[2], // 默认返回数据结构课程
    success: true,
    message: '获取章节数据成功'
  };
};

const getMockKnowledgePoints = (courseId, chapterId) => {
  // 根据课程ID和章节ID返回不同的知识点数据
  const courseKnowledgePoints = {
    1: { // Java
      1: [
        { id: 101, title: '变量和数据类型', description: 'Java中的基本数据类型和变量声明' },
        { id: 102, title: '运算符', description: 'Java中的各种运算符使用' },
        { id: 103, title: '控制结构', description: 'if-else, for, while等控制语句' },
      ],
      2: [
        { id: 201, title: '类和对象', description: '面向对象编程的核心概念' },
        { id: 202, title: '继承', description: 'Java继承机制' },
        { id: 203, title: '封装', description: 'Java封装特性' },
        { id: 204, title: '多态', description: 'Java多态性实现'},
      ],
      3: [
        { id: 301, title: '异常处理机制', description: 'try-catch-finally语句' },
      ],
      4: [
        { id: 401, title: 'ArrayList', description: 'ArrayList集合的使用' },
        { id: 402, title: 'HashMap', description: 'HashMap的原理和使用' },
      ],
    },
    2: { // 数据结构与算法实验
      1: [
        { id: 101, title: '数组', description: '一维和多维数组' },
        { id: 102, title: '链表', description: '单链表、双链表' },
      ],
      2: [
        { id: 201, title: '栈操作', description: '栈的基本操作' },
        { id: 202, title: '队列操作', description: '队列的基本操作' },
      ],
      3: [
        { id: 301, title: '二叉树遍历', description: '前序、中序、后序遍历' },
      ],
      4: [
        { id: 401, title: '图的表示', description: '邻接矩阵和邻接表' },
      ],
    },
    3: { // 软件工程研讨
      1: [
        { id: 101, title: '软件工程概述', description: '介绍软件工程的基本概念和发展历程。' },
        { id: 102, title: '软件生命周期', description: '学习软件开发的各个阶段和过程模型。' },
      ],
      2: [
        { id: 201, title: '需求获取', description: '讲解需求获取的方法和技术。' },
        { id: 202, title: '需求分析', description: '学习需求分析和建模技术。' },
        { id: 203, title: '需求规格说明', description: '掌握需求文档的编写规范。' },
      ],
      3: [
        { id: 301, title: '软件架构设计', description: '介绍软件架构的设计原则和模式。' },
        { id: 302, title: '详细设计', description: '学习模块设计和接口设计。' },
      ],
      4: [
        { id: 401, title: '测试策略', description: '学习软件测试的策略和方法。' },
        { id: 402, title: '测试技术', description: '掌握单元测试、集成测试等技术。' },
      ],
      5: [
        { id: 501, title: '软件维护', description: '探讨软件维护的类型和过程。' },
        { id: 502, title: '软件演化', description: '学习软件演化的管理和控制。' },
      ],
    },
    4: { // Web开发技术
      1: [
        { id: 101, title: 'Web基础', description: '介绍Web开发的基本概念和技术栈。' },
        { id: 102, title: 'HTTP协议', description: '学习HTTP协议的基本原理。' },
      ],
      2: [
        { id: 201, title: 'HTML基础', description: '讲解HTML标签和页面结构。' },
        { id: 202, title: 'CSS样式', description: '学习CSS选择器和样式设计。' },
        { id: 203, title: '响应式设计', description: '掌握响应式网页设计技术。' },
      ],
      3: [
        { id: 301, title: 'JavaScript基础', description: '介绍JavaScript语言特性。' },
        { id: 302, title: 'DOM操作', description: '学习DOM树的操作和事件处理。' },
      ],
      4: [
        { id: 401, title: 'Vue.js框架', description: '学习Vue.js的基本用法和组件开发。' },
        { id: 402, title: 'React框架', description: '掌握React的核心概念和Hooks。' },
      ],
      5: [
        { id: 501, title: 'Node.js后端', description: '学习Node.js服务器端开发。' },
        { id: 502, title: '数据库集成', description: '掌握数据库的连接和操作。' },
      ],
    },
    5: { // 人工智能导论
      1: [
        { id: 101, title: 'AI概述', description: '介绍人工智能的基本概念和发展历史。' },
        { id: 102, title: 'AI应用领域', description: '学习人工智能在各个领域的应用。' },
      ],
      2: [
        { id: 201, title: '机器学习基础', description: '讲解机器学习的基本原理和算法。' },
        { id: 202, title: '监督学习', description: '学习分类和回归算法。' },
        { id: 203, title: '无监督学习', description: '掌握聚类和降维技术。' },
      ],
      3: [
        { id: 301, title: '神经网络', description: '介绍神经网络的基本结构和原理。' },
        { id: 302, title: '深度学习', description: '学习深度神经网络的训练方法。' },
      ],
      4: [
        { id: 401, title: '自然语言处理', description: '学习文本处理和语言模型。' },
        { id: 402, title: '机器翻译', description: '掌握机器翻译的基本技术。' },
      ],
      5: [
        { id: 501, title: '计算机视觉', description: '学习图像识别和目标检测。' },
        { id: 502, title: '视觉AI应用', description: '掌握视觉AI在实际项目中的应用。' },
      ],
    },
    6: { // 数据库系统实验
      1: [
        { id: 101, title: '数据库基础', description: '介绍数据库的基本概念和关系模型。' },
        { id: 102, title: '数据模型', description: '学习实体关系模型和关系模型。' },
      ],
      2: [
        { id: 201, title: 'SQL基础', description: '讲解SQL语言的基本语法。' },
        { id: 202, title: '查询语句', description: '学习SELECT语句和复杂查询。' },
        { id: 203, title: '数据操作', description: '掌握INSERT、UPDATE、DELETE操作。' },
      ],
      3: [
        { id: 301, title: '数据库设计', description: '介绍数据库设计的原则和方法。' },
        { id: 302, title: '规范化理论', description: '学习关系数据库的规范化。' },
      ],
      4: [
        { id: 401, title: '事务管理', description: '学习事务的ACID特性和并发控制。' },
        { id: 402, title: '锁机制', description: '掌握数据库锁的类型和使用。' },
      ],
      5: [
        { id: 501, title: '性能优化', description: '学习数据库查询优化技术。' },
        { id: 502, title: '索引设计', description: '掌握数据库索引的设计原则。' },
      ],
    }
  };

  const knowledgePoints = courseKnowledgePoints[courseId]?.[chapterId] || courseKnowledgePoints[2]?.[chapterId] || [];

  return {
    data: knowledgePoints,
    success: true,
    message: '获取知识点数据成功'
  };
};

// 获取章节数据
const loadChapters = async () => {
  if (!props.courseId) return;

  loading.value = true;
  try {
    const response = await teacherService.getChapterByCourseId(props.courseId);
    if (response.data.success) {
      // 将后端数据格式转换为组件需要的格式
      chapters.value = response.data.data.map(chapter => ({
        id: chapter.chapterId,
        title: chapter.chapterName,
        description: chapter.description,
        expanded: false,
        // 保留原始数据以备后用
        originalData: chapter
      }));
    } else {
      ElMessage.error('获取章节数据失败');
    }
  } catch (error) {
    // 如果后端没有数据，使用模拟数据
    console.warn('后端章节数据获取失败，使用模拟数据:', error.message);
    const mockResponse = getMockChapters(props.courseId);
    chapters.value = mockResponse.data;
  } finally {
    loading.value = false;
  }
};

// 获取知识点数据
const loadKnowledgePoints = async (chapterId) => {
  if (!props.courseId || !chapterId) return;

  try {
    const response = await teacherService.getKnowledgePointsByChapterId(chapterId);
    if (response.data.success) {
      // 将后端数据格式转换为组件需要的格式
      allKnowledgePoints.value[chapterId] = response.data.data.map(point => ({
        id: point.pointId,
        title: point.pointName,
        description: point.description,
        // 保留原始数据以备后用
        originalData: point
      }));
    }
  } catch (error) {
    // 如果后端没有数据，使用模拟数据
    console.warn('后端知识点数据获取失败，使用模拟数据:', error.message);
    const mockResponse = getMockKnowledgePoints(props.courseId, chapterId);
    allKnowledgePoints.value[chapterId] = mockResponse.data;
  }
};

const toggleChapter = async (chapter) => {
  chapter.expanded = !chapter.expanded;

  // 当展开章节时，加载该章节的知识点数据
  if (chapter.expanded && !allKnowledgePoints.value[chapter.id]) {
    await loadKnowledgePoints(chapter.id);
  }
};

const getKnowledgePoints = (chapterId) => {
  return allKnowledgePoints.value[chapterId] || [];
};

// 监听courseId变化
watch(() => props.courseId, (newCourseId) => {
  if (newCourseId) {
    loadChapters();
    allKnowledgePoints.value = {};
  }
}, { immediate: true });

onMounted(() => {
  if (props.courseId) {
    loadChapters();
  }
});
</script>

<style scoped>
.chapter-list-container {
  padding: 30px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  max-width: 1200px;
  margin: 40px auto;
}

h2 {
  font-size: 28px;
  color: #2c3e50;
  text-align: center;
  margin-bottom: 30px;
  font-weight: bold;
  position: relative;
}

h2::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -10px;
  transform: translateX(-50%);
  width: 60px;
  height: 4px;
  background-color: #409EFF;
  border-radius: 2px;
}

.chapter-list {
  list-style: none;
  padding: 0;
}

.chapter-item {
  background-color: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 15px;
  padding: 20px 25px;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.chapter-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border-color: #409EFF;
}

.chapter-container {
  cursor: pointer;
}

.chapter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.chapter-header h3 {
  margin-bottom: 0;
}

.expand-icon {
  font-size: 18px;
  color: #409EFF;
  transition: transform 0.3s ease;
}

.chapter-header.expanded .expand-icon {
  transform: rotate(90deg);
}

h3 {
  font-size: 20px;
  color: #34495e;
  margin-top: 0;
  margin-bottom: 8px;
  font-weight: 600;
}

p {
  font-size: 15px;
  color: #7f8c8d;
  line-height: 1.8;
  margin-bottom: 0;
}

/* 知识点列表样式 */
.knowledge-point-list {
  margin-top: 20px;
}

.knowledge-point-item {
  position: relative;
  background-color: #ffffff;
  border: 1px solid #e0e0e0;
  border-left: 3px solid #427ab9;
  border-radius: 8px;
  padding: 15px 20px;
  margin-bottom: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.knowledge-point-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.point-header {
  margin-bottom: 8px;
}

.point-title {
  font-size: 18px;
  color: #34495e;
  font-weight: 600;
}

.point-description {
  font-size: 14px;
  color: #7f8c8d;
  line-height: 1.6;
}

/* 加载和空状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
}

.loading-container .el-icon {
  font-size: 32px;
  color: #409EFF;
  animation: rotate 1s linear infinite;
}

.loading-container p {
  margin-top: 16px;
  font-size: 16px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}
</style>