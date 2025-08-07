package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * @author lk_hhh
 */ // 1. 用户Mapper
@Repository
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据用户类型查询用户列表
     */
    @Select("SELECT * FROM users WHERE user_type = #{userType} AND status = 'active'")
    List<User> findByUserType(@Param("userType") String userType);


    @Select("SELECT * FROM users WHERE phone = #{phone} AND status = 'active'")
    User findByUserPhone(@Param("phone") String phone);



    @Select("SELECT * FROM users WHERE email = #{email} AND status = 'active'")
    User findByUserEmail(@Param("email") String email);
    /**
     * 模糊查询用户
     */
    @Select("SELECT * FROM users WHERE real_name LIKE CONCAT('%', #{keyword}, '%') OR username LIKE CONCAT('%', #{keyword}, '%')")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Select("SELECT * FROM users WHERE user_id = #{userId}")
    User findById(@Param("userId") Integer userId);
}
