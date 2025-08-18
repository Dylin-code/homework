package com.example.demo.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.model.entity.Transfer;
import com.example.demo.repository.mapper.TransferMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TransferRepo extends ServiceImpl<TransferMapper, Transfer> {

    public Optional<Transfer> findByIdForUpdate(String transferId){
        return Optional.ofNullable(baseMapper.findByIdForUpdate(transferId));
    }
}
