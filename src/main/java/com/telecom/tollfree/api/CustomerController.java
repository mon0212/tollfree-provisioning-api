package com.telecom.tollfree.api;

import com.telecom.tollfree.api.ApiModels.*;
import com.telecom.tollfree.service.CustomerService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {
	private final CustomerService service;

	public CustomerController(CustomerService s) {
		service = s;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CustomerView create(@Valid @RequestBody CustomerInput x) {
		return service.create(x);
	}

	@GetMapping("/{id}")
	public CustomerView get(@PathVariable String id) {
		return service.get(id);
	}

	@PutMapping("/{id}")
	public CustomerView update(@PathVariable String id, @Valid @RequestBody CustomerInput x) {
		return service.update(id, x);
	}

	@GetMapping
	public Page<CustomerView> list(@PageableDefault(size = 20) Pageable p) {
		return service.list(p);
	}
}
