package com.aiproject.smartcampus.mapper;

//import cn.hutool.db.Page;
import com.aiproject.smartcampus.pojo.dto.PostGetDTO;
import com.aiproject.smartcampus.pojo.po.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

/**
 * @author lk_hhh
 */
@Repository
@Mapper
public interface PostMapper extends BaseMapper<Post> {
    /**
     * 查询帖子列表（带用户信息）
     */
    @Select("SELECT p.*, u.username " +
            "FROM post p " +
            "LEFT JOIN users u ON p.user_id = u.user_id " +
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
            @Result(property = "userName", column = "username")
    })
    IPage<PostGetDTO> findPostPageWithUser(Page<PostGetDTO> page);

    @Select("SELECT p.*, u.username " +
            "FROM post p " +
            "LEFT JOIN users u ON p.user_id = u.user_id " +
            "WHERE u.userid = #{id}" +
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
            @Result(property = "userName", column = "username")
    })
    IPage<PostGetDTO> findPostPageByUser(Page<PostGetDTO> page, @Param("id") Integer id);

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
    @Select("SELECT p.*, u.username " +
            "FROM post p " +
            "LEFT JOIN users u ON p.user_id = u.user_id " +
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
            @Result(property = "userName", column = "username")
    })
    PostGetDTO getPostById(@Param("id") Integer id);


    @Select("SELECT view_count FROM post WHERE id = #{postId}")
    @Result(property = "viewCount", column = "view_count")
    Integer getViewCountById(@Param("postId") Integer postId);


    @Select("SELECT like_count FROM post WHERE id = #{postId}")
    @Result(property = "likeCount", column = "like_count")
    Integer getLikeCountById(@Param("postId") Integer postId);

    /**
     * 更新帖子点赞量
     */
    @Update("UPDATE post SET like_count = like_count + 1 WHERE id = #{postId}")
    void incrementLikeCount(Integer postId);

    @Update("UPDATE post SET comment_count = comment_count + 1 WHERE id = #{postId}")
    void incrementCommentCount(Long postId);
}