<template>
  <!-- 帖子列表 -->
  <div class="my-posts-container">
    <!-- 分页信息 -->
    <div class="pagination-info">
      第 {{ currentPage }} 页 / 共 {{ totalPages }} 页
    </div>

    <div v-for="post in paginatedPosts" :key="post.id" class="post-card">
      <div class="post-header">
        <div class="user-info">
          <div class="avatar">
            <img :src="post.user.avatar" :alt="post.user.name" />
          </div>
          <div class="user-details">
            <h3 class="username">{{ post.user.name }}</h3>
            <p class="post-time">{{ post.createdAt }}</p>
          </div>
        </div>
        <div class="post-actions">
          <button 
            class="action-btn like-btn" 
            @click="toggleLike(post)"
            :disabled="likeLoading.has(post.id)"
          >
            <span v-if="likeLoading.has(post.id)" class="loading">⏳</span>
            <span v-else-if="post.liked" class="liked">❤️</span>
            <span v-else class="unliked">🤍</span>
            <span class="like-count">{{ post.likes }}</span>
          </button>
        </div>
      </div>

      <div class="post-content">
        <h2 class="post-title">{{ post.title }}</h2>
        <p class="post-text">{{ post.content }}</p>
      </div>

      <!-- 评论区域 -->
      <div class="comments-section">
        <div class="comment-form">
          <input
            type="text"
            v-model="newComments[post.id]"
            placeholder="添加评论..."
            class="comment-input"
            @keyup.enter="addComment(post.id)"
          />
          <button @click="addComment(post.id)" class="comment-btn">发送</button>
        </div>

        <div class="comments-list">
          <div
            v-for="comment in post.commentsExpanded
              ? post.comments
              : visibleComments(post.comments)"
            :key="comment.id"
            class="comment"
          >
            <div class="comment-user">
              <img
                :src="comment.user.avatar"
                :alt="comment.user.name"
                class="comment-avatar"
              />
              <span class="comment-username">{{ comment.user.name }}</span>
            </div>
            <p class="comment-text">{{ comment.text }}</p>
            <p class="comment-time">{{ comment.createdAt }}</p>
          </div>

          <div class="comments-toggle" v-if="post.comments.length > 2">
            <span
              v-if="post.commentsExpanded"
              @click="post.commentsExpanded = false"
              class="toggle-btn"
            >
              收起评论
            </span>
            <span
              v-else
              @click="post.commentsExpanded = true"
              class="toggle-btn"
            >
              查看更多评论({{ post.comments.length - 2 }})
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 无帖子提示 -->
    <div v-if="paginatedPosts.length === 0" class="no-posts">
      <p>还没有任何帖子，快发表你的想法吧！</p>
    </div>

    <!-- 分页控件 -->
    <div class="pagination-controls">
      <button
        class="pagination-btn prev-next"
        :disabled="currentPage === 1"
        @click="changePage(currentPage - 1)"
      >
        上一页
      </button>

      <div class="page-numbers">
        <button
          v-for="page in visiblePages"
          :key="page"
          class="pagination-btn number"
          :class="{ active: page === currentPage }"
          @click="changePage(page)"
        >
          {{ page }}
        </button>
      </div>

      <button
        class="pagination-btn prev-next"
        :disabled="currentPage === totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue';
import { studentService } from "@/services/api";
import studentAvatar from '@/assets/student-avatar.png'
import { getCurrentUser, isUserLoggedIn } from "@/utils/userUtils";

// 获取当前登录用户信息
const currentUser = computed(() => getCurrentUser());

// 新评论对象
const newComments = ref({});
// 当前页码
const currentPage = ref(1);
// 每页显示的帖子数量
const postsPerPage = 2;
// 帖子列表数据
const posts = ref([]);
// 加载状态
const likeLoading = ref(new Set()); // 用于跟踪正在点赞的帖子ID

const fetchPost = async() => {
  try{
    const response = await studentService.getOwnPosts(currentUser.value.id);
    posts.value = response.data.data.records.map(item => ({
      id: item.id,
      title: item.title,
      content: item.content,
      user:{
        id: item.userId,
        name: item.userName,
        avatar: "https://tse3-mm.cn.bing.net/th/id/OIP-C.MyVTP6gOD1WSIDQ8CIV1qAHaHa?w=167&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3"
      },
      likes: item.likeCount,
      liked: false,
      createdAt: item.createdTime,
      commentsExpanded: false,
      comments: []
    }))
    // console.log("post", posts.data);
    for(var i = 0; i < posts.value.length; i++){
      const responseComment =  await studentService.getAllComments(posts.value[i].id)
      if(responseComment.data){
        const comment = responseComment.data.data.records.map(item =>({
          id: item.commentId,
          text: item.content,
          user: {
            id: item.userId,
            name: item.userName,
            avatar: "https://tse3-mm.cn.bing.net/th/id/OIP-C.MyVTP6gOD1WSIDQ8CIV1qAHaHa?w=167&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3"
          },
          createAt: item.createdTime
        }))
        posts.value[i].comments = comment;
      }
    }
    
  }catch(e){
    console.log("error fetch");
  }
}

// 计算当前页显示的帖子
const paginatedPosts = computed(() => {
  const startIndex = (currentPage.value - 1) * postsPerPage;
  const endIndex = startIndex + postsPerPage;
  return posts.value.slice(startIndex, endIndex);
});

// 计算总页数
const totalPages = computed(() => {
  return Math.ceil(posts.value.length / postsPerPage);
});

// 计算可见页码（最多显示5页）
const visiblePages = computed(() => {
  const pages = [];
  const maxVisiblePages = 5;
  let startPage = Math.max(
    1,
    currentPage.value - Math.floor(maxVisiblePages / 2)
  );
  let endPage = startPage + maxVisiblePages - 1;

  if (endPage > totalPages.value) {
    endPage = totalPages.value;
    startPage = Math.max(1, endPage - maxVisiblePages + 1);
  }

  for (let i = startPage; i <= endPage; i++) {
    pages.push(i);
  }

  return pages;
});

// 格式化日期
// const formatDate = (date) => {
//   const now = new Date();
//   const diff = now - date;

//   // 一分钟内
//   if (diff < 1000 * 60) {
//     return "刚刚";
//   }

//   // 一小时内
//   if (diff < 1000 * 60 * 60) {
//     const minutes = Math.floor(diff / (1000 * 60));
//     return `${minutes}分钟前`;
//   }

//   // 一天内
//   if (diff < 1000 * 60 * 60 * 24) {
//     const hours = Math.floor(diff / (1000 * 60 * 60));
//     return `${hours}小时前`;
//   }

//   // 超过一天
//   return date.toLocaleDateString();
// };

// 点赞/取消点赞
const toggleLike = async(post) => {
  // 检查用户是否已登录
  if (!isUserLoggedIn()) {
    alert("请先登录后再进行点赞操作");
    return;
  }

  // 防止重复点击
  if (likeLoading.value.has(post.id)) {
    return;
  }

  likeLoading.value.add(post.id);

  try {
    if (post.liked) {
      // 取消点赞
      const response = await studentService.unlikePost(post.id);
      if (response.data && response.data.success) {
        post.liked = false;
        post.likes = Math.max(0, post.likes - 1);
      } else {
        alert("取消点赞失败，请重试");
      }
    } else {
      // 点赞
      const response = await studentService.likePost(post.id);
      if (response.data && response.data.success) {
        post.liked = true;
        post.likes += 1;
      } else {
        alert("点赞失败，请重试");
      }
    }
  } catch (error) {
    console.error("点赞操作失败:", error);
    alert("操作失败，请检查网络连接后重试");
  } finally {
    likeLoading.value.delete(post.id);
  }
};

// 添加评论
const addComment = async (postId) => {
  // 检查用户是否已登录
  if (!isUserLoggedIn()) {
    alert("请先登录后再发表评论");
    return;
  }

  const commentText = newComments.value[postId]?.trim();
  if (!commentText) return;

  try {
    // 构建评论数据
    const commentData = {
      id: 0,
      postId: postId,
      content: commentText,
      userId: currentUser.value.id,
      userName: currentUser.value.name,
      userAvatar: currentUser.value.avatar,
      createTime: new Date().toISOString()
    };

    // 调用API创建评论
    const response = await studentService.createComment(commentData);
    
    if (response.data && response.data.success) {
      // 评论创建成功，重新获取帖子列表以更新评论
      await fetchPost();
      newComments.value[postId] = "";
    } else {
      alert("评论发布失败，请重试");
    }
  } catch (error) {
    console.error("发布评论失败:", error);
    alert("评论发布失败，请检查网络连接后重试");
  }
};

// 计算可见的评论
const visibleComments = (comments) => {
  // 默认显示最近的2条评论
  return comments.slice(0, 2);
};

// 切换页面
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
};

onMounted(async() => {
  fetchPost();
})
</script>
<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
}

.community-section {
  max-width: 800px;
  margin: 20px auto;
  padding: 0 20px;
}

.my-posts-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  width: 900px;
  opacity: 0.8;
}


.pagination-info {
  background: #f8f9ff;
  padding: 12px 20px;
  font-weight: 600;
  color: #4a6bdf;
  border-bottom: 1px solid #eef1f7;
  text-align: center;
}

.post-card {
  margin: 20px;
  padding: 20px;
  border-radius: 10px;
  background: white;
  box-shadow: 0 3px 15px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f3ff;
  transition: all 0.3s ease;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #eef1f7;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.username {
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 5px;
}

.post-time {
  font-size: 0.85rem;
  color: #7f8c8d;
}

.post-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.95rem;
  padding: 6px 12px;
  border-radius: 30px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f7ff;
}

.action-btn:hover {
  background: #4a6bdf;
  color: white;
}

.like-btn {
  transition: all 0.3s ease;
  border-radius: 20px;
  padding: 8px 12px;
  min-width: 60px;
}

.like-btn:hover {
  background-color: #fff5f5;
  transform: scale(1.05);
}

.like-btn:active {
  transform: scale(0.95);
}

.liked {
  color: #ff5252;
  animation: heartBeat 0.3s ease-in-out;
}

.unliked {
  color: #666;
  transition: color 0.3s ease;
}

.like-count {
  margin-left: 4px;
  font-weight: 600;
  color: #333;
}

.loading {
  color: #999;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.like-btn:disabled:hover {
  transform: none;
  background-color: transparent;
}

@keyframes heartBeat {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.post-content {
  padding: 15px 0;
}

.post-title {
  font-size: 1.4rem;
  color: #2c3e50;
  margin-bottom: 12px;
  font-weight: 700;
}

.post-text {
  color: #34495e;
  line-height: 1.7;
  font-size: 1.05rem;
}

.comments-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f3ff;
}

.comment-form {
  display: flex;
  margin-bottom: 20px;
  background: #f8f9ff;
  border-radius: 30px;
  overflow: hidden;
}

.comment-input {
  flex-grow: 1;
  padding: 12px 20px;
  border: none;
  background: transparent;
  font-size: 1rem;
}

.comment-input:focus {
  outline: none;
}

.comment-btn {
  background: #4a6bdf;
  color: white;
  border: none;
  padding: 0 25px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.3s;
}

.comment-btn:hover {
  background: #3d5bbd;
}

.comments-list {
  margin-top: 10px;
}

.comment {
  padding: 12px 15px;
  margin-bottom: 10px;
  background: #f8f9ff;
  border-radius: 10px;
  position: relative;
}

.comment:after {
  content: "";
  position: absolute;
  top: -10px;
  left: 30px;
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-bottom: 10px solid #f8f9ff;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.comment-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-username {
  font-weight: 600;
  font-size: 0.95rem;
  color: #4a6bdf;
}

.comment-text {
  margin-left: 40px;
  font-size: 0.95rem;
  color: #2c3e50;
  line-height: 1.5;
}

.comment-time {
  margin-top: 5px;
  font-size: 0.8rem;
  color: #7f8c8d;
  text-align: right;
}

.comments-toggle {
  text-align: center;
  margin-top: 10px;
}

.toggle-btn {
  color: #4a6bdf;
  font-weight: 500;
  cursor: pointer;
  padding: 5px 12px;
  border-radius: 15px;
  display: inline-block;
  transition: all 0.2s;
}

.toggle-btn:hover {
  background: #f1f4ff;
}

.no-posts {
  text-align: center;
  padding: 40px 20px;
  color: #7f8c8d;
  font-size: 1.1rem;
}

.pagination-controls {
  display: flex;
  justify-content: center;
  padding: 20px;
  gap: 10px;
  margin-top: 10px;
  border-top: 1px solid #f0f3ff;
}

.pagination-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: white;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  border: 1px solid #e2e8f0;
}

.pagination-btn:hover:not(:disabled) {
  background: #4a6bdf;
  color: white;
  border-color: #4a6bdf;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-btn.active {
  background: #4a6bdf;
  color: white;
  border-color: #4a6bdf;
}

.page-numbers {
  display: flex;
  gap: 8px;
}
</style>
