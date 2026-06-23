package org.example.springboot.service;


import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.example.springboot.common.Result;
import org.example.springboot.enumClass.FileType;
import org.example.springboot.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    /**
     * 允许上传的图片MIME类型白名单
     */
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    /**
     * 允许上传的图片扩展名白名单
     */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    /**
     * 允许上传的通用文件MIME类型白名单
     */
    private static final Set<String> ALLOWED_COMMON_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
        "application/pdf", "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    );

    /**
     * 允许上传的通用文件扩展名白名单
     */
    private static final Set<String> ALLOWED_COMMON_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "pdf", "doc", "docx", "xls", "xlsx", "txt"
    );

    @Operation(summary = "文件上传")
    public Result<?> upLoad(MultipartFile file, FileType fileType) {
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(file.getOriginalFilename())) {
            LOGGER.error("文件不存在");
            return Result.error("-1", "文件不存在！");
        }
        LOGGER.info("upload FILE:" + file.getOriginalFilename());

        // 文件类型安全校验
        String validationError = validateFile(file, fileType);
        if (validationError != null) {
            LOGGER.warn("文件类型校验不通过: {}, type={}", file.getOriginalFilename(), fileType);
            return Result.error("-1", validationError);
        }

        String path = FileUtil.saveFile(file, null, fileType.getTypeName());
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(path)) {
            return Result.success(path);
        } else {
            return Result.error("-1", "文件上传失败");
        }
    }

    /**
     * 校验文件类型是否在白名单内
     * @param file 上传的文件
     * @param fileType 文件分类（IMG/COMMON）
     * @return 校验失败返回错误信息，成功返回null
     */
    private String validateFile(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            return "文件不能为空";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "文件名不能为空";
        }

        // 校验文件扩展名
        String extension = getFileExtension(originalFilename).toLowerCase();
        boolean extensionValid;
        if (fileType == FileType.IMG) {
            extensionValid = ALLOWED_IMAGE_EXTENSIONS.contains(extension);
        } else {
            extensionValid = ALLOWED_COMMON_EXTENSIONS.contains(extension);
        }

        if (!extensionValid) {
            return "不允许上传该类型的文件（." + extension + "）。" +
                   (fileType == FileType.IMG ? "仅支持图片格式：" : "仅支持：") +
                   (fileType == FileType.IMG ? String.join(", ", ALLOWED_IMAGE_EXTENSIONS) : String.join(", ", ALLOWED_COMMON_EXTENSIONS));
        }

        // 校验MIME类型
        String mimeType = file.getContentType();
        if (mimeType != null) {
            boolean mimeValid;
            if (fileType == FileType.IMG) {
                mimeValid = ALLOWED_IMAGE_MIME_TYPES.contains(mimeType.toLowerCase());
            } else {
                mimeValid = ALLOWED_COMMON_MIME_TYPES.contains(mimeType.toLowerCase());
            }
            if (!mimeValid) {
                // 对于图片类型，MIME校验失败直接拒绝
                // 对于通用类型，仅警告但不阻止（因为某些系统可能不识别MIME）
                if (fileType == FileType.IMG) {
                    return "不支持的图片格式（MIME: " + mimeType + "）";
                }
                LOGGER.warn("文件MIME类型不在推荐白名单中: {}, MIME: {}", originalFilename, mimeType);
            }
        }

        return null;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    /**
     * 删除文件（带安全校验，防止路径遍历攻击）
     */
    @DeleteMapping("/remove/{filename}")
    public Result<?> fileRemove(@PathVariable String filename){
        // 安全校验：只允许安全的文件名
        if (!isValidFileName(filename)) {
            LOGGER.warn("非法文件名尝试删除: {}", filename);
            return Result.error("-1", "文件名包含非法字符");
        }

        String filePath = File.separator + "img" + File.separator + filename;
        boolean res = FileUtil.deleteFile(filePath);

        return res? Result.success():Result.error("-1","删除失败！");
    }

    /**
     * 校验文件名是否安全（防止路径遍历攻击）
     * 只允许字母、数字、下划线、连字符和点
     */
    private boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        // 禁止包含路径分隔符或路径遍历
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }
        // 只允许安全字符：字母、数字、下划线、连字符、点
        return fileName.matches("^[a-zA-Z0-9_\\-.]++$");
    }

    public List<String> uploadMultiple(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            LOGGER.error("没有文件上传");
            return null;
        }

        List<String> successPaths = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (StringUtils.isEmpty(file.getOriginalFilename())) {
                    failedFiles.add(file.getOriginalFilename() + ": 文件不存在");
                    continue;
                }
                LOGGER.info("upload FILE:" + file.getOriginalFilename());

                // 文件类型安全校验
                String validationError = validateFile(file, FileType.COMMON);
                if (validationError != null) {
                    LOGGER.warn("文件类型校验不通过: {}, error={}", file.getOriginalFilename(), validationError);
                    failedFiles.add(file.getOriginalFilename() + ": " + validationError);
                    continue;
                }

                String path = FileUtil.saveFile(file,null,FileType.COMMON.getTypeName());
                if (StringUtils.isNotBlank(path)) {
                    successPaths.add(path);
                } else {
                    failedFiles.add(file.getOriginalFilename() + ": 文件上传失败");
                }
            } catch (Exception e) {
                LOGGER.error("文件上传时发生异常: " + e.getMessage(), e);
                failedFiles.add(file.getOriginalFilename() + ": 文件上传时发生异常");
            }
        }

        // 检查是否所有文件都成功上传
        if (!failedFiles.isEmpty()) {
            // 如果有文件上传失败，删除所有成功上传的文件
            for (String path : successPaths) {
                File uploadedFile = new File(path);
                if (uploadedFile.exists() && uploadedFile.isFile()) {
                    if (uploadedFile.delete()) {
                        LOGGER.info("Deleted successfully uploaded file: " + path);
                    } else {
                        LOGGER.warn("Failed to delete file: " + path);
                    }
                }
            }

            // 返回错误信息
            return null;
        } else {
            // 如果全部成功，则只返回成功路径
            return successPaths;
        }
    }
}
