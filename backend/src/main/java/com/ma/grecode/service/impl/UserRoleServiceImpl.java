package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.UserRole;
import com.ma.grecode.service.UserRoleService;
import com.ma.grecode.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

/**
* @author 19135
* @description 针对表【user_role(用户角色关联表)】的数据库操作Service实现
* @createDate 2025-12-08 16:35:49
*/
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>
    implements UserRoleService{

}




