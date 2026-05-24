package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.CustomerRank;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.repository.customer.CustomerRankRepository;
import com.sneakershop.backend.service.ValidationSupport;
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
        validateRank(rank, null);
        rank.setName(ValidationSupport.trim(rank.getName()));
        return rankRepository.save(rank);
    }

    public CustomerRank updateRank(Long id, CustomerRank data) {
        CustomerRank rank = rankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hạng này"));
        validateRank(data, id);
        rank.setName(ValidationSupport.trim(data.getName()));
        rank.setMinPoints(data.getMinPoints());
        rank.setDiscountPercent(data.getDiscountPercent());
        rank.setDescription(data.getDescription());
        return rankRepository.save(rank);
    }

    public void deleteRank(Long id) {
        rankRepository.deleteById(id);
    }

    private void validateRank(CustomerRank rank, Long currentId) {
        String name = ValidationSupport.trim(rank.getName());
        if (name == null) throw new ValidationException("name", "Tên hạng khách hàng không được để trống.");
        Integer minPoints = rank.getMinPoints();
        if (minPoints == null || minPoints < 0) throw new ValidationException("minPoints", "Điểm tối thiểu không được âm.");
        Integer discount = rank.getDiscountPercent() == null ? 0 : rank.getDiscountPercent();
        if (discount < 0 || discount > 100) throw new ValidationException("discountPercent", "Phần trăm giảm giá phải từ 0 đến 100.");
        boolean duplicateName = currentId == null ? rankRepository.existsByNameNormalized(name) : rankRepository.existsByNameNormalizedAndIdNot(name, currentId);
        if (duplicateName) throw new ValidationException("name", "Tên hạng khách hàng đã tồn tại.");
        boolean duplicatePoint = currentId == null ? rankRepository.existsByMinPoints(minPoints) : rankRepository.existsByMinPointsAndIdNot(minPoints, currentId);
        if (duplicatePoint) throw new ValidationException("minPoints", "Mốc điểm của hạng khách hàng đã tồn tại.");
    }
}
