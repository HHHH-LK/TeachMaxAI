import { useAuthStore } from '@/store/authStore.js';

class ChatSSEService {
    constructor() {
        this.controllers       = new Map();   // AbortController
        this.messageHandlers   = new Map();   // 用户回调
        this.reconnectTimeouts = new Map();   // 重连定时器
        this.heartbeatTimers   = new Map();   // 心跳定时器

        this.baseURL              = 'http://localhost:8108';
        this.maxReconnectAttempts = Infinity; // 无限重连
        this.initialDelay         = 1000;     // 首次重连 1s
        this.maxDelay             = 30000;    // 最大重连 30s
    }

    /* ---------------- 建立连接 ---------------- */
    async connect(userId, handlers = {}) {
        this.disconnect(userId);             // 先清理旧连接
        const token = useAuthStore().getToken;
        if (!token) {
            const err = new Error('未找到认证token');
            handlers.onError?.(err);
            throw err;
        }

        this.messageHandlers.set(userId, handlers);
        const controller = new AbortController();
        this.controllers.set(userId, controller);

        try {
            const url = `${this.baseURL}/api/chat/events/${userId}`;
            const res = await fetch(url, {
                method: 'GET',
                headers: {
                    token,
                    Accept: 'text/event-stream',
                    'Cache-Control': 'no-cache',
                    Connection: 'keep-alive'
                },
                signal: controller.signal
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            console.log(`[SSE] 连接成功: ${userId}`);
            handlers.onConnected?.({ userId });
            this.startHeartbeat(userId, token);   // 心跳
            this.readStream(userId, res.body);    // 读取流
        } catch (e) {
            console.error(`[SSE] 连接失败: ${userId}`, e);
            this.scheduleReconnect(userId, handlers);
        }
    }

    /* ---------------- 读取流 ---------------- */
    async readStream(userId, body) {
        const reader = body.getReader();
        const decoder = new TextDecoder();
        let buf = '';

        try {
            while (true) {
                const { value, done } = await reader.read();
                if (done) {
                    console.log(`[SSE] 流结束: ${userId}`);
                    break;
                }
                buf += decoder.decode(value, { stream: true });
                const lines = buf.split('\n');
                buf = lines.pop() || '';

                let event = '', data = '';
                for (const ln of lines) {
                    const l = ln.trim();
                    if (l.startsWith('event:')) event = l.slice(6).trim();
                    else if (l.startsWith('data:')) data = l.slice(5).trim();
                    else if (l === '' && event && data) {
                        this.dispatch(userId, event, data);
                        event = ''; data = '';
                    }
                }
            }
        } catch (e) {
            if (e.name !== 'AbortError') {
                console.error(`[SSE] 读取错误: ${userId}`, e);
            }
        } finally {
            reader.releaseLock?.();
            this.clearTimers(userId);
            this.scheduleReconnect(userId, this.messageHandlers.get(userId));
        }
    }

    /* ---------------- 事件分发 ---------------- */
    dispatch(userId, eventType, raw) {
        const h = this.messageHandlers.get(userId);
        if (!h) return;
        try {
            const data = JSON.parse(raw);
            switch (eventType) {
                case 'connected':      h.onConnected?.(data); break;
                case 'ai_stream_message': h.onAiStreamMessage?.(data); break;
                case 'chat_message':   h.onChatMessage?.(data); break;
                default:               console.warn(`[SSE] 未知事件: ${eventType}`);
            }
        } catch {
            console.warn('[SSE] 非 JSON 数据', raw);
        }
    }

    /* ---------------- 心跳保活 ---------------- */
    startHeartbeat(userId, token) {
        this.clearHeartbeat(userId);
        const timer = setInterval(async () => {
            if (!this.controllers.has(userId)) return this.clearHeartbeat(userId);
            try {
                await fetch(`${this.baseURL}/api/chat/ping/${userId}`, {
                    method: 'POST',
                    headers: { token }
                });
            } catch {
                /* ignore */
            }
        }, 2000);
        this.heartbeatTimers.set(userId, timer);
    }

    /* ---------------- 重连 ---------------- */
    scheduleReconnect(userId, handlers) {
        const attempt = (this.reconnectTimeouts.get(userId)?.attempt || 0) + 1;
        const delay   = Math.min(this.initialDelay * 2 ** (attempt - 1), this.maxDelay);

        console.log(`[SSE] ${attempt} 秒后重连: ${userId}`);
        const t = setTimeout(() => this.connect(userId, handlers), delay);
        this.reconnectTimeouts.set(userId, { timer: t, attempt });
    }

    /* ---------------- 断开 ---------------- */
    disconnect(userId) {
        this.clearTimers(userId);
        this.controllers.get(userId)?.abort();
        this.controllers.delete(userId);
        this.messageHandlers.delete(userId);
        this.reconnectTimeouts.delete(userId);
        console.log(`[SSE] 已断开: ${userId}`);
    }

    disconnectAll() {
        [...this.controllers.keys()].forEach(id => this.disconnect(id));
    }

    isConnected(userId) {
        return this.controllers.has(userId);
    }

    /* ---------------- 工具 ---------------- */
    clearTimers(userId) {
        this.clearHeartbeat(userId);
        const to = this.reconnectTimeouts.get(userId);
        if (to) {
            clearTimeout(to.timer);
            this.reconnectTimeouts.delete(userId);
        }
    }
    clearHeartbeat(userId) {
        const t = this.heartbeatTimers.get(userId);
        if (t) clearInterval(t);
        this.heartbeatTimers.delete(userId);
    }
}

/* 单例 */
const chatSSEService = new ChatSSEService();
if (typeof window !== 'undefined') {
    window.addEventListener('beforeunload', () => chatSSEService.disconnectAll());
}
export default chatSSEService;