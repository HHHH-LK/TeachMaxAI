package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.pojo.dto.CommentDTO;
import com.aiproject.smartcampus.pojo.dto.PostDTO;
import com.aiproject.smartcampus.pojo.dto.PostGetDTO;
import com.aiproject.smartcampus.pojo.po.Comment;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import com.aiproject.smartcampus.commons.client.Result;

public interface CommunityService {
    Result createPost(@Valid PostDTO postDTO);

    Result<IPage<PostGetDTO>> getPostsByPage(Integer pageNum, Integer pageSize);;

    Result<IPage<CommentDTO>> getCommentsByPostId(Integer postId, Integer pageNum, Integer pageSize);

    Result<PostGetDTO> getPostWithComments(Integer postId);

    Result<Comment> addCommentToPost( @Valid CommentDTO commentDTO);



    // 帖子点赞
    Result likePost(Integer postId);

    Result likeComment(Integer commentId);

    Result<IPage<PostGetDTO>> getOwnPost(int pageNum, int pageSize, Integer userId);
}