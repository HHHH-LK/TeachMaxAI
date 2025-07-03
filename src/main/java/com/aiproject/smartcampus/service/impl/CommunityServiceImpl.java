package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.mapper.CommentMapper;
import com.aiproject.smartcampus.mapper.PostMapper;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.pojo.po.Comment;
import com.aiproject.smartcampus.pojo.po.Post;
import com.aiproject.smartcampus.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    //新增帖子
    @Override
    public PostDTO createPost(PostDTO postDTO) {

        Post post = new Post();
        post.setCategory(postDTO.getCategory());
        post.setContent(postDTO.getContent());
        post.setUserId(postDTO.getUserId());
        post.setTitle(postDTO.getTitle());

        // 插入数据库
        postMapper.insertPost(post);

        // 返回包含ID的DTO
        return postMapper.getPostById(postDTO.getId());
    }

    //获取所有
    @Override
    public List<PostDTO> gerAllPosts() {
        return postMapper.findPostListWithUser();
    }

    @Override
    public List<Comment> getCommentsByPostId(Integer postId) {
        return commentMapper.findCommentTreeByPostId(postId);
    }

    //获取单个帖子
    @Override
    public PostDTO getPostWithComments(Integer postId) {
        PostDTO postDTO = postMapper.getPostById(postId);
        postMapper.incrementViewCount(postId);
        postDTO.setViewCount(postDTO.getViewCount()+1);

        return postDTO;
    }

    //添加评论
    @Override
    @Transactional
    public CommentDTO addCommentToPost(Integer postId, CommentDTO commentDTO) {
        if (postMapper.getPostById(postId) == null) {
            throw new IllegalArgumentException("Post not found");
        }
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setContent(commentDTO.getContent());
        comment.setUserId(commentDTO.getUserId());
        comment.setParentId(commentDTO.getParentId());

        commentMapper.insert(comment);
        postMapper.incrementViewCount(postId);

        return commentMapper.getCommentById(commentDTO.getCommentId());
    }

    @Override
    public void likePost(Long postId) {
        postMapper.incrementLikeCount(postId);
    }

    @Override
    public void likeComment(Long commentId) {
        commentMapper.incrementLikeCount(commentId);
    }

}
