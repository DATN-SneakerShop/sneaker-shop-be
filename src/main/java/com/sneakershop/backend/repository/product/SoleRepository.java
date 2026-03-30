package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Sole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SoleRepository extends JpaRepository<Sole, Long> {
    List<Sole> findAllByDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);
}