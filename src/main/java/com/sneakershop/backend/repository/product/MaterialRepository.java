package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {}