package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.PointsLog;

/**
 * 积分变动日志 Mapper
 */
@Mapper
public interface PointsLogMapper extends BaseMapper<PointsLog> {
}
