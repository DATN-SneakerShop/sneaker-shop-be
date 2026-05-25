package com.sneakershop.backend.service;

import com.sneakershop.backend.exception.ValidationException;
import java.text.Normalizer;
import java.util.Locale;

public final class ValidationSupport {
    public static final int MAX_TOTAL_ITEMS_PER_ORDER = 50;

    private ValidationSupport() {}

    public static String trim(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    public static String lowerTrim(String value) {
        String v = trim(value);
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }

    public static void requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("quantity", "Số lượng sản phẩm phải là số nguyên dương.");
        }
    }

    public static void validatePerItemQuantity(Integer quantity) {
        requirePositiveQuantity(quantity);
    }

    public static void validateTotalQuantity(int total) {
        if (total > MAX_TOTAL_ITEMS_PER_ORDER) {
            throw new ValidationException("quantity", "Tổng số lượng sản phẩm trong đơn vượt quá giới hạn cho phép.");
        }
    }
}
