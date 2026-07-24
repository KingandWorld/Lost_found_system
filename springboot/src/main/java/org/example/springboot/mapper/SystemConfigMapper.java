package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.SystemConfig;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
}
