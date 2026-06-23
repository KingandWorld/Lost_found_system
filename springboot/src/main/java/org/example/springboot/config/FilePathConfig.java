package org.example.springboot.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储路径配置
 *
 * 统一管理图片/文件在磁盘上的存储目录，
 * 启动时将相对路径转为绝对路径，避免工作目录变化导致文件存到不同位置。
 * 默认值 ./files 相对于当前工作目录。
 */
@Component
public class FilePathConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePathConfig.class);

    public static String basePath;

    @Value("${file.base-path:./files}")
    public void setBasePath(String path) {
        // 将路径转为绝对路径并归一化，确保不随工作目录变化
        Path absolutePath = Paths.get(path).toAbsolutePath().normalize();
        basePath = absolutePath.toString();
        LOGGER.info("文件存储根目录已配置: {}", basePath);
    }
}
