package com.aiproject.smartcampus.mapper;

import com.aiproject.smartcampus.pojo.po.BattleLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FightingMapper extends BaseMapper<BattleLog> {
}
