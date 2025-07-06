package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.dto.*;
import com.aiproject.smartcampus.service.CommunityService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communityService;
    private final CommunityService postService;

    //创建帖子
    @PostMapping
    public Result createPost(@Valid @RequestBody PostDTO postDTO) {
        return postService.createPost(postDTO);
    }

    //获取帖子评论
    @GetMapping("/comments/{postId}")
    public Result<IPage<CommentDTO>> getCommentsByPostId(
            @PathVariable("postId") Integer postId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return postService.getCommentsByPostId(postId, pageNum, pageSize);
    }

    //获取所有帖子
    @GetMapping("/all")
    public Result<IPage<PostGetDTO>> getPostsByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return communityService.getPostsByPage(pageNum, pageSize);
    }

    //获取帖子
    @GetMapping("/{postId}")
    public Result<PostGetDTO> getPostWithComments(@PathVariable Integer postId) {
        return postService.getPostWithComments(postId);
    }

    //添加评论到帖子
    @PostMapping("/add/comments")
    public Result addCommentToPost(@Valid @RequestBody CommentDTO commentDTO) {
        return postService.addCommentToPost( commentDTO);
    }

    // 帖子点赞
    @PostMapping("/{postId}/like")
    public Result likePost(@PathVariable Integer postId) {
        return postService.likePost(postId);
    }

    // 评论点赞
    @PostMapping("/comments/{commentId}/like")
    public Result likeComment(@PathVariable Integer commentId) {
        return postService.likeComment(commentId);
    }
}