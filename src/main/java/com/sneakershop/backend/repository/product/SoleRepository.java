package com.sneakershop.backend.repository.product;
import com.sneakershop.backend.entity.product.Sole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SoleRepository extends JpaRepository<Sole, Long> {
    List<Sole> findAllByDeletedFalse();
    boolean existsByNameAndDeletedFalse(String name);

    @Query("select count(e) > 0 from Sole e where lower(trim(e.name)) = lower(trim(:name))")
    boolean existsByNameNormalized(@Param("name") String name);

    @Query("select count(e) > 0 from Sole e where lower(trim(e.name)) = lower(trim(:name)) and e.id <> :id")
    boolean existsByNameNormalizedAndIdNot(@Param("name") String name, @Param("id") Long id);

}