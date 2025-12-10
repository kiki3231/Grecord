package com.ma.grecode.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ma.grecode.entity.Permission;
import com.ma.grecode.service.PermissionService;
import com.ma.grecode.mapper.PermissionMapper;
import org.springframework.stereotype.Service;

/**
* @author 19135
* @description 针对表【permission(权限表)】的数据库操作Service实现
* @createDate 2025-12-08 16:35:49
*/
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
    implements PermissionService{

}




