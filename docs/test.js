import { useAuthStore } from "@/store/authStore.js";

class ChatSSEService {
    constructor() {
        this.controllers = new Map();       // userId -> AbortController
        this.handlers = new Map();          // userId -> handlers
        this.reconnectAttempts = new Map(); // userId -> number
        this.maxReconnectAttempts = 3;
        this.reconnectDelay = 1000; // ms
        this.baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8108";
    }

    getAuthToken() {
        const authStore = useAuthStore();
        return authStore.getToken;
    }

    buildSSEUrl(userId) {
        return `${this.baseURL}/api/chat/events/${encodeURIComponent(userId)}`;
    }

    isConnected(userId) {
        return this.controllers.has(String(userId));
    }

    getConnectedUsers() {
        return Array.from(this.controllers.keys());
    }

    async connect(userId, handlers = {}) {
        userId = String(userId);

        // 如果已连接，只更新处理器即可（避免重复连接/断开）
        if (this.isConnected(userId)) {
            this.handlers.set(userId, handlers);
            return;
        }

        const token = this.getAuthToken();
        if (!token) {
            const err = new Error("未找到认证token");
            handlers.onError && handlers.onError(err);
            throw err;
        }

        this.handlers.set(userId, handlers);
        const controller = new AbortController();
        this.controllers.set(userId, controller);

        try {
            const url = this.buildSSEUrl(userId);
            const resp = await fetch(url, {
                method: "GET",
                headers: {
                    Accept: "text/event-stream",
                    "Cache-Control": "no-cache",
                    Connection: "keep-alive",
                    token
                },
                signal: controller.signal,
                mode: "cors"
            });

            if (!resp.ok) {
                throw new Error(`HTTP ${resp.status}: ${resp.statusText}`);
            }

            // 注意：真正的“连接成功”以服务端 event: connected 为准
            // 这里不提前回调，以免造成重复
            this.reconnectAttempts.set(userId, 0);

            this.readSSEStream(userId, resp.body, handlers);
        } catch (error) {
            this.handleError(userId, error);
            throw error;
        }
    }

    async readSSEStream(userId, stream, handlers) {
        const reader = stream.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        let currentEvent = "";
        let currentDataLines = [];

        const flushEvent = () => {
            if (!currentEvent) return;
            const dataStr = currentDataLines.join("\n");
            this.handleSSEEvent(userId, currentEvent, dataStr, handlers);
            currentEvent = "";
            currentDataLines = [];
        };

        try {
            while (true) {
                const { value, done } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value, { stream: true });
                buffer += chunk;

                const lines = buffer.split("\n");
                buffer = lines.pop() || "";

                for (const rawLine of lines) {
                    const line = rawLine.trimEnd();

                    if (line.startsWith("event:")) {
                        // 新事件，先冲洗上一次
                        flushEvent();
                        currentEvent = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        currentDataLines.push(line.substring(5).trim());
                    } else if (line === "") {
                        // 事件结束
                        flushEvent();
                    }
                    // 其他字段忽略（id:, retry:）
                }
            }
        } catch (error) {
            if (error.name !== "AbortError") {
                this.handleError(userId, error);
            }
        } finally {
            try {
                reader.releaseLock();
            } catch (_) {}
        }
    }

    handleSSEEvent(userId, eventType, eventData, handlers) {
        // console.log(`[SSE] ${eventType} -> ${eventData}`);
        let data = eventData;
        try {
            data = JSON.parse(eventData);
        } catch (_) {
            // 非JSON，保持原样字符串
        }

        switch (eventType) {
            case "connected":
                handlers.onConnected && handlers.onConnected(data);
                break;
            case "heartbeat":
                handlers.onHeartbeat && handlers.onHeartbeat(data);
                break;
            case "ai_stream_message":
                handlers.onAiStreamMessage && handlers.onAiStreamMessage(data);
                break;
            case "chat_message":
                handlers.onChatMessage && handlers.onChatMessage(data);
                break;
            default:
                // console.log(`[SSE] 未处理的事件: ${eventType}`);
                break;
        }
    }

    handleError(userId, error) {
        const handlers = this.handlers.get(userId);
        handlers && handlers.onError && handlers.onError(error);

        const attempts = this.reconnectAttempts.get(userId) || 0;
        if (attempts < this.maxReconnectAttempts) {
            this.reconnectAttempts.set(userId, attempts + 1);
            setTimeout(() => this.reconnect(userId), this.reconnectDelay * (attempts + 1));
        } else {
            this.disconnect(userId);
        }
    }

    reconnect(userId) {
        userId = String(userId);
        const handlers = this.handlers.get(userId);
        handlers && handlers.onReconnect && handlers.onReconnect();

        // 取消旧连接
        const controller = this.controllers.get(userId);
        if (controller) {
            try { controller.abort(); } catch (_) {}
            this.controllers.delete(userId);
        }

        // 重新连接
        this.connect(userId, handlers).catch(() => {});
    }

    disconnect(userId) {
        userId = String(userId);
        const controller = this.controllers.get(userId);
        if (controller) {
            try { controller.abort(); } catch (_) {}
            this.controllers.delete(userId);
        }
        this.handlers.delete(userId);
        this.reconnectAttempts.delete(userId);
        // console.log(`[SSE] 断开: ${userId}`);
    }

    disconnectAll() {
        for (const [userId, controller] of this.controllers.entries()) {
            try { controller.abort(); } catch (_) {}
        }
        this.controllers.clear();
        this.handlers.clear();
        this.reconnectAttempts.clear();
    }

    async getOnlineCount() {
        try {
            const token = this.getAuthToken();
            const resp = await fetch(`${this.baseURL}/api/chat/online-count`, {
                headers: { token }
            });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const json = await resp.json();
            return json.data || 0;
        } catch (e) {
            console.error("获取在线人数失败:", e);
            return 0;
        }
    }

    async isUserOnline(userId) {
        try {
            const token = this.getAuthToken();
            const resp = await fetch(`${this.baseURL}/api/chat/is-online/${encodeURIComponent(userId)}`, {
                headers: { token }
            });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const json = await resp.json();
            return json.data === true;
        } catch (e) {
            console.error("检查用户在线状态失败:", e);
            return false;
        }
    }
}

const chatSSEService = new ChatSSEService();

// 页面卸载时清理所有连接
if (typeof window !== "undefined") {
    window.addEventListener("beforeunload", () => {
        chatSSEService.disconnectAll();
    });
}

export default chatSSEService;
