package com.alef33.dto;

import com.alef33.model.Customer;

public record CustomerResponse(
        Long id,
        String name,
        String ice,
        String email,
        String phone,
        String address,
        String city
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getIce(), c.getEmail(),
                c.getPhone(), c.getAddress(), c.getCity());
    }
}