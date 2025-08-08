<template>
  <div class="community-section">
    <!-- 发帖表单 -->
    <div class="post-form">
      <h2>发表新帖</h2>
      <div class="form-group">
        <input
          type="text"
          v-model="newPost.title"
          placeholder="输入标题"
          class="form-input"
        />
      </div>
      <div class="form-group">
        <textarea
          v-model="newPost.content"
          placeholder="分享你的想法..."
          class="form-textarea"
        ></textarea>
      </div>
      <div class="form-actions">
        <button 
          @click="submitPost" 
          class="submit-btn" 
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="posts-container">
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
            <button @click="addComment(post.id)" class="comment-btn">
              发送
            </button>
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
  </div>
</template>

<script setup>
import { studentService } from "@/services/api";
import create from "@ant-design/icons-vue/lib/components/IconFont";
import { ref, reactive, computed, onMounted } from "vue";
import { getCurrentUser, isUserLoggedIn } from "@/utils/userUtils";

// 获取当前登录用户信息
const currentUser = computed(() => getCurrentUser());

// const token = localStorage.getItem('authToken');
// const decoded = parseJwt(token);

// 新帖子数据
const newPost = reactive({
  title: "",
  content: "",
});

// 新评论数据
const newComments = ref({});

// 当前页码
const currentPage = ref(1);
// 每页帖子数量
const postsPerPage = 10;

// 加载状态
const isSubmitting = ref(false);
const likeLoading = ref(new Set()); // 用于跟踪正在点赞的帖子ID 

const fetchPost = async() => {
  try{
    const response = await studentService.getAllPosts();
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
      liked: false, // 初始化为false，稍后会更新
      createdAt: item.createTime,
      commentsExpanded: false,
      comments: []
    }))
    
    // 获取每个帖子的评论
    for(var i = 0; i < posts.value.length; i++){
      // 获取评论
      const responseComment = await studentService.getAllComments(posts.value[i].id)
      if(responseComment.data){
        const comment = responseComment.data.data.records.map(item =>({
          id: item.commentId,
          text: item.content,
          user: {
            id: item.userId,
            name: item.userName,
            avatar: "https://tse4-mm.cn.bing.net/th/id/OIP-C.mKNsBZtaT08geDERZr7NzQHaMp?w=115&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3"
          },
          createdAt: item.createTime
        }))
        posts.value[i].comments = comment;
      }
      

    }
    
  }catch(e){
    console.log("error fetch");
  }
}

// 帖子列表数据
const posts = ref([]);

const visibleComments = (comments) => {
  return comments.slice(0, 2);
};

// 计算分页后的帖子
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

// // 格式化日期
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

// 提交新帖子
const submitPost = async () => {
  // 检查用户是否已登录
  if (!isUserLoggedIn()) {
    alert("请先登录后再发布帖子");
    return;
  }

  if (!newPost.title.trim() || !newPost.content.trim()) {
    alert("标题和内容不能为空");
    return;
  }

  if (isSubmitting.value) return; // 防止重复提交

  isSubmitting.value = true;

  try {
    // 构建符合API要求的数据结构
    const postData = {
      id: 0,
      title: newPost.title,
      content: newPost.content,
      userId: currentUser.value.id,
      userName: currentUser.value.name,
      userAvatar: currentUser.value.avatar,
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      category: "general", // 可以根据需要设置分类
      createTime: new Date().toISOString(),
      updateTime: new Date().toISOString()
    };

    // 调用API创建帖子
    const response = await studentService.createPost(postData);
    
    if (response.data && response.data.success) {
      // 创建成功，重新获取帖子列表
      await fetchPost();
      
      // 清空表单
      newPost.title = "";
      newPost.content = "";
      
      // 回到第一页查看新帖子
      currentPage.value = 1;
      
      alert("帖子发布成功！");
    } else {
      alert("发布失败，请重试");
    }
  } catch (error) {
    console.error("发布帖子失败:", error);
    alert("发布失败，请检查网络连接后重试");
  } finally {
    isSubmitting.value = false;
  }
};

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

// 切换页面
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    // 滚动到顶部以便查看新页面内容
    const scrollContainer = document.querySelector(".posts-scroll-container");
    if (scrollContainer) {
      scrollContainer.scrollTop = 0;
    }
  }
};

onMounted(async() =>{
  fetchPost();
})
</script>

<style scoped>


.comments-toggle {
  margin-top: 10px;
  text-align: center;
}

.toggle-btn {
  color: #c2ccee;
  cursor: pointer;
  font-size: 0.9rem;
  padding: 5px 10px;
  border-radius: 15px;
  display: inline-block;
  transition: all 0.2s;
}

.toggle-btn:hover {
  background-color: #f1f4ff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.community-section {
  max-width: 900px;
  margin: 0 auto;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
}

.post-form {
  background: linear-gradient(120deg, #e2e8ff, #ffffff);
  opacity: 0.7;
  color: black;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  margin-bottom: 25px;
}

.post-form h2 {
  margin-top: 0;
  font-size: 1.6rem;
  margin-bottom: 15px;
}

.form-group {
  margin-bottom: 15px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 15px;
  border-radius: 8px;
  border: none;
  font-size: 1rem;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.4);
}

.form-textarea {
  min-height: 120px;
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.submit-btn {
  background-color: #ffd5d5;
  color: black;
  border: none;
  padding: 10px 25px;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

.submit-btn:hover {
  background-color: #ff5252;
  transform: translateY(-2px);
}

.submit-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.submit-btn:disabled:hover {
  background-color: #cccccc;
  transform: none;
}

.posts-container {
  background: white;
  opacity: 0.8;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.pagination-info {
  background: #f8f9ff;
  padding: 12px 20px;
  font-weight: 600;
  color: #4a6bdf;
  border-bottom: 1px solid #eef1f7;
  text-align: center;
}

.posts-scroll-container {
  max-height: 500px;
  overflow-y: auto;
  padding: 0 20px;
}

.posts-scroll-container::-webkit-scrollbar {
  width: 8px;
}

.posts-scroll-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.posts-scroll-container::-webkit-scrollbar-thumb {
  background: #c5cae9;
  border-radius: 4px;
}

.posts-scroll-container::-webkit-scrollbar-thumb:hover {
  background: #7986cb;
}

.post-card {
  margin: 20px 0;
  border-radius: 8px;
  background: white;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
  border: 1px solid #eef1f7;
  transition: all 0.3s ease;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #f0f3ff;
}

.user-info {
  display: flex;
  align-items: center;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  border: 2px solid #eef1f7;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.username {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2c3e50;
}

.post-time {
  margin: 0;
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
  padding: 6px 10px;
  border-radius: 5px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn:hover {
  background: #f5f7ff;
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
  padding: 15px;
}

.post-title {
  font-size: 1.3rem;
  color: #2c3e50;
  margin-top: 0;
  margin-bottom: 10px;
}

.post-text {
  color: #34495e;
  line-height: 1.6;
  margin: 0;
}

.comments-section {
  padding: 0 15px 15px;
}

.comment-form {
  display: flex;
  margin-top: 15px;
  margin-bottom: 15px;
}

.comment-input {
  flex-grow: 1;
  padding: 10px 15px;
  border: 1px solid #e0e3ff;
  border-radius: 20px 0 0 20px;
  font-size: 0.95rem;
}

.comment-input:focus {
  outline: none;
  border-color: #4a6bdf;
}

.comment-btn {
  background: #4a6bdf;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 0 20px 20px 0;
  cursor: pointer;
  font-size: 0.95rem;
  transition: background 0.3s;
}

.comment-btn:hover {
  background: #3d5bbd;
}

.comments-list {
  margin-top: 15px;
}

.comment {
  background: #f8f9ff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  border-left: 3px solid #4a6bdf;
}

.comment-user {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.comment-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 10px;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-username {
  font-size: 0.9rem;
  font-weight: 600;
  color: #4a6bdf;
}

.comment-text {
  margin: 0;
  font-size: 0.95rem;
  color: #2c3e50;
  padding-left: 40px;
}

.comment-time {
  margin: 5px 0 0;
  font-size: 0.8rem;
  color: #7f8c8d;
  padding-left: 40px;
}

.no-posts {
  text-align: center;
  padding: 30px;
  color: #7f8c8d;
  font-size: 1.1rem;
}

.pagination-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #f8f9ff;
  border-top: 1px solid #eef1f7;
}

.page-numbers {
  display: flex;
  gap: 5px;
}

.pagination-btn {
  padding: 8px 15px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.pagination-btn.prev-next {
  background: #f1f4ff;
  color: #4a6bdf;
}

.pagination-btn.prev-next:disabled {
  color: #b0b9d0;
  cursor: not-allowed;
}

.pagination-btn.prev-next:not(:disabled):hover {
  background: #4a6bdf;
  color: white;
}

.pagination-btn.number {
  min-width: 36px;
  background: white;
  color: #4a5568;
  border: 1px solid #e2e8f0;
}

.pagination-btn.number.active {
  background: #4a6bdf;
  color: white;
  border-color: #4a6bdf;
}

.pagination-btn.number:hover:not(.active) {
  background: #f1f4ff;
}
</style>
