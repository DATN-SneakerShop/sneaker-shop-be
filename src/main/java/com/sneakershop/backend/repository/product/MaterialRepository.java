package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);
}