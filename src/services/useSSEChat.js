import { useAuthStore } from '@/store/authStore.js';

export function useSSEChat() {
  const sendSSEMessage = async (options) => {
    const { userId, question, onMessage, onComplete, onError } = options;

    return new Promise((resolve, reject) => {
      let isComplete = false;
      let reader = null;
      let controller = null;

      const cleanup = () => {
        if (controller) {
          controller.abort();
        }
        if (reader) {
          try {
            reader.cancel();
          } catch (e) {
            // 忽略取消错误
          }
        }
      };

      let timeoutId = null;

      const complete = () => {
        if (!isComplete) {
          isComplete = true;
          if (timeoutId) {
            clearTimeout(timeoutId);
          }
          setTimeout(cleanup, 100);
          onComplete?.();
          resolve();
        }
      };

      const handleError = (error) => {
        if (!isComplete) {
          isComplete = true;
          if (timeoutId) {
            clearTimeout(timeoutId);
          }
          setTimeout(cleanup, 100);
          onError?.(error);
          reject(error);
        }
      };

      const processSSE = async () => {
        try {
          const authStore = useAuthStore();
          const token = authStore.getToken;

          controller = new AbortController();

          timeoutId = setTimeout(() => {
            if (!isComplete) {
              console.log('⏰ 10分钟超时，取消连接');
              controller.abort();
              handleError(new Error('连接超时（10分钟）'));
            }
          }, 600000);

          console.log('🚀 开始SSE连接...');

          const response = await fetch('http://localhost:8108/chat/stream', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'token': token,
              'Accept': 'text/event-stream',
              'Cache-Control': 'no-cache',
              'Connection': 'keep-alive'
            },
            body: JSON.stringify({ userId, question }),
            signal: controller.signal,
            timeout: 600000
          });

          if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${await response.text()}`);
          }

          console.log('✅ SSE连接成功');

          reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';
          let hasReceivedData = false;

          while (!isComplete) {
            try {
              const { value, done } = await reader.read();

              if (done) {
                console.log('📖 流结束');
                if (hasReceivedData && !isComplete) {
                  console.log('⏳ 等待可能的延迟完成事件...');
                  await new Promise(resolve => setTimeout(resolve, 30000));
                }
                break;
              }

              if (value) {
                hasReceivedData = true;
                const chunk = decoder.decode(value, { stream: true });
                buffer += chunk;

                const events = buffer.split('\n\n');
                buffer = events.pop() || '';

                for (const event of events) {
                  if (isComplete) break;

                  const lines = event.split('\n');
                  let eventType = '';
                  let eventData = '';

                  for (const line of lines) {
                    if (line.startsWith('event:')) {
                      eventType = line.substring(6).trim();
                    } else if (line.startsWith('data:')) {
                      eventData = line.substring(5).trim();
                    }
                  }

                  if (eventData) {
                    console.log(`📨 收到事件: ${eventType} -> ${eventData}`);

                    switch (eventType) {
                      case 'thinking':
                        onMessage?.('🤔 正在思考...');
                        break;
                      case 'chunk':
                        // 在这里处理 ¥ 符号，逐字符发送给前端
                        for (let i = 0; i < eventData.length; i++) {
                          const char = eventData[i];
                          onMessage?.(char);
                        }
                        break;
                      case 'completed':
                        console.log('✅ 收到完成事件');
                        complete();
                        return;
                      case 'error':
                        console.error('❌ 收到错误事件:', eventData);
                        handleError(new Error(eventData));
                        return;
                      default:
                        if (eventData && eventData !== 'keep-alive') {
                          // 对于其他事件类型，也逐字符发送
                          for (let i = 0; i < eventData.length; i++) {
                            const char = eventData[i];
                            onMessage?.(char);
                          }
                        }
                    }
                  }
                }
              }
            } catch (readError) {
              if (readError.name === 'AbortError') {
                console.log('🛑 连接被中止');
                break;
              }
              throw readError;
            }
          }

          if (!isComplete && hasReceivedData) {
            console.log('🔚 流结束，自动完成');
            complete();
          }

        } catch (error) {
          console.error('💥 SSE处理错误:', error);
          if (error.name !== 'AbortError') {
            handleError(error);
          }
        }
      };

      processSSE();
    });
  };

  return { sendSSEMessage };
}