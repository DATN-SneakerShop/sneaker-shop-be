package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCustomerId(Long customerId);
}