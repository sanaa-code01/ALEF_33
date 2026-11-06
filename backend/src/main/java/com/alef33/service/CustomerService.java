package com.alef33.service;

import com.alef33.dto.CustomerRequest;
import com.alef33.exception.BusinessException;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.Customer;
import com.alef33.repository.CustomerRepository;
import com.alef33.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;

    @Transactional(readOnly = true)
    public List<Customer> list(String search) {
        if (search == null || search.isBlank()) {
            return customerRepository.findAllByOrderByNameAsc();
        }
        return customerRepository.findByNameContainingIgnoreCaseOrderByNameAsc(search.trim());
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        Customer customer = new Customer();
        apply(customer, request);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        apply(customer, request);
        return customer;
    }

    /** A customer with quotes can't be deleted (it would break the documents' history) */
    @Transactional
    public void delete(Long id) {
        Customer customer = get(id);
        if (quoteRepository.existsByCustomerId(id)) {
            throw new BusinessException("Customer " + customer.getName()
                    + " has quotes and cannot be deleted");
        }
        customerRepository.delete(customer);
    }

    private void apply(Customer customer, CustomerRequest r) {
        customer.setName(r.name().trim());
        customer.setIce(r.ice());
        customer.setEmail(r.email());
        customer.setPhone(r.phone());
        customer.setAddress(r.address());
        customer.setCity(r.city());
    }
}