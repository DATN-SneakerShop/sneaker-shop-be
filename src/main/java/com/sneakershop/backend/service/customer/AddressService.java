package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.Address;
import com.sneakershop.backend.repository.customer.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;

    public List<Address> getByCustomer(Long customerId) {
        return addressRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Address save(Address address) {
        // Nếu địa chỉ mới là mặc định, hủy mặc định các địa chỉ cũ
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            List<Address> list = addressRepository.findByCustomerId(address.getCustomer().getId());
            list.forEach(a -> a.setIsDefault(0));
            addressRepository.saveAll(list);
        }
        return addressRepository.save(address);
    }

    public void delete(Long id) { addressRepository.deleteById(id); }
}