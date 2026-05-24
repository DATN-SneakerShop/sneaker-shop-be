package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findAllByDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);

    @Query("select count(e) > 0 from Color e where lower(trim(e.name)) = lower(trim(:name))")
    boolean existsByNameNormalized(@Param("name") String name);

    @Query("select count(e) > 0 from Color e where lower(trim(e.name)) = lower(trim(:name)) and e.id <> :id")
    boolean existsByNameNormalizedAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Query("select count(c) > 0 from Color c where c.hexCode is not null and lower(trim(c.hexCode)) = lower(trim(:hexCode))")
    boolean existsByHexCodeNormalized(@Param("hexCode") String hexCode);

    @Query("select count(c) > 0 from Color c where c.hexCode is not null and lower(trim(c.hexCode)) = lower(trim(:hexCode)) and c.id <> :id")
    boolean existsByHexCodeNormalizedAndIdNot(@Param("hexCode") String hexCode, @Param("id") Long id);

}