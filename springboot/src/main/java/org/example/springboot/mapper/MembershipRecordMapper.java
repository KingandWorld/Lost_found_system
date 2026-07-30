package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.MembershipRecord;

/**
 * 会员记录 Mapper
 */
@Mapper
public interface MembershipRecordMapper extends BaseMapper<MembershipRecord> {
}
