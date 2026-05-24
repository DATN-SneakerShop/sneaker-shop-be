package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.CustomerRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerRankRepository extends JpaRepository<CustomerRank, Long> {
    // Sắp xếp giảm dần theo điểm để dễ dàng quét hạng cho khách
    List<CustomerRank> findAllByOrderByMinPointsDesc();

    boolean existsByName(String name);

    @Query("select count(r) > 0 from CustomerRank r where lower(trim(r.name)) = lower(trim(:name))")
    boolean existsByNameNormalized(@Param("name") String name);

    @Query("select count(r) > 0 from CustomerRank r where lower(trim(r.name)) = lower(trim(:name)) and r.id <> :id")
    boolean existsByNameNormalizedAndIdNot(@Param("name") String name, @Param("id") Long id);

    boolean existsByMinPoints(Integer minPoints);
    boolean existsByMinPointsAndIdNot(Integer minPoints, Long id);

}