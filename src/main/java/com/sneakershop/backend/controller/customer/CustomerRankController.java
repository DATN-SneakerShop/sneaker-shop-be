package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.entity.customer.CustomerRank;
import com.sneakershop.backend.service.customer.CustomerRankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customer-ranks")
@RequiredArgsConstructor
public class CustomerRankController {

    private final CustomerRankService rankService;

    @GetMapping
    public List<CustomerRank> getAll() {
        return rankService.getAllRanks();
    }

    @PostMapping
    public CustomerRank create(@RequestBody CustomerRank rank) {
        return rankService.createRank(rank);
    }

    @PutMapping("/{id}")
    public CustomerRank update(@PathVariable Long id, @RequestBody CustomerRank rank) {
        return rankService.updateRank(id, rank);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        rankService.deleteRank(id);
    }
}