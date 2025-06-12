package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.Notificate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificateMapper extends BaseMapper<Notificate> {

    /**
     * 基于id查询通知信息
     * */
    @Select("select * from notification where id=#{l}")
    Notificate selectForUpdate(long l);
}
