package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.mapper.SysUserMapper;
import com.sphas.project03.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * 用户Service实现
 * 说明：就是把 SysUserMapper 包一层，给 Controller/Task 用（MyBatis-Plus标准写法）
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    // 暂时不写业务逻辑，够用就行
}
