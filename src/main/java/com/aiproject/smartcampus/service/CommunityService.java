package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.pojo.dto.CommentDTO;
import com.aiproject.smartcampus.pojo.dto.PostDTO;
import com.aiproject.smartcampus.pojo.po.Comment;
import jakarta.validation.Valid;

import java.util.List;

public interface CommunityService {
    PostDTO createPost(@Valid PostDTO postDTO);

    List<PostDTO> gerAllPosts();

    List<Comment> getCommentsByPostId(Integer postId);

    PostDTO getPostWithComments(Integer postId);

    CommentDTO addCommentToPost(Integer postId, @Valid CommentDTO commentDTO);

    // 帖子点赞
    void likePost(Long postId);

    // 评论点赞
    void likeComment(Long commentId);
}
