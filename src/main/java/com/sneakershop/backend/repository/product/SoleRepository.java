package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Sole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoleRepository extends JpaRepository<Sole, Long> {}