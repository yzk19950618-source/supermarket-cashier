package com.cashier.module.file.controller;

import com.cashier.common.result.R;
import com.cashier.common.result.ResultCode;
import com.cashier.module.file.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 本地上传（保存到 app.upload-dir，通过 /uploads/** 访问）。
 * 图片经服务端压缩为 JPEG（长边与质量见 app.image-upload）；无法解码时保留原文件。
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final Path uploadRoot;
    private final ImageUploadService imageUploadService;

    public FileController(
            @Value("${app.upload-dir}") String uploadDir,
            ImageUploadService imageUploadService) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.imageUploadService = imageUploadService;
    }

    @Operation(summary = "上传图片")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return R.fail(ResultCode.PARAM_ERROR.getCode(), "file is empty");
        }
        String original = file.getOriginalFilename();
        String ext = extensionFromFilename(original);
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp|heic|heif)")) {
            return R.fail(ResultCode.PARAM_ERROR.getCode(), "仅支持 JPG、PNG、GIF、WebP、BMP、HEIC/HEIF 图片");
        }
        String ym = YearMonth.now().toString().replace("-", "");
        Path dir = uploadRoot.resolve(ym);
        Files.createDirectories(dir);
        String base = UUID.randomUUID().toString().replace("-", "");
        Path temp = Files.createTempFile("cashier-upload-", ".part");
        try {
            file.transferTo(temp.toFile());
            Path targetJpg = dir.resolve(base + ".jpg");
            if (imageUploadService.tryWriteCompressedJpeg(temp, targetJpg)) {
                return okBody(ym, base + ".jpg");
            }
            Path targetRaw = dir.resolve(base + ext);
            Files.copy(temp, targetRaw, StandardCopyOption.REPLACE_EXISTING);
            log.info("Uploaded without re-encode -> {}", targetRaw);
            return okBody(ym, base + ext);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String extensionFromFilename(String original) {
        if (original == null || !original.contains(".")) {
            return ".jpg";
        }
        return original.substring(original.lastIndexOf('.')).toLowerCase();
    }

    private R<Map<String, String>> okBody(String ym, String filename) {
        String url = "/uploads/" + ym + "/" + filename;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("url", url);
        body.put("path", url);
        log.info("Uploaded file -> {}", url);
        return R.ok(body);
    }
}
