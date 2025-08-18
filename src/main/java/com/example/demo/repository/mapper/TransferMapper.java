package com.example.demo.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.model.entity.Transfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TransferMapper extends BaseMapper<Transfer> {

    @Select("SELECT * FROM transfers WHERE transfer_id = #{transferId} FOR UPDATE")
    Transfer findByIdForUpdate(String transferId);
}
