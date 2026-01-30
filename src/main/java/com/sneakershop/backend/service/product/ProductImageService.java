package com.sneakershop.backend.service.product;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    @Value("${upload.path}")
    private String uploadPath;

    /**
     * Upload ảnh tạm thời (chưa gắn product)
     */
    public String uploadTemp(MultipartFile file) {

        if (file.isEmpty()) {
            throw new RuntimeException("File rỗng");
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dir = new File(uploadPath);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, fileName);

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("Upload failed", e);
        }

        // FE sẽ dùng URL này để preview
        return "uploads/" + fileName;
    }
}
