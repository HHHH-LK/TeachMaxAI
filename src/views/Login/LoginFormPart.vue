<template>
  <div class="box"></div>
  <div class="content container-fluid">
    <div class="login">
      <input
        type="checkbox"
        name="form-type"
        id="form-type"
        v-model="isRegister"
        @change="resetForms"
      />
      <div class="form-type-text">
        <span>login</span>
        <span>register</span>
      </div>
      <label for="form-type"></label>
      <div class="login-form">
        <form class="form-front" @submit.prevent="handleLogin">
          <h1>LOGIN</h1>
          <div class="form-input usernameInput">
            <input
              type="text" v-model="loginForm.username"
              placeholder="用户名"
              required
            />
          </div>
          <div class="form-input passwordInput">
            <input
              type="password" v-model="loginForm.password"
              placeholder="密码"
              required
            />
          </div>
          <div class="forgot-password">
            <a href="#" @click.prevent="showForgotPassword">忘记密码？</a>
          </div>
          <div class="form-submit">
            <input type="submit" value="登录" />
          </div>
        </form>

        <form class="form-back" @submit.prevent="handleRegister">
          <h1>REGISTER</h1>
          <div class="role-selection">
            <div class="role-option">
              <input
                type="radio"
                id="student"
                v-model="registerForm.userType"
                value="student"
                required
              />
              <label for="student">学生</label>
            </div>
            <div class="role-option">
              <input
                type="radio"
                id="teacher"
                v-model="registerForm.userType"
                value="teacher"
              />
              <label for="teacher">教师</label>
            </div>
          </div>
          <div class="form-input CusernameInput">
            <input
              type="text" v-model="registerForm.username"
              placeholder="用户名"
              required
            />
          </div>
          <div class="form-input CpasswordInput">
            <input
              type="password" v-model="registerForm.password"
              placeholder="密码"
              required
            />
          </div>
          <div class="form-input Cpassword2Input">
            <input
              type="password" v-model="registerForm.confirmPassword"
              placeholder="此次密码需和上次相同"
              required
            />
          </div>
          <div class="form-submit">
            <input type="submit" value="立即注册" />
          </div>
        </form>
      </div>
    </div>

    <div v-if="showForgotModal" class="forgot-password-modal" @click="closeForgotPassword">
      <div class="forgot-password-content" @click.stop>
        <div class="modal-header">
          <h2>忘记密码</h2>
          <button class="close-btn" @click="closeForgotPassword">&times;</button>
        </div>

        <div class="forgot-step">
          <p>请输入您的用户名和新密码</p>
          <div class="form-input">
            <input
              type="text" v-model="forgotPasswordForm.username"
              placeholder="请输入用户名"
              required
            />
          </div>
          <div class="form-input">
            <input
              type="password" v-model="forgotPasswordForm.newPassword"
              placeholder="请输入新密码"
              required
            />
          </div>
          <div class="form-input">
            <input
              type="password" v-model="forgotPasswordForm.confirmNewPassword"
              placeholder="请确认新密码"
              required
            />
          </div>
          <div class="form-submit">
            <button @click="resetPassword" :disabled="!forgotPasswordForm.username || !forgotPasswordForm.newPassword || !forgotPasswordForm.confirmNewPassword">
              重置密码
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { authService } from "@/services/api";
import router from "@/router/index.js";
import {useAuthStore} from "@/store/authStore.js";

export default {
  setup() {
    // 登录表单数据
    const loginForm = ref({
      username: "",
      password: "",
    });

    // 注册表单数据
    const registerForm = ref({
      userType: "",
      username: "",
      password: "",
      confirmPassword: "",
    });

    // 忘记密码相关状态
    const showForgotModal = ref(false);

    // 忘记密码表单数据
    const forgotPasswordForm = ref({
      username: "",
      newPassword: "",
      confirmNewPassword: "",
    });

    // 重置表单数据
    const resetForms = () => {
      loginForm.value = { username: "", password: "" };
      registerForm.value = { username: "", password: "", confirmPassword: "" };
    };

    // 处理登录请求
    const handleLogin = async () => {
      try {
        const response = await authService.login({
          principal: "account",
          username: loginForm.value.username,
          password: loginForm.value.password,
        });

        if (response.data.code === 0) {
          const tokenWithPrefix = response.data.data;

          const token = tokenWithPrefix.split(":")[1];
          const payloadBase64 = token.split(".")[1];
          const payloadBase64Url = payloadBase64
            .replace(/-/g, "+")
            .replace(/_/g, "/");
          const padLength = 4 - (payloadBase64Url.length % 4);
          const paddedPayload =
            payloadBase64Url + (padLength < 4 ? "=".repeat(padLength) : "");
          const jsonPayload = decodeURIComponent(
            Array.from(atob(paddedPayload))
              .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
              .join("")
          );
          const payload = JSON.parse(jsonPayload);
          const userRole = payload.role;

          // 保存用户信息到store
          const userInfo = {
            id: payload.userId || payload.sub,
            username: payload.username || payload.sub,
            role: userRole,
            // 可以根据需要添加更多用户信息
          };

          useAuthStore().setToken(token);
          useAuthStore().setUser(userInfo);

          console.log(userRole)
          ElMessage.success("登录成功");
          router.push(`/${userRole}`);
        } else {
          ElMessage.error(`登录失败: ${response.data.message}`);
        }
      } catch (error) {
        ElMessage.error(`登录错误: ${error.message}`);
      }
    };

    const handleRegister = async () => {
      try {
        if(registerForm.value.userType == null){
          ElMessage.error("需要选择角色！");
          return;
        }

        const response = await authService.register({
          userType: registerForm.value.userType,
          username: registerForm.value.username,
          password: registerForm.value.password,
          repassword: registerForm.value.confirmPassword
        });

        if (response.data.code === 0) {
          ElMessage.success("注册成功！请登录");
          resetForms();
        } else {
          ElMessage.error(`注册失败: ${response.data.message}`);
        }
      } catch (error) {
        ElMessage.error(`注册错误: ${error.message}`);
      }
    };

    // 忘记密码相关函数
    const showForgotPassword = () => {
      showForgotModal.value = true;
      resetForgotPasswordForm();
    };

    const closeForgotPassword = () => {
      showForgotModal.value = false;
      resetForgotPasswordForm();
    };

    const resetForgotPasswordForm = () => {
      forgotPasswordForm.value = {
        username: "",
        newPassword: "",
        reNewPassword: "",
      };
    };

    const resetPassword = async () => {
      try {
        if (!forgotPasswordForm.value.username || !forgotPasswordForm.value.newPassword || !forgotPasswordForm.value.confirmNewPassword) {
          ElMessage.error("请填写完整的信息");
          return;
        }

        if (forgotPasswordForm.value.newPassword !== forgotPasswordForm.value.confirmNewPassword) {
          ElMessage.error("两次输入的密码不一致");
          return;
        }

        // 调用重置密码的API
        const response = await authService.resetPassword({
          username: forgotPasswordForm.value.username,
          newPassword: forgotPasswordForm.value.newPassword,
          reNewPassword: forgotPasswordForm.value.newPassword,
        });

        if (response.data.code === 0) {
          ElMessage.success("密码重置成功，请使用新密码登录");
          closeForgotPassword();
        } else {
          ElMessage.error(`重置失败: ${response.data.message}`);
        }
      } catch (error) {
        ElMessage.error(`重置密码错误: ${error.message}`);
      }
    };

    // 清理定时器
    // onUnmounted(() => {
    //   if (countdownTimer.value) {
    //     clearInterval(countdownTimer.value);
    //   }
    // });

    return {
      loginForm,
      registerForm,
      showForgotModal,
      forgotPasswordForm,
      resetForms,
      handleLogin,
      handleRegister,
      showForgotPassword,
      closeForgotPassword,
      resetPassword,
    };
  },
};
</script>
<style scoped>
.content {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  filter: drop-shadow(0px 20px 10px rgba(0, 0, 0, 0.5));
}

.login {
  width: 500px;
  height: auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-radius: 1rem;
  padding: 1rem;
}

.form-type-text {
  display: inline-block;
  font-weight: 500;
  font-size: 1.2rem;
  font-family: "Lucida Console", Courier, monospace;
  color: #565859;
  letter-spacing: 0.1rem;
  margin: 0.5rem 1rem;
}

.form-type-text span {
  margin: 0.5rem 0.5rem;
}

#form-type {
  display: none;
}

#form-type ~ label {
  width: 3rem;
  height: 1rem;
  background: #ecf0f1;
  border-radius: 0.5rem;
  position: relative;
  cursor: pointer;
}

/* 伪选择器 */
#form-type ~ label::before {
  content: "";
  width: 1.5rem;
  height: 1.5rem;
  position: absolute;
  background-color: rgba(68, 64, 65, 0.6);
  border-radius: 0.75rem;
  border: 0.1rem solid #565859;
  top: -25%;
  left: -25%;
  transition: 0.5s;
}

#form-type:checked ~ label::before {
  background-color: rgba(68, 64, 65, 0.6);
  border: 0.1rem solid #585858;
  left: 75%;
}

.login-form {
  margin: 1rem 0;
  position: relative;
  perspective: 800px;
}

.form-front {
  width: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-items: center;
  padding: 4rem 4rem;
  border-radius: 1rem;
  position: absolute;
  top: 0;
  backface-visibility: hidden;
  transition: 0.5s;
}

/* 新增角色选择框样式 */
.role-selection {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin: 20px 0;
  width: 100%;
}

.role-option {
  display: flex;
  align-items: center;
}

.role-selection input[type="radio"] {
  /* 隐藏默认的单选按钮 */
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.role-selection label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: 16px;
  color: #565859;
  position: relative;
  padding-left: 30px;
}

.role-selection label::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border: 2px solid #565859;
  border-radius: 50%;
  background: white;
  transition: all 0.3s;
}

.role-selection input[type="radio"]:checked + label::before {
  background-color: #565859;
  border-color: #565859;
}

.role-selection input[type="radio"]:checked + label::after {
  content: "";
  position: absolute;
  left: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  z-index: 1;
}

/* 确保表单背面的内容不会太挤 */
.form-back {
  padding: 40px 40px; /* 增加内边距 */
}

.form-back {
  width: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2.5rem 4rem;
  border-radius: 1rem;
  backface-visibility: hidden;
  transform: rotateY(-180deg);
  transition: 0.5s;
}

#form-type:checked ~ .login-form > .form-front {
  transform: rotateY(180deg);
}

#form-type:checked ~ .login-form > .form-back {
  transform: rotateY(0deg);
}

.form-front h1,
.form-back h1 {
  color: #565859;
}

.form-input {
  width: 100%;
  padding: 1rem 1rem;
  margin: 0.5rem 0.5rem;
  background-color: white;
  border-radius: 0.4rem;
  position: relative;
  border: 3px solid transparent;
}

.form-input input {
  width: 75%;
  padding: 0rem 1rem;
  outline: none;
  border: none;
  background-color: white;
  font-size: 0.8rem;
}

.form-submit {
  width: 80%;
}

.form-submit input {
  border-radius: 5rem;
  font-family: 黑体;
  width: 100%;
  padding: 0.5rem 1rem;
  background-color: white;
  outline: none;
  border: none;
  color: black;
  transition: 0.3s;
}

.form-front .form-submit input:hover {
  background-color: #c8c8c8;
  color: white;
}

.form-back .form-submit input:hover {
  background-color: #bfc9de;
  color: rgb(184, 184, 184);
}

.forgot-password {
  margin: 0.5rem 0;
}

.forgot-password a {
  color: #565859;
  text-decoration: none;
  font-size: 0.8rem;
}

.forgot-password a:hover {
  text-decoration: underline;
}

/* 忘记密码弹窗样式 */
.forgot-password-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.forgot-password-content {
  background-color: white;
  padding: 2rem;
  border-radius: 1rem;
  width: 400px;
  max-width: 90%;
  position: relative;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.modal-header h2 {
  margin: 0;
  color: #565859;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #565859;
}

.close-btn:hover {
  color: #000;
}

.forgot-step {
  text-align: center;
}

.forgot-step p {
  margin-bottom: 1rem;
  color: #565859;
}

.forgot-step .form-input {
  margin: 1rem 0;
  padding: 0;
  background-color: transparent;
  border: none;
}

.forgot-step .form-input input {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 0.4rem;
  font-size: 0.9rem;
  box-sizing: border-box;
}

.forgot-step .form-submit {
  width: 100%;
  margin-top: 1rem;
}

.forgot-step .form-submit button {
  width: 100%;
  padding: 0.8rem;
  background-color: #565859;
  color: white;
  border: none;
  border-radius: 0.4rem;
  cursor: pointer;
  font-size: 0.9rem;
  transition: 0.3s;
}

.forgot-step .form-submit button:hover:not(:disabled) {
  background-color: #404040;
}

.forgot-step .form-submit button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.resend-code {
  margin: 0.5rem 0;
  font-size: 0.8rem;
}

.resend-code a {
  color: #565859;
  text-decoration: none;
}

.resend-code a:hover {
  text-decoration: underline;
}

.prompt {
  padding: 1rem 0rem;
  line-height: 1rem;
  color: white;
}

@media screen and (max-width: 1600px) {
  html {
    font-size: 14px;
  }
  .login {
    width: 450px;
  }
  .form-back,
  .form-front {
    height: 350px;
  }
}

@media screen and (max-width: 960px) {
  .login {
    width: 100%;
  }
  .form-input {
    width: 100%;
  }
  .forgot-password-content {
    width: 90%;
    margin: 0 1rem;
  }
}
</style>