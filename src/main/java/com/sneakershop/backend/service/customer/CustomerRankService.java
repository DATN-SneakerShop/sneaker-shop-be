package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.CustomerRank;
import com.sneakershop.backend.repository.customer.CustomerRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerRankService {

    private final CustomerRankRepository rankRepository;

    public List<CustomerRank> getAllRanks() {
        return rankRepository.findAllByOrderByMinPointsDesc();
    }

    public CustomerRank createRank(CustomerRank rank) {
        if (rankRepository.existsByName(rank.getName())) {
            throw new RuntimeException("Tên hạng này đã tồn tại!");
        }
        return rankRepository.save(rank);
    }

    public CustomerRank updateRank(Long id, CustomerRank data) {
        CustomerRank rank = rankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng này"));

        rank.setName(data.getName());
        rank.setMinPoints(data.getMinPoints());
        rank.setDiscountPercent(data.getDiscountPercent());
        rank.setDescription(data.getDescription());

        return rankRepository.save(rank);
    }

    public void deleteRank(Long id) {
        rankRepository.deleteById(id);
    }
}