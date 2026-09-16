package com.telecom.tollfree.api;

import com.telecom.tollfree.api.ApiModels.*;
import com.telecom.tollfree.service.ProvisioningService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/provisioning")
@SecurityRequirement(name = "bearerAuth")

public class ProvisioningController {
	private final ProvisioningService service;

	public ProvisioningController(ProvisioningService s) {
		service = s;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
	public ProvisionView provision(@Valid @RequestBody ProvisionInput x) {
		return service.provision(x);
	}
}
