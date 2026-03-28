package com.sneakershop.backend.entity.voucher;

import com.sneakershop.backend.entity.customer.Customer;
import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "voucher_customer")
@Data
public class VoucherCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}