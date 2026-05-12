package com.cashier.module.file.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 将常见位图（含手机拍照、WebP）压缩为 JPEG：限制长边、质量压缩，并处理 EXIF 方向（Thumbnailator）。
 * 解码失败时由调用方回退为原文件拷贝。
 */
@Slf4j
@Service
public class ImageUploadService {

    @Value("${app.image-upload.max-long-edge:2048}")
    private int maxLongEdge;

    @Value("${app.image-upload.jpeg-quality:0.82}")
    private float jpegQuality;

    /**
     * 写入压缩后的 JPEG。
     *
     * @param source 临时文件等可读路径
     * @param destJpg 目标路径（建议以 .jpg 结尾）
     * @return 是否成功写入
     */
    public boolean tryWriteCompressedJpeg(Path source, Path destJpg) {
        try {
            long before = Files.size(source);
            Thumbnails.of(source.toFile())
                    .size(maxLongEdge, maxLongEdge)
                    .outputFormat("jpg")
                    .outputQuality(jpegQuality)
                    .toFile(destJpg.toFile());
            long after = Files.size(destJpg);
            log.info("Image upload compressed {} bytes -> {} bytes (JPEG)", before, after);
            return true;
        } catch (IOException e) {
            log.warn("Image JPEG compress skipped: {}", e.getMessage());
            return false;
        }
    }
}
