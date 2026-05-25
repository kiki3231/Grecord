package com.ma.grecode.mapper;

import com.ma.grecode.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 19135
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2025-12-08 16:35:48
* @Entity grecorde.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User selectUserByUsername(@Param("username") String username);

}

