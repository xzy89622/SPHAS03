package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.service.DatabaseBackupService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * 运维接口（管理员）
 * 说明：满足“数据库备份、系统维护”这类毕设要求。
 */
@RestController
@RequestMapping("/api/admin/ops")
public class AdminOpsController extends BaseController {

    private final DatabaseBackupService databaseBackupService;

    public AdminOpsController(DatabaseBackupService databaseBackupService) {
        this.databaseBackupService = databaseBackupService;
    }

    /** 触发一次备份 */
    @PostMapping("/backup/run")
    public R<String> runBackup(HttpServletRequest request) {
        requireAdmin(request);
        String name = databaseBackupService.runBackup();
        return R.ok(name);
    }

    /** 备份列表 */
    @GetMapping("/backup/list")
    public R<List<String>> list(HttpServletRequest request) {
        requireAdmin(request);
        return R.ok(databaseBackupService.listBackups());
    }

    /** 下载备份文件 */
    @GetMapping("/backup/download/{name}")
    public void download(@PathVariable String name, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        File f = databaseBackupService.getBackupFile(name);

        response.setContentType("application/sql");
        response.setHeader("Content-Disposition", "attachment; filename=" + name);
        Files.copy(f.toPath(), response.getOutputStream());
    }
}