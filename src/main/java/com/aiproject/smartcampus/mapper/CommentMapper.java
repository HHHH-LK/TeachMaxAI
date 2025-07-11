package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.CommentDTO;
import com.aiproject.smartcampus.pojo.po.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT c.*, u.username " +
            "FROM comment c " +
            "LEFT JOIN users u ON c.user_id = u.user_id " +
            "WHERE c.post_id = #{postId} " +
            "ORDER BY c.create_time")
    @Results({
            @Result(property = "commentId", column = "comment_id", id = true),
            @Result(property = "postId", column = "post_id"),
            @Result(property = "content", column = "content"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "userName", column = "username")
    })
    IPage<CommentDTO> findCommentsByPostId(IPage<CommentDTO> page, @Param("postId") Integer postId);
    /**
     * 更新评论点赞数
     */

    @Update("UPDATE comment SET like_count = like_count + 1 WHERE comment_id = #{commentId}")
    void incrementLikeCount(Integer commentId);

    @Select("SELECT like_count FROM comment WHERE comment_id = #{commentId}")
    Integer getLikeCountById(@Param("commentId") Integer commentId);

}