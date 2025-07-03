package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.dto.PostDTO;
import com.aiproject.smartcampus.pojo.po.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author lk_hhh
 */
@Repository
@Mapper
public interface PostMapper extends BaseMapper<Post> {
    /**
     * 查询帖子列表（带用户信息）
     */
    @Select("SELECT p.*, u.username, u.avatar as user_avatar " +
            "FROM post p " +
            "LEFT JOIN user u ON p.user_id = u.id " +
            "ORDER BY p.create_time DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "content", column = "content"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "viewCount", column = "view_count"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "commentCount", column = "comment_count"),
            @Result(property = "category", column = "category"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "userName", column = "username"),
            @Result(property = "userAvatar", column = "user_avatar")
    })
    List<PostDTO> findPostListWithUser();

    @Insert("INSERT INTO post (title, content, user_id, category) " +
            "VALUES (#{title}, #{content}, #{userId}, #{category})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertPost(Post post);

    /**
     * 更新帖子浏览量
     */
    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{postId}")
    int incrementViewCount(@Param("postId") Integer postId);

    /**
     * 查询帖子详情（带用户信息） - 使用PostDTO
     */
    @Select("SELECT p.*, u.username, u.avatar as user_avatar " +
            "FROM post p " +
            "LEFT JOIN user u ON p.user_id = u.id " +
            "WHERE p.id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "title", column = "title"),
            @Result(property = "content", column = "content"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "viewCount", column = "view_count"),
            @Result(property = "likeCount", column = "like_count"),
            @Result(property = "commentCount", column = "comment_count"),
            @Result(property = "category", column = "category"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time"),
            @Result(property = "userName", column = "username"),
            @Result(property = "userAvatar", column = "user_avatar")
    })
    PostDTO getPostById(@Param("id") Integer id);

    @Update("UPDATE post SET like_count = like_count + 1 WHERE id = #{postId}")
    void incrementLikeCount(Long postId);
}
