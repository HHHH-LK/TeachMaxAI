package com.aiproject.smartcampus.pojo.dto;

import lombok.Data;

@Data
public class PostGetDTO {
    private Integer id; //帖子id
    private String title; //帖子标题
    private String content; //帖子内容
    private Integer userId;  //发布人id
    private String userName; //发布人姓名
    private Integer viewCount; //浏览量
    private Integer likeCount; //点赞量
    private Integer commentCount; //评论量
    private String category; //帖子分类
    private String createTime; //创建时间
    private String updateTime; //更新时间
}
