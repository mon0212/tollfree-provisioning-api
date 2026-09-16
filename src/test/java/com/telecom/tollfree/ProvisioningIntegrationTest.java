package com.telecom.tollfree;

import com.telecom.tollfree.api.ApiModels.*;
import com.telecom.tollfree.domain.*;
import com.telecom.tollfree.messaging.NumberEventPublisher;
import com.telecom.tollfree.repository.*;
import com.telecom.tollfree.service.ProvisioningService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ProvisioningIntegrationTest {
	@Autowired
	CustomerRepository customers;
	@Autowired
	TelecomNumberRepository numbers;
	@Autowired
	ProvisioningService provisioning;
	@MockBean
	NumberEventPublisher events;

	@BeforeEach
	void clean() {
		numbers.deleteAll();
		customers.deleteAll();
	}

	@Test
	void provisionsPersistedCustomerAndNumber() {
		Customer c = new Customer();
		c.setName("Acme");
		c.setEmail("acme@example.test");
		c = customers.save(c);
		numbers.save(new TelecomNumber("8001234567"));
		var response = provisioning.provision(new ProvisionInput(c.getId(), "8001234567"));
		assertEquals(NumberStatus.RESERVED, response.status());
		assertEquals(NumberStatus.RESERVED, numbers.findById("8001234567").orElseThrow().getStatus());
		verify(events).publish(argThat(e -> e.eventType().equals("NumberReserved")));
	}
}
