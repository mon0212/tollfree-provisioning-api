package com.telecom.tollfree.repository;

import com.telecom.tollfree.domain.ProvisioningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProvisioningRequestRepository extends JpaRepository<ProvisioningRequest, String> {
	Optional<ProvisioningRequest> findByCustomerIdAndNumber(String customerId, String number);
}
