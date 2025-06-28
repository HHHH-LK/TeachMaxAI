package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.LocalMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AIMapper extends BaseMapper<LocalMessage> {

    /**
     * 根据用户ID查询记忆内容
     */
    @Select("SELECT * FROM memory_store WHERE id = #{userId}")
    LocalMessage findByUserId(@Param("userId") Integer userId);

    /**
     * 更新记忆内容
     */
    @Update("UPDATE memory_store SET content = #{content}, create_time = #{createTime} WHERE id = #{id}")
    int updateMemory(@Param("id") Integer id, @Param("content") String content, @Param("createTime") String createTime);

}
