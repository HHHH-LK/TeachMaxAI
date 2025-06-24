package com.aiproject.smartcampus.pojo.bo;

import dev.langchain4j.data.message.ChatMessage;
import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: lecture-langchain-20250525
 * @description: 本地缓存实体
 * @author: lk
 * @create: 2025-05-11 10:21
 **/

@Data
public  class CacheEntry {

        private static final Duration CACHE_TTL = Duration.ofHours(5);

        private final List<ChatMessage> messages;
        //持久时间
        public final long timestamp;
        /**
         * 标记脏数据
         * */
        private volatile boolean dirty;
        
        public CacheEntry(List<ChatMessage> messages) {
            this.messages = new ArrayList<>(messages);
            this.timestamp = System.currentTimeMillis();
            this.dirty = false;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL.toMillis();
        }
        
        public List<ChatMessage> getMessages() {
            return new ArrayList<>(messages);
        }
        
        public void setMessages(List<ChatMessage> messages) {
            this.messages.clear();
            this.messages.addAll(messages);
            this.dirty = true;
        }

        public boolean isDirty() {
            return dirty;
        }

        public void markClean() {
            this.dirty = false;
        }
    }