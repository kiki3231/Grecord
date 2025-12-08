package com.ma.grecorde.mapper;

import com.ma.grecorde.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 19135
* @description 针对表【user_role(用户角色关联表)】的数据库操作Mapper
* @createDate 2025-12-08 16:35:49
* @Entity grecorde.entity.UserRole
*/
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

}




