// stores/authStore.js
import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('token') || null,
        user: JSON.parse(localStorage.getItem('user')) || null, // 新增user状态
    }),
    actions: {
        setToken(newToken) {
            this.token = newToken;
            if (newToken) {
                localStorage.setItem('token', newToken);
            } else {
                localStorage.removeItem('token');
            }
        },
        setUser(userData) {
            this.user = userData;
            if (userData) {
                localStorage.setItem('user', JSON.stringify(userData));
            } else {
                localStorage.removeItem('user');
            }
        },
        clearToken() {
            this.token = null;
            localStorage.removeItem('token');
            this.user = null;
            localStorage.removeItem('user');
        }
    },
    getters: {
        getToken: (state) => state.token,
        getUser: (state) => state.user,
    }
});
