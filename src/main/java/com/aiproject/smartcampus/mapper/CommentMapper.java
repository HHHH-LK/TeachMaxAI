package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.CommentDTO;
import com.aiproject.smartcampus.pojo.po.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {
    /**
     * 查询帖子下的评论（带用户信息）
     */
//    @Select("SELECT c.*, u.username, u.avatar as user_avatar " +
//            "FROM comment c " +
//            "LEFT JOIN user u ON c.user_id = u.id " +
//            "WHERE c.post_id = #{postId} " +
//            "ORDER BY c.create_time ASC")
//    @Results({
//            @Result(property = "commentId", column = "comment_id"),
//            @Result(property = "postId", column = "post_id"),
//            @Result(property = "content", column = "content"),
//            @Result(property = "userId", column = "user_id"),
//            @Result(property = "parentId", column = "parent_id"),
//            @Result(property = "likeCount", column = "like_count"),
//            @Result(property = "createTime", column = "create_time"),
//            @Result(property = "userName", column = "username"),
//            @Result(property = "userAvatar", column = "user_avatar")
//    })
//    List<Comment> findCommentsByPostIdWithUser(@Param("postId") Integer postId);

    /**
     * 查询评论树形结构（递归）
     */
    @Select("WITH RECURSIVE comment_tree AS (" +
            "  SELECT c.*, u.username, u.avatar as user_avatar " +
            "  FROM comment c " +
            "  LEFT JOIN user u ON c.user_id = u.id " +
            "  WHERE c.post_id = #{postId} AND c.parent_id IS NULL " +
            "  UNION ALL " +
            "  SELECT c.*, u.username, u.avatar as user_avatar " +
            "  FROM comment c " +
            "  INNER JOIN comment_tree ct ON c.parent_id = ct.comment_id " +
            "  LEFT JOIN user u ON c.user_id = u.id" +
            ") " +
            "SELECT * FROM comment_tree ORDER BY create_time")
    @Results({
            @Result(property = "commentId", column = "comment_id"),
            @Result(property = "postId", column = "post_id"),
            @Result(property = "content", column = "content"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "userName", column = "username"),
            @Result(property = "userAvatar", column = "user_avatar")
    })
    List<Comment> findCommentTreeByPostId(@Param("postId") Integer postId);

    /**
     * 更新评论点赞数
     */
    @Update("UPDATE comment SET like_count = like_count + #{increment} WHERE comment_id = #{commentId}")
    int updateLikeCount(@Param("commentId") Integer commentId, @Param("increment") Integer increment);

    @Select("SELECT c.*, u.username, u.avatar as user_avatar " +
            "FROM comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.comment_id = #{commentId}")
    @Results({
            @Result(property = "commentId", column = "comment_id"),
            @Result(property = "postId", column = "post_id"),
            @Result(property = "content", column = "content"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "userName", column = "username"),
            @Result(property = "userAvatar", column = "user_avatar")
    })
    CommentDTO getCommentById(@Param("commentId") Integer commentId);


    @Update("UPDATE post SET like_count = like_count + 1 WHERE comment_id = #{commentId}")
    void incrementLikeCount(Long commentId);
}
