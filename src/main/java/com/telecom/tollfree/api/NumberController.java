package com.telecom.tollfree.api;

import com.telecom.tollfree.api.ApiModels.*;
import com.telecom.tollfree.domain.NumberStatus;
import com.telecom.tollfree.service.NumberService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/numbers")
@SecurityRequirement(name = "bearerAuth")
public class NumberController {
	private final NumberService service;

	public NumberController(NumberService s) {
		service = s;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
	public NumberView importNumber(@Valid @RequestBody NumberInput x) {
		return service.importNumber(x.number());
	}

	@GetMapping("/{number}")
	public NumberView status(@PathVariable String number) {
		return service.status(number);
	}

	@GetMapping
	public Page<NumberView> search(@RequestParam(required = false) NumberStatus status,
			@PageableDefault(size = 20) Pageable p) {
		return service.search(status, p);
	}

	@PostMapping("/{number}/actions/{action}")
	@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
	public NumberView action(@PathVariable String number, @PathVariable NumberStatus action) {
		return service.transition(number, action);
	}
}
