package com.sphas.project03.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sphas.project03.entity.SysUser;

/**
 * 用户Service
 * 说明：目前项目里有 SysUserMapper，但缺少 Service，导致定时任务/社交模块编译不过
 */
public interface SysUserService extends IService<SysUser> {
    // 先不加额外方法，IService 已经够用（getById/list/page/update等）
}
