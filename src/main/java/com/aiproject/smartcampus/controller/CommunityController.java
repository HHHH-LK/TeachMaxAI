package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.pojo.dto.CommentDTO;
import com.aiproject.smartcampus.pojo.dto.PostDTO;
import com.aiproject.smartcampus.pojo.po.Comment;
import com.aiproject.smartcampus.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService postService;

    //创建帖子
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody PostDTO postDTO) {
        return ResponseEntity.ok(postService.createPost(postDTO));
    }

    //获取所有帖子
    @GetMapping("/posts")
    public List<PostDTO> getAllPosts() {
        return postService.gerAllPosts();
    }

    //获取帖子评论
    @GetMapping("/comments/{postId}")
    List<Comment> getCommentsByPostId(Integer postId){
        return postService.getCommentsByPostId(postId);
    }

    //获取帖子详情
    @GetMapping("/posts/{postId}")
    public PostDTO getPostWithComments(@PathVariable Integer postId) {
        return postService.getPostWithComments(postId);
    }

    //
    @PostMapping("/posts/{postId}/comments")
    public CommentDTO addCommentToPost(
            @PathVariable Integer postId, @Valid @RequestBody CommentDTO commentDTO) {
        return postService.addCommentToPost(postId, commentDTO);
    }

}
