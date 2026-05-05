package com.sneakershop.backend.service.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class ProductImageService {

    // 🔥 Lấy cấu hình từ application.properties giống như WebConfig
    @org.springframework.beans.factory.annotation.Value("${upload.path}")
    private String uploadPathConfig;

    public String uploadTemp(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File rỗng");
        }

        try {
            // Dùng biến cấu hình thay vì Paths.get("src", "main", ...)
            Path uploadPath = Paths.get(uploadPathConfig);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn để FE gọi: http://localhost:8080/uploads/ten_file.jpg
            return "uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }
}