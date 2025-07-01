package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Admin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @program: SmartCampus
 * @description: 管理员Mapper
 * @author: lk
 * @create: 2025-06-28
 **/
@Repository
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    
    /**
     * 根据管理员工号查询管理员
     */
    @Select("SELECT a.*, u.* FROM admins a LEFT JOIN users u ON a.user_id = u.user_id WHERE a.admin_number = #{adminNumber}")
    @Results({
        @Result(property = "adminId", column = "admin_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "adminNumber", column = "admin_number"),
        @Result(property = "department", column = "department"),
        @Result(property = "position", column = "position"),
        @Result(property = "adminLevel", column = "admin_level"),
        @Result(property = "permissions", column = "permissions"),
        @Result(property = "lastLoginTime", column = "last_login_time"),
        @Result(property = "lastLoginIp", column = "last_login_ip"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "user.userId", column = "user_id"),
        @Result(property = "user.username", column = "username"),
        @Result(property = "user.realName", column = "real_name"),
        @Result(property = "user.email", column = "email"),
        @Result(property = "user.phone", column = "phone"),
        @Result(property = "user.userType", column = "user_type"),
        @Result(property = "user.status", column = "status")
    })
    Admin findByAdminNumber(@Param("adminNumber") String adminNumber);
    
    /**
     * 根据用户ID查询管理员
     */
    @Select("SELECT a.*, u.real_name, u.username, u.email, u.phone FROM admins a " +
            "LEFT JOIN users u ON a.user_id = u.user_id WHERE a.user_id = #{userId}")
    Admin findByUserId(@Param("userId") Integer userId);
    
    /**
     * 根据部门查询管理员列表
     */
    @Select("SELECT a.*, u.real_name, u.username FROM admins a " +
            "LEFT JOIN users u ON a.user_id = u.user_id " +
            "WHERE a.department = #{department} AND a.status = 'active'")
    List<Admin> findByDepartment(@Param("department") String department);
    
    /**
     * 根据管理员等级查询管理员列表
     */
    @Select("SELECT a.*, u.real_name, u.username FROM admins a " +
            "LEFT JOIN users u ON a.user_id = u.user_id " +
            "WHERE a.admin_level = #{adminLevel} AND a.status = 'active'")
    List<Admin> findByAdminLevel(@Param("adminLevel") String adminLevel);
    
    /**
     * 查询所有活跃的管理员
     */
    @Select("SELECT a.*, u.real_name, u.username, u.email FROM admins a " +
            "LEFT JOIN users u ON a.user_id = u.user_id " +
            "WHERE a.status = 'active' ORDER BY a.admin_level DESC, a.created_at ASC")
    List<Admin> findActiveAdmins();
    
    /**
     * 更新最后登录信息
     */
    @Update("UPDATE admins SET last_login_time = #{loginTime}, last_login_ip = #{loginIp} WHERE admin_id = #{adminId}")
    int updateLastLoginInfo(@Param("adminId") Integer adminId, 
                           @Param("loginTime") LocalDateTime loginTime, 
                           @Param("loginIp") String loginIp);
    
    /**
     * 更新管理员状态
     */
    @Update("UPDATE admins SET status = #{status} WHERE admin_id = #{adminId}")
    int updateStatus(@Param("adminId") Integer adminId, @Param("status") String status);
    
    /**
     * 更新管理员权限
     */
    @Update("UPDATE admins SET permissions = #{permissions} WHERE admin_id = #{adminId}")
    int updatePermissions(@Param("adminId") Integer adminId, @Param("permissions") String permissions);
    
    /**
     * 检查管理员工号是否存在
     */
    @Select("SELECT COUNT(*) FROM admins WHERE admin_number = #{adminNumber}")
    int checkAdminNumberExists(@Param("adminNumber") String adminNumber);
    
    /**
     * 检查用户是否已是管理员
     */
    @Select("SELECT COUNT(*) FROM admins WHERE user_id = #{userId}")
    int checkUserIsAdmin(@Param("userId") Integer userId);
    
    /**
     * 管理员统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "COUNT(CASE WHEN status = 'active' THEN 1 END) as active_count, " +
            "COUNT(CASE WHEN admin_level = 'super_admin' THEN 1 END) as super_admin_count, " +
            "COUNT(CASE WHEN admin_level = 'admin' THEN 1 END) as admin_count, " +
            "COUNT(CASE WHEN admin_level = 'operator' THEN 1 END) as operator_count " +
            "FROM admins")
    Map<String, Object> getAdminStatistics();
    
    /**
     * 根据部门统计管理员数量
     */
    @Select("SELECT department, COUNT(*) as count FROM admins " +
            "WHERE status = 'active' AND department IS NOT NULL " +
            "GROUP BY department ORDER BY count DESC")
    List<Map<String, Object>> getAdminCountByDepartment();
    
    /**
     * 查询最近登录的管理员
     */
    @Select("SELECT a.*, u.real_name FROM admins a " +
            "LEFT JOIN users u ON a.user_id = u.user_id " +
            "WHERE a.last_login_time IS NOT NULL AND a.status = 'active' " +
            "ORDER BY a.last_login_time DESC LIMIT #{limit}")
    List<Admin> findRecentLoginAdmins(@Param("limit") Integer limit);
    
    /**
     * 搜索管理员（根据姓名、工号、部门）
     */
    @Select("SELECT a.*, u.real_name, u.username FROM admins a " +
            "LEFT JOIN users u ON a.user_id = u.user_id " +
            "WHERE (u.real_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.admin_number LIKE CONCAT('%', #{keyword}, '%') " +
            "OR a.department LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND a.status = 'active'")
    List<Admin> searchAdmins(@Param("keyword") String keyword);
    
    /**
     * 根据权限代码查询拥有该权限的管理员
     */
    @Select("SELECT a.*, u.real_name FROM admins a " +
            "LEFT JOIN users u ON a.user_id = u.user_id " +
            "WHERE JSON_CONTAINS(a.permissions, JSON_QUOTE(#{permissionCode})) " +
            "AND a.status = 'active'")
    List<Admin> findAdminsByPermission(@Param("permissionCode") String permissionCode);
    
    /**
     * 批量更新管理员状态
     */
    @Update("<script>" +
            "UPDATE admins SET status = #{status} WHERE admin_id IN " +
            "<foreach collection='adminIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateStatus(@Param("adminIds") List<Integer> adminIds, @Param("status") String status);


}