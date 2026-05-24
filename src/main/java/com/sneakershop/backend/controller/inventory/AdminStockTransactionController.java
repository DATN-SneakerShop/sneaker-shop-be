package com.sneakershop.backend.controller.inventory;

import com.sneakershop.backend.dto.inventory.StockTransactionDTO;
import com.sneakershop.backend.entity.inventory.StockTransaction;
import com.sneakershop.backend.repository.inventory.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/inventory/transactions")
@RequiredArgsConstructor
public class AdminStockTransactionController {

    private final StockTransactionRepository stockTransactionRepository;

    @GetMapping
    public List<StockTransactionDTO> list(
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) Long referenceId
    ) {
        List<StockTransaction> transactions;
        if (variantId != null) {
            transactions = stockTransactionRepository.findByVariant_IdOrderByCreatedAtDesc(variantId);
        } else if (referenceType != null && referenceId != null) {
            transactions = stockTransactionRepository.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(referenceType, referenceId);
        } else {
            transactions = stockTransactionRepository.findAll();
            transactions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }
        return transactions.stream().map(this::toDto).collect(Collectors.toList());
    }

    private StockTransactionDTO toDto(StockTransaction tx) {
        StockTransactionDTO dto = new StockTransactionDTO();
        dto.setId(tx.getId());
        if (tx.getVariant() != null) {
            dto.setVariantId(tx.getVariant().getId());
            dto.setSku(tx.getVariant().getSku());
        }
        dto.setType(tx.getType());
        dto.setQuantity(tx.getQuantity());
        dto.setBeforeStock(tx.getBeforeStock());
        dto.setAfterStock(tx.getAfterStock());
        dto.setBeforeReservedQuantity(tx.getBeforeReservedQuantity());
        dto.setAfterReservedQuantity(tx.getAfterReservedQuantity());
        dto.setBeforeAvailableStock(Math.max(0, tx.getBeforeStock() - tx.getBeforeReservedQuantity()));
        dto.setAfterAvailableStock(Math.max(0, tx.getAfterStock() - tx.getAfterReservedQuantity()));
        dto.setReferenceType(tx.getReferenceType());
        dto.setReferenceId(tx.getReferenceId());
        dto.setNote(tx.getNote());
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }
}
