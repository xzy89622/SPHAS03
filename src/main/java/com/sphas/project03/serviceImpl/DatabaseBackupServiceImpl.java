package com.sphas.project03.serviceImpl;

import com.sphas.project03.common.BizException;
import com.sphas.project03.service.DatabaseBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths; // ✅ Java8 用 Paths.get
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * mysqldump 备份实现（Java8兼容）
 */
@Service
public class DatabaseBackupServiceImpl implements DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupServiceImpl.class);

    // mysqldump 命令（Windows 没加环境变量就写绝对路径）
    @Value("${app.backup.mysqldump:mysqldump}")
    private String mysqldump;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPass;

    @Value("${app.backup.dir:backups}")
    private String backupDir;

    @Override
    public String runBackup() {
        try {
            DbInfo info = parseJdbc(jdbcUrl);

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String name = "project03_" + ts + ".sql";

            // ✅ Java8：用 Paths.get
            Path dir = Paths.get(backupDir);
            Files.createDirectories(dir);

            Path out = dir.resolve(name);

            // -p 后面不加空格，避免交互
            List<String> cmd = new ArrayList<>();
            cmd.add(mysqldump);
            cmd.add("-h");
            cmd.add(info.host);
            cmd.add("-P");
            cmd.add(String.valueOf(info.port));
            cmd.add("-u");
            cmd.add(dbUser);
            cmd.add("-p" + dbPass);
            cmd.add("--single-transaction");
            cmd.add("--routines");
            cmd.add("--events");
            cmd.add(info.db);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectOutput(out.toFile());

            Process p = pb.start();
            int code = p.waitFor();

            if (code != 0) {
                // ✅ Java8：没有 Files.readString
                String msg = "";
                try {
                    msg = new String(Files.readAllBytes(out), Charset.forName("UTF-8"));
                } catch (Exception ignore) {
                }
                log.warn("mysqldump exit={}, out={}", code, msg);
                throw new BizException("备份失败：mysqldump 执行异常（exit=" + code + "）\n" + msg);
            }

            return name;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("备份失败：" + e.getMessage());
        }
    }

    @Override
    public List<String> listBackups() {
        try {
            Path dir = Paths.get(backupDir);
            if (!Files.exists(dir)) return new ArrayList<>();

            List<String> names = new ArrayList<>();
            Files.list(dir)
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparingLong(p -> -p.toFile().lastModified()))
                    .forEach(p -> names.add(p.getFileName().toString()));

            return names;
        } catch (Exception e) {
            throw new BizException("读取备份列表失败：" + e.getMessage());
        }
    }

    @Override
    public File getBackupFile(String name) {
        // 防穿越：只允许文件名，不允许路径
        if (name == null || name.contains("/") || name.contains("\\")) {
            throw new BizException("文件名不合法");
        }

        File f = Paths.get(backupDir).resolve(name).toFile();
        if (!f.exists() || !f.isFile()) throw new BizException("备份文件不存在");
        return f;
    }

    /**
     * 解析 jdbc:mysql://host:port/db?... （简单够用）
     */
    private DbInfo parseJdbc(String url) {
        String s = url;
        if (s.startsWith("jdbc:mysql://")) s = s.substring("jdbc:mysql://".length());

        String hostPortDb = s.split("\\?")[0];
        String[] hpDb = hostPortDb.split("/");

        String hp = hpDb[0];
        String db = (hpDb.length > 1) ? hpDb[1] : "";

        String host = hp;
        int port = 3306;

        if (hp.contains(":")) {
            String[] arr = hp.split(":");
            host = arr[0];
            port = Integer.parseInt(arr[1]);
        }

        if (db == null || db.trim().isEmpty()) throw new BizException("jdbcUrl 解析失败：" + url);

        DbInfo info = new DbInfo();
        info.host = host;
        info.port = port;
        info.db = db;
        return info;
    }

    private static class DbInfo {
        String host;
        int port;
        String db;
    }
}