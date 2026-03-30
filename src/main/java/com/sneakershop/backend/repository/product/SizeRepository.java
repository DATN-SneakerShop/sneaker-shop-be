package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {
    List<Size> findAllByDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);
}