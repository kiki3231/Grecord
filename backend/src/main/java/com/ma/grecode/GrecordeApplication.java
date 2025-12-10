package com.ma.grecode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan({"com.ma.grecode.mapper"})  // 扫描所有mapper
@ComponentScan({"com.ma.*"})
public class GrecordeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrecordeApplication.class, args);
    }

}
