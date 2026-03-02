package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.dto.product.ProductSearchRequest;
import com.sneakershop.backend.entity.product.Category;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductVariant;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> build(ProductSearchRequest req) {
        return (root, query, cb) -> {

            // 🔥 QUAN TRỌNG: tránh duplicate khi join variants + categories
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            /* =====================================================
               XÁC ĐỊNH CÓ CẦN JOIN VARIANT KHÔNG (tối ưu hiệu năng)
            ===================================================== */
            Join<Product, ProductVariant> variantJoin = null;

            boolean needVariantJoin =
                    (req.getKeyword() != null && !req.getKeyword().isBlank()) ||
                            (req.getSize() != null && !req.getSize().isBlank()) ||
                            (req.getSizeType() != null && !req.getSizeType().isBlank()) ||
                            (req.getColorway() != null && !req.getColorway().isBlank()) ||
                            (req.getVariantStatus() != null && !req.getVariantStatus().isBlank()) ||
                            (req.getSortPrice() != null && !req.getSortPrice().isBlank());

            if (needVariantJoin) {
                variantJoin = root.join("variants", JoinType.LEFT);
            }

            /* =====================================================
               KEYWORD SEARCH (🔥 1 Ô SEARCH CHUNG CHO SHOP GIÀY)
               Tìm theo:
               - Product: name, sku, brand, gender, releaseType
               - Variant: sku, size, colorway
               - Category: name
            ===================================================== */
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {

                String keyword = "%" + req.getKeyword().toLowerCase().trim() + "%";

                // đảm bảo đã join variant khi search keyword
                if (variantJoin == null) {
                    variantJoin = root.join("variants", JoinType.LEFT);
                }

                // join category để search theo tên danh mục
                Join<Product, Category> categoryJoin =
                        root.join("categories", JoinType.LEFT);

                // ===== PRODUCT FIELDS =====
                Predicate pName = cb.like(cb.lower(root.get("name")), keyword);
                Predicate pSku = cb.like(cb.lower(root.get("sku")), keyword);
                Predicate pBrand = cb.like(cb.lower(root.get("brand")), keyword);
                Predicate pGender = cb.like(cb.lower(root.get("gender")), keyword);
                Predicate pReleaseType = cb.like(cb.lower(root.get("releaseType")), keyword);

                // ===== VARIANT FIELDS (rất quan trọng cho shop giày) =====
                Predicate vSku = cb.like(cb.lower(variantJoin.get("sku")), keyword);
                Predicate vSize = cb.like(cb.lower(variantJoin.get("size")), keyword);
                Predicate vColorway = cb.like(cb.lower(variantJoin.get("colorway")), keyword);

                // ===== CATEGORY FIELD =====
                Predicate cName = cb.like(cb.lower(categoryJoin.get("name")), keyword);

                predicates.add(
                        cb.or(
                                pName,
                                pSku,
                                pBrand,
                                pGender,
                                pReleaseType,
                                vSku,
                                vSize,
                                vColorway,
                                cName
                        )
                );
            }

            /* =====================================================
               CATEGORY FILTER
            ===================================================== */
            if (req.getCategoryIds() != null && !req.getCategoryIds().isEmpty()) {
                Join<Product, Category> categoryJoin =
                        root.join("categories", JoinType.LEFT);

                predicates.add(categoryJoin.get("id").in(req.getCategoryIds()));
            }

            /* =====================================================
               PRODUCT FILTER
            ===================================================== */
            if (req.getBrand() != null && !req.getBrand().isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("brand")),
                                req.getBrand().toLowerCase().trim()
                        )
                );
            }

            if (req.getGender() != null && !req.getGender().isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("gender")),
                                req.getGender().toLowerCase().trim()
                        )
                );
            }

            if (req.getReleaseType() != null && !req.getReleaseType().isBlank()) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("releaseType")),
                                req.getReleaseType().toLowerCase().trim()
                        )
                );
            }

            if (req.getLimited() != null) {
                predicates.add(
                        cb.equal(root.get("limited"), req.getLimited())
                );
            }

            /* =====================================================
               VARIANT FILTER
            ===================================================== */
            if (variantJoin != null) {

                if (req.getSize() != null && !req.getSize().isBlank()) {
                    predicates.add(
                            cb.equal(
                                    cb.lower(variantJoin.get("size")),
                                    req.getSize().toLowerCase().trim()
                            )
                    );
                }

                if (req.getSizeType() != null && !req.getSizeType().isBlank()) {
                    predicates.add(
                            cb.equal(
                                    cb.lower(variantJoin.get("sizeType")),
                                    req.getSizeType().toLowerCase().trim()
                            )
                    );
                }

                if (req.getColorway() != null && !req.getColorway().isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(variantJoin.get("colorway")),
                                    "%" + req.getColorway().toLowerCase().trim() + "%"
                            )
                    );
                }

                if (req.getVariantStatus() != null && !req.getVariantStatus().isBlank()) {
                    predicates.add(
                            cb.equal(
                                    cb.lower(variantJoin.get("status")),
                                    req.getVariantStatus().toLowerCase().trim()
                            )
                    );
                }
            }

            /* =====================================================
               SORT THEO GIÁ (min price của variants)
               ?sortPrice=asc | desc
            ===================================================== */
            if (req.getSortPrice() != null && !req.getSortPrice().isBlank()) {

                // dùng lại join cũ để tránh join 2 lần gây chậm
                Join<Product, ProductVariant> priceJoin =
                        (variantJoin != null)
                                ? variantJoin
                                : root.join("variants", JoinType.LEFT);

                // group by để dùng MIN(price)
                query.groupBy(root.get("id"));

                if ("asc".equalsIgnoreCase(req.getSortPrice())) {
                    query.orderBy(cb.asc(cb.min(priceJoin.get("price"))));
                } else if ("desc".equalsIgnoreCase(req.getSortPrice())) {
                    query.orderBy(cb.desc(cb.min(priceJoin.get("price"))));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}