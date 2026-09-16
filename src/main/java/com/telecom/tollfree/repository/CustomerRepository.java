package com.telecom.tollfree.repository;

import com.telecom.tollfree.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
