package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Integer commentId; // 评论ID
    private Integer postId; // 帖子ID
    private String content; // 评论内容
    private Integer userId; // 用户ID
    private String userName; // 用户名
    private Integer parentId; // 父评论Id
    private Integer likeCount; // 点赞数
    private LocalDateTime createTime; // 评论创建时间
}
