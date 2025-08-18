package com.example.demo.config;

import com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ApplicationRunner {

    @Bean
    public DdlApplicationRunner ddlApplicationRunner(@Autowired(required = false)List ddlList) {
        return new DdlApplicationRunner(ddlList);
    }
}
