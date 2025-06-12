// NotificationMessage.java
package com.aiproject.smartcampus.pojo.bo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationMessage {
    private Long id;
    private String content;
    private String type;
    private LocalDateTime createTime;
    private Integer receiverId;
    private String messageType;
}