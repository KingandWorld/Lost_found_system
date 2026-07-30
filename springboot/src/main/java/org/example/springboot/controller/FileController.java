package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springboot.common.Result;
import org.example.springboot.enumClass.FileType;
import org.example.springboot.service.FileService;
import org.example.springboot.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "文件上传接口类")
@RequestMapping("/file")
@RestController
public class FileController {

    @Autowired
    private FileService fileService;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    @Operation(summary = "文件上传")
    @PostMapping("/upload/img")
    public Result<?> upLoad(@RequestParam("file") MultipartFile file) {
      return   fileService.upLoad(file, FileType.IMG);
    }
    @Operation(summary = "多文件上传，并且在有失败时删除已上传成功的文件")
    @PostMapping("/uploadMultiple")
    public Result<?> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        List<String> strings = fileService.uploadMultiple(files);
        return !strings.isEmpty() ? Result.success(strings):Result.error("-1","文件上传失败！");
    }

    @Operation(summary = "获取缩略图路径（如果缩略图不存在则返回原图路径）")
    @GetMapping("/thumbnail")
    public Result<?> getThumbnail(@RequestParam String path) {
        String thumbPath = FileUtil.getThumbnailPath(path);
        return Result.success(thumbPath);
    }
}





