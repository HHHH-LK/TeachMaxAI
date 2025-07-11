package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.mapper.CommentMapper;
import com.aiproject.smartcampus.mapper.PostMapper;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.pojo.po.Comment;
import com.aiproject.smartcampus.pojo.po.Post;
import com.aiproject.smartcampus.service.CommunityService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aiproject.smartcampus.commons.client.Result;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    //新增帖子
    @Override
    public Result createPost(PostDTO postDTO) {

        Post post = new Post();
//        post.setCategory(postDTO.getCategory());
        post.setContent(postDTO.getContent());
        post.setUserId(postDTO.getUserId());
        post.setTitle(postDTO.getTitle());

        // 插入数据库
        postMapper.insertPost(post);
        postDTO.setId(post.getId());

        // 返回包含贴子ID
        return Result.success(postDTO.getId());
    }

    //获取所有
    @Override
    public Result<IPage<PostGetDTO>> getPostsByPage(Integer pageNum, Integer pageSize) {
        // 创建分页对象 (页号从1开始)
        Page<PostGetDTO> page = new Page<>(pageNum, pageSize);

        // 执行分页查询
        IPage<PostGetDTO> resultPage = postMapper.findPostPageWithUser(page);

        return Result.success(resultPage);
    }

    //获取某个用户
    @Override
    public Result<IPage<PostGetDTO>> getOwnPost(int pageNum, int pageSize, Integer userId) {
        Page<PostGetDTO> page = new Page<>(pageNum, pageSize);

        // 执行分页查询
        IPage<PostGetDTO> resultPage = postMapper.findPostPageByUser(page, userId);
        return Result.success(resultPage);

    }


    //获取帖子评论
    @Override
    public Result<IPage<CommentDTO>> getCommentsByPostId(Integer postId, Integer pageNum, Integer pageSize) {
        Page<CommentDTO> page = new Page<>(pageNum, pageSize);
        IPage<CommentDTO> commentsPage = commentMapper.findCommentsByPostId(page, postId);
        return Result.success(commentsPage);
    }

    //获取单个帖子
    @Override
    public Result<PostGetDTO> getPostWithComments(Integer postId) {
        PostGetDTO postGetDTO = postMapper.getPostById(postId);
        if (postGetDTO == null) {
            throw new IllegalArgumentException("Post not found");
        }
        postMapper.incrementViewCount(postId);
        postGetDTO.setViewCount(postGetDTO.getViewCount()+1);

        Integer view = postMapper.getViewCountById(postId);
        postGetDTO.setViewCount(view);
        return Result.success(postGetDTO);
    }

    //添加评论
    @Override
    @Transactional
    public Result addCommentToPost(CommentDTO commentDTO) {
//        if (postMapper.getPostById(commentDTO.getPostId()) == null) {
//            throw new IllegalArgumentException("Post not found");
//        }
        Comment comment = new Comment();
        comment.setPostId(commentDTO.getPostId());
        comment.setContent(commentDTO.getContent());
        comment.setUserId(commentDTO.getUserId());
        if (commentDTO.getParentId() == null) {
            comment.setParentId(null); // 如果没有父评论
        } else {
            comment.setParentId(commentDTO.getParentId());
        }

        commentMapper.insert(comment);
        postMapper.incrementViewCount(commentDTO.getPostId());

        return Result.success("Comment added successfully");
    }

    //点赞帖子
    @Override
    public Result likePost(Integer postId) {
        postMapper.incrementLikeCount(postId);
        Integer postCount = postMapper.getLikeCountById(postId);
        if ( postCount == null) {
            return Result.error("Post not found or already liked");
        }
        return Result.success("Post liked successfully:" + postCount);
    }

    //点赞评论
    @Override
    public Result likeComment(Integer commentId) {
        commentMapper.incrementLikeCount(commentId);
        Integer commentCount = commentMapper.getLikeCountById(commentId);
        if (commentCount == null) {
            return Result.error("Comment not found or already liked");
        }
        return Result.success("Comment liked successfully:" + commentCount);
    }



}
