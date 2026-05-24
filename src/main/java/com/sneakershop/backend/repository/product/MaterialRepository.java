package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);

    @Query("select count(e) > 0 from Material e where lower(trim(e.name)) = lower(trim(:name))")
    boolean existsByNameNormalized(@Param("name") String name);

    @Query("select count(e) > 0 from Material e where lower(trim(e.name)) = lower(trim(:name)) and e.id <> :id")
    boolean existsByNameNormalizedAndIdNot(@Param("name") String name, @Param("id") Long id);

}