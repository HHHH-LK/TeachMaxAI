<template>
  <div class="user-management">
    <div class="toolbar">
      <el-radio-group v-model="filterRole">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="管理员">管理员</el-radio-button>
        <el-radio-button label="教师">教师</el-radio-button>
        <el-radio-button label="学生">学生</el-radio-button>
      </el-radio-group>
      <el-button type="primary" @click="addUser">新增用户</el-button>
    </div>
    
    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名或邮箱"
        style="width: 300px"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    
    <el-table :data="paginatedUsers" stripe style="width: 100%">
      <el-table-column prop="name" label="姓名" width="180" />
      <el-table-column prop="role" label="角色" width="180" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="createdAt" label="创建日期" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" @click="editUser(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteUser(scope.row)"
          >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页组件 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalUsers"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="管理员" value="管理员"></el-option>
            <el-option label="教师" value="教师"></el-option>
            <el-option label="学生" value="学生"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
              type="password"
              show-password
              v-model="form.newPassword"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelForm">取消</el-button>
          <el-button type="primary" @click="saveUser">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { ElMessageBox, ElMessage } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { adminService, authService } from "@/services/api";

const users = ref([
  {
    id: 1,
    name: "王科飞",
    role: "管理员",
    email: "wang@example.com",
    createdAt: "2023-01-15",
  },
  {
    id: 2,
    name: "张冬梅",
    role: "教师",
    email: "zhang@example.com",
    createdAt: "2023-02-20",
  },
  {
    id: 3,
    name: "陈清泉",
    role: "教师",
    email: "chenqing@example.com",
    createdAt: "2023-02-22",
  },
  {
    id: 4,
    name: "李芭比",
    role: "教师",
    email: "liba@example.com",
    createdAt: "2023-04-11",
  },
  {
    id: 5,
    name: "李贵阳",
    role: "学生",
    email: "ligui@example.com",
    createdAt: "2023-03-10",
  },
  {
    id: 6,
    name: "胡海涛",
    role: "学生",
    email: "huhai@example.com",
    createdAt: "2023-04-01",
  },
  {
    id: 7,
    name: "陈小明",
    role: "学生",
    email: "xiaoming@example.com",
    createdAt: "2023-03-20",
  },
  {
    id: 8,
    name: "刘小红",
    role: "学生",
    email: "xiaohong@example.com",
    createdAt: "2023-02-11",
  },
  {
    id: 9,
    name: "张小华",
    role: "学生",
    email: "xiaohua@example.com",
    createdAt: "2023-03-21",
  },
  {
    id: 10,
    name: "孙小亮",
    role: "学生",
    email: "xiaoliang@example.com",
    createdAt: "2023-02-22",
  },
  {
    id: 11,
    name: "周小雯",
    role: "学生",
    email: "xiaowen@example.com",
    createdAt: "2023-04-02",
  },
  {
    id: 12,
    name: "冯小琴",
    role: "学生",
    email: "xiaoqin@example.com",
    createdAt: "2023-03-14",
  },
  {
    id: 13,
    name: "韩小慧",
    role: "学生",
    email: "xiaohui@example.com",
    createdAt: "2023-02-19",
  },
  {
    id: 14,
    name: "吴小强",
    role: "学生",
    email: "xiaoqiang@example.com",
    createdAt: "2023-03-11",
  },
]);

const filterRole = ref("");
const searchKeyword = ref("");
const currentPage = ref(1);
const pageSize = ref(10);

// 过滤和搜索用户
const filteredUsers = computed(() => {
  let filtered = users.value;
  
  // 按角色过滤
  if (filterRole.value) {
    filtered = filtered.filter((user) => user.role === filterRole.value);
  }
  
  // 按关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    filtered = filtered.filter((user) => 
      user.name.toLowerCase().includes(keyword) || 
      user.email.toLowerCase().includes(keyword)
    );
  }
  
  return filtered;
});

// 分页后的用户数据
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredUsers.value.slice(start, end);
});

// 总用户数
const totalUsers = computed(() => filteredUsers.value.length);

const dialogVisible = ref(false);
const dialogTitle = ref("");
const form = ref({
  id: null,
  name: "",
  role: "",
  newPassword: "",
});

const addUser = () => {
  dialogTitle.value = "新增用户";
  form.value = { id: null, name: "", role: "", newPassword: "" };
  dialogVisible.value = true;
};

const editUser = (user) => {
  dialogTitle.value = "编辑用户";
  form.value = { ...user };
  dialogVisible.value = true;
};

const saveUser = async () => {
  if (form.value.id) {
    try {
      // 准备后端所需的数据格式
      const editRequest = {
        userId: form.value.id,
        realName: form.value.name,
        userType: convertRoleToType(form.value.role),
        email: form.value.email,
      };

      console.log("realName", editRequest.realName);
      // 调用API更新用户信息
      const response = await adminService.editUser(editRequest);

      // 检查响应是否成功
      if (response.data && response.data.code === 0) {
        // 更新本地用户列表
        const index = users.value.findIndex((u) => u.id === form.value.id);
        if (index !== -1) {
          users.value[index] = { ...form.value };
        }
        ElMessage.success("用户更新成功");
      } else {
        throw new Error(response.data?.msg || "更新失败");
      }
    } catch (error) {
      console.error("更新用户失败:", error);
      ElMessage.error(`更新失败: ${error.message}`);
    }
  } else {
    try {
      // 表单验证
      if (!form.value.name || !form.value.role || !form.value.newPassword) {
        ElMessage.error("请填写所有必填字段");
        return;
      }

      // 准备注册数据（符合接口要求）
      const registerData = {
        username: form.value.name, // 使用姓名作为用户名
        password: form.value.newPassword,
        repassword: form.value.newPassword, // 确认密码用相同值
        userType: convertRoleToType(form.value.role), // 转换角色类型
      };

      // 调用注册API
      const response = await authService.register(registerData);

      // 检查响应状态
      if (response.data && response.data.code === 0) {
        // 使用API返回的用户信息创建新用户
        const newUser = {
          id: response.data.data.userId, // 使用后端生成的ID
          name: form.value.name,
          role: form.value.role,
          email: "默认邮箱", // 注册接口不返回邮箱
          createdAt: new Date().toISOString().split("T")[0],
        };

        // 添加到本地用户列表
        users.value.push(newUser);
        ElMessage.success("用户添加成功");
      } else {
        throw new Error(response.data?.msg || "添加失败");
      }
    } catch (error) {
      console.error("添加用户失败:", error);
      ElMessage.error(`添加失败: ${error.message || "服务器错误"}`);
    }
    dialogVisible.value = false;
  }
};

// 角色转换函数（从中文转后端标识符）
function convertRoleToType(role) {
  const roleMap = {
    管理员: "admin",
    教师: "teacher",
    学生: "student",
  };
  return roleMap[role] || role;
}

const cancelForm = () => {
  dialogVisible.value = false;
};

// 搜索处理函数
const handleSearch = () => {
  currentPage.value = 1; // 重置到第一页
};

// 分页大小改变处理
const handleSizeChange = (newSize) => {
  pageSize.value = newSize;
  currentPage.value = 1; // 重置到第一页
};

// 当前页改变处理
const handleCurrentChange = (newPage) => {
  currentPage.value = newPage;
};

// 监听过滤条件变化，重置分页
watch([filterRole, searchKeyword], () => {
  currentPage.value = 1;
});

const deleteUser = (user) => {
  ElMessageBox.confirm(`确定要删除用户 “${user.name}” 吗?`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
      .then(async () => {
        try {
          // 调用删除API，使用user.id作为userId
          const response = await adminService.deleteUser(user.id);

          // 检查删除是否成功
          if (response.data && response.data.code === 0) {
            // 从本地用户列表中移除
            users.value = users.value.filter((u) => u.id !== user.id);
            ElMessage.success("删除成功");
          } else {
            throw new Error(response.data?.msg || "删除操作失败");
          }
        } catch (error) {
          console.error("删除用户失败:", error);
          ElMessage.error(`删除失败: ${error.message || "未知错误"}`);
        }
      })
      .catch(() => {
        // 用户取消操作
        ElMessage.info("已取消删除");
      });
};

const getAllUsers = async () => {
  try {
    const response = await adminService.getAllUsers();

    // 调试日志
    console.log("API完整响应:", response);

    // 检查响应结构
    if (!response || !response.data || response.data.code === undefined) {
      throw new Error("API返回结构无效");
    }

    // 检查业务状态码
    if (response.data.code !== 0) {
      throw new Error(response.data.msg || "未知的API错误");
    }

    // 检查用户数据数组
    if (!response.data.data || !Array.isArray(response.data.data)) {
      throw new Error("用户数据格式无效");
    }

    console.log("用户数据:", response.data.data);

    const userList = response.data.data
        .map((item) => {
          // 检查用户对象是否存在
          if (!item || !item.user) {
            console.warn("用户项缺少user对象:", item);
            return null;
          }

          const user = item.user;
          let roleName = "";

          // 安全获取用户类型
          const userType = user.userType || "unknown";

          // 根据 userType 确定角色名称
          switch (userType.toLowerCase()) {
            case "admin":
              roleName = "管理员";
              break;
            case "teacher":
              roleName = "教师";
              break;
            case "student":
              roleName = "学生";
              break;
            default:
              roleName = userType;
          }

          let createdAt = "";
          if (user.createdAt && typeof user.createdAt === "string") {
            try {
              // 使用split方法前检查是否为字符串
              createdAt = user.createdAt.split("T")[0];
            } catch (e) {
              console.warn("日期格式解析错误:", user.createdAt, e);
              createdAt = user.createdAt;
            }
          } else {
            // createdAt为null或undefined时设置默认值
            createdAt = user.createdAt || "未知日期";
          }

          return {
            id: user.userId || 0,
            name: user.realName || "未知用户",
            role: roleName,
            email: user.email || "",
            createdAt,
          };
        })
        .filter((item) => item !== null);

    return userList;
  } catch (error) {
    console.error("获取用户数据失败:", error);

    // 提供更多调试信息
    if (error.response) {
      console.error("API响应详情:", error.response.data);
    }

    return [];
  }
};

onMounted(async () => {
  users.value = await getAllUsers();
  // 初始化分页
  currentPage.value = 1;
});
</script>

<style scoped>
.user-management {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  margin-bottom: 16px;
  justify-content: space-between;
}

.search-bar {
  margin-bottom: 16px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
