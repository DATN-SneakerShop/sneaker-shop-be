package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // Lấy danh sách hình theo sản phẩm
    List<ProductImage> findByProductId(Long productId);

    // Lấy thumbnail
    ProductImage findByProductIdAndIsThumbnailTrue(Long productId);
}
