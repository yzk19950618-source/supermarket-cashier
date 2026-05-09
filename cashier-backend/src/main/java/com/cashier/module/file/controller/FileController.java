package com.cashier.module.file.controller;

import com.cashier.common.result.R;
import com.cashier.common.result.ResultCode;
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
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 本地上传（保存到 app.upload-dir，通过 /uploads/** 访问）。
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final Path uploadRoot;

    public FileController(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Operation(summary = "上传图片")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return R.fail(ResultCode.PARAM_ERROR.getCode(), "file is empty");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
            ext = ".jpg";
        }
        String ym = YearMonth.now().toString().replace("-", "");
        Path dir = uploadRoot.resolve(ym);
        Files.createDirectories(dir);
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(name);
        file.transferTo(target.toFile());
        String url = "/uploads/" + ym + "/" + name;
        Map<String, String> body = new LinkedHashMap<>();
        body.put("url", url);
        body.put("path", url);
        log.info("Uploaded file -> {}", target);
        return R.ok(body);
    }
}
