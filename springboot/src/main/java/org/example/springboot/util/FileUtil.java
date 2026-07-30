package org.example.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {
    private final static  Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 缩略图最大宽度
     */
    private static final int THUMB_MAX_WIDTH = 300;

    /**
     * 缩略图最大高度
     */
    private static final int THUMB_MAX_HEIGHT = 300;

    /**
     * 缩略图前缀
     */
    private static final String THUMB_PREFIX = "thumb_";

    /**
     * 获取文件存储根目录（从 FilePathConfig 动态读取，支持运行时配置）
     */
    public static String getBasePath() {
        return org.example.springboot.config.FilePathConfig.basePath;
    }
    // 获取项目根目录路径
    public static Path getProjectRootPath() throws IOException {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:.");
        if (resources.length == 0) {
            throw new IOException("Cannot find project root path.");
        }
        // 通常第一个资源就是项目的根目录
        File rootDir = resources[0].getFile();
        return rootDir.toPath();
    }

    // 公共的文件保存方法
    public static String saveFile(MultipartFile file, String folderName, String baseDir) {
        return saveFile(file, folderName, baseDir, false);
    }

    /**
     * 公共的文件保存方法（支持生成缩略图）
     * @param file 上传的文件
     * @param folderName 子目录名
     * @param baseDir 基础目录
     * @param generateThumb 是否生成缩略图
     */
    public static String saveFile(MultipartFile file, String folderName, String baseDir, boolean generateThumb) {
        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        long timestamp = System.currentTimeMillis();
        String extension = ""; // 文件扩展名，默认为空

        // 获取文件扩展名
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        String dFileName = timestamp + extension;

        // 获取项目根目录路径
        Path projectRootPath = null;
        try {
            projectRootPath = Paths.get(getBasePath());
            Path fileDirectory = projectRootPath.resolve(baseDir);

            // 如果folderName不为null，则在指定目录后面加入folderName
            if (folderName != null && !folderName.isEmpty()) {
                fileDirectory = fileDirectory.resolve(folderName);
            }

            if (!Files.exists(fileDirectory)) {
                Files.createDirectories(fileDirectory); // 如果目录不存在，则创建目录
            }
            Path uploadFilePath = fileDirectory.resolve(dFileName);
            File uploadFile = uploadFilePath.toFile();

            file.transferTo(uploadFile);
            LOGGER.info("File saved at: {}", uploadFile.getAbsolutePath());

            // 生成缩略图（仅对图片文件）
            if (generateThumb && isImageFile(extension)) {
                String thumbFileName = THUMB_PREFIX + dFileName;
                Path thumbFilePath = fileDirectory.resolve(thumbFileName);
                generateThumbnail(uploadFile, thumbFilePath.toFile(), extension);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 返回相对路径，不再添加/api前缀
        String relativePath = "/" + baseDir + "/" + (folderName != null && !folderName.isEmpty() ? folderName + "/" : "") + dFileName;
        return relativePath;
    }

    /**
     * 判断是否为图片文件
     */
    private static boolean isImageFile(String extension) {
        return extension.equals(".jpg") || extension.equals(".jpeg") ||
               extension.equals(".png") || extension.equals(".gif") ||
               extension.equals(".webp") || extension.equals(".bmp");
    }

    /**
     * 生成缩略图
     * @param sourceFile 源图片文件
     * @param thumbFile 缩略图目标文件
     * @param extension 文件扩展名
     */
    public static void generateThumbnail(File sourceFile, File thumbFile, String extension) {
        try {
            BufferedImage originalImage = ImageIO.read(sourceFile);
            if (originalImage == null) {
                LOGGER.warn("无法读取图片文件，跳过缩略图生成: {}", sourceFile.getName());
                return;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 计算缩放比例，保持宽高比
            double scale = Math.min(
                (double) THUMB_MAX_WIDTH / originalWidth,
                (double) THUMB_MAX_HEIGHT / originalHeight
            );

            // 如果原图已经很小，不需要缩放
            if (scale >= 1.0) {
                LOGGER.info("原图尺寸 {}x{} 已小于缩略图最大尺寸，跳过缩略图生成", originalWidth, originalHeight);
                return;
            }

            int thumbWidth = (int) (originalWidth * scale);
            int thumbHeight = (int) (originalHeight * scale);

            // 创建缩略图
            BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(originalImage, 0, 0, thumbWidth, thumbHeight, null);
            g2d.dispose();

            // 确定输出格式
            String formatName = extension.startsWith(".") ? extension.substring(1) : extension;
            if ("jpg".equals(formatName)) formatName = "jpeg";

            ImageIO.write(thumbnail, formatName, thumbFile);
            LOGGER.info("缩略图已生成: {} ({}x{})", thumbFile.getName(), thumbWidth, thumbHeight);

        } catch (IOException e) {
            LOGGER.error("生成缩略图失败: {}", e.getMessage());
        }
    }

    /**
     * 根据原始文件路径获取对应的缩略图路径
     * @param originalPath 原始文件路径（如 /img/lost/123456.jpg）
     * @return 缩略图路径（如 /img/lost/thumb_123456.jpg），如果缩略图不存在则返回原图路径
     */
    public static String getThumbnailPath(String originalPath) {
        if (originalPath == null || originalPath.isEmpty()) {
            return originalPath;
        }

        int lastSlash = originalPath.lastIndexOf('/');
        if (lastSlash < 0) {
            return originalPath;
        }

        String dir = originalPath.substring(0, lastSlash);
        String filename = originalPath.substring(lastSlash + 1);

        // 如果已经是缩略图，直接返回
        if (filename.startsWith(THUMB_PREFIX)) {
            return originalPath;
        }

        String thumbFilename = THUMB_PREFIX + filename;
        String thumbPath = dir + "/" + thumbFilename;

        // 检查缩略图文件是否存在
        try {
            Path fullThumbPath = Paths.get(getBasePath(), thumbPath);
            if (Files.exists(fullThumbPath)) {
                return thumbPath;
            }
        } catch (Exception e) {
            LOGGER.warn("检查缩略图文件时出错: {}", e.getMessage());
        }

        // 缩略图不存在，返回原图
        return originalPath;
    }

    // 保存图片的方法
    public static String saveImage(MultipartFile file, String folderName) {
        return saveFile(file, folderName, "img", true);
    }

    // 保存视频的方法
    public static String saveVideo(MultipartFile file, String folderName) {
        return saveFile(file, folderName, "videos");
    }
    /**
     * 根据文件名删除文件（同时删除对应的缩略图）
     *
     * @param filename 文件名（相对于项目根目录的相对路径）
     * @return 删除成功返回 true，否则返回 false
     */
    public static boolean deleteFile(String filename) {
        try {
            // 如果路径有前导斜杠，移除它，以便路径解析正确
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }

            // 获取文件的绝对路径
            Path filePath = Paths.get(getBasePath(), filename);
            boolean result = false;
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                LOGGER.info("File deleted: {}", filePath);
                result = true;
            } else {
                LOGGER.warn("File not found: {}", filePath);
            }

            // 同时删除对应的缩略图
            int lastSlash = filename.lastIndexOf('/');
            if (lastSlash >= 0) {
                String dir = filename.substring(0, lastSlash);
                String name = filename.substring(lastSlash + 1);
                if (!name.startsWith(THUMB_PREFIX)) {
                    String thumbPath = dir + "/" + THUMB_PREFIX + name;
                    Path fullThumbPath = Paths.get(getBasePath(), thumbPath);
                    if (Files.exists(fullThumbPath)) {
                        Files.delete(fullThumbPath);
                        LOGGER.info("Thumbnail deleted: {}", fullThumbPath);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            LOGGER.error("Error deleting file: {}", filename, e);
            return false;
        }
    }

    public static void writeToFile(String fileName, String content) throws IOException {
        // 创建文件对象
        File file = new File(fileName);

        // 获取并打印文件的绝对路径
        System.out.println("Writing to file: " + file.getAbsolutePath());

        // 使用 try-with-resources 确保 FileWriter 在使用完毕后自动关闭
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(content);
        }

    }
}
