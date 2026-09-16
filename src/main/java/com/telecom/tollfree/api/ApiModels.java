package com.telecom.tollfree.api;

import com.telecom.tollfree.domain.NumberStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;

public final class ApiModels {
	private ApiModels() {
	}

	public record CustomerInput(@NotBlank String name, @Email @NotBlank String email) {
	}

	public record CustomerView(String id, String name, String email) {
	}

	public record NumberInput(
			@Pattern(regexp = "^800[0-9]{7}$", message = "must be a 10 digit 800 number") String number) {
	}

	public record NumberView(String number, NumberStatus status, String customerId) implements Serializable {
	}

	public record ProvisionInput(@NotBlank String customerId, @Pattern(regexp = "^800[0-9]{7}$") String number) {
	}

	public record ProvisionView(String requestId, String number, NumberStatus status, boolean idempotent) {
	}

	public record ErrorView(String code, String message) {
	}
}
