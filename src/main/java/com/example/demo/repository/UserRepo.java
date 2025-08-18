package com.example.demo.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.model.entity.User;
import com.example.demo.repository.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepo extends ServiceImpl<UserMapper, User> {

    public Optional<User> findByIdForUpdate(String userId){
        return Optional.ofNullable(baseMapper.findByIdForUpdate(userId));
    }
}
