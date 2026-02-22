package com.sphas.project03.service;

import java.io.File;
import java.util.List;

/**
 * 数据库备份服务
 * 说明：毕设要求里提到“数据库备份”，这里做一个最实用的版本：
 *  - 管理员一键触发 mysqldump
 *  - 备份文件落到本地 backups 目录
 */
public interface DatabaseBackupService {

    /** 立即备份，返回备份文件名 */
    String runBackup();

    /** 列出备份文件（按时间倒序） */
    List<String> listBackups();

    /** 根据文件名取备份文件 */
    File getBackupFile(String name);
}