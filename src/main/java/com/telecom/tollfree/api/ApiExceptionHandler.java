package com.telecom.tollfree.api;

import com.telecom.tollfree.api.ApiModels.ErrorView;
import com.telecom.tollfree.service.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ErrorView notFound(Exception e) {
		return new ErrorView("NOT_FOUND", e.getMessage());
	}

	@ExceptionHandler({ ConflictException.class, DataIntegrityViolationException.class })
	@ResponseStatus(HttpStatus.CONFLICT)
	ErrorView conflict(Exception e) {
		return new ErrorView("CONFLICT", e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ErrorView validation(MethodArgumentNotValidException e) {
		return new ErrorView("VALIDATION_ERROR", e.getBindingResult().getFieldError().getDefaultMessage());
	}
}
