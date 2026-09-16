package com.telecom.tollfree.api;

import com.telecom.tollfree.config.SecurityConfig;
import com.telecom.tollfree.service.NumberService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NumberController.class)
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
class NumberControllerSecurityTest {
	@Autowired
	MockMvc mvc;
	@MockBean
	NumberService service;

	@Test
	void rejectsUnauthenticatedMutation() throws Exception {
		mvc.perform(post("/api/v1/numbers").contentType("application/json").content("{\"number\":\"8001234567\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "READ_ONLY")
	void rejectsReadOnlyMutation() throws Exception {
		mvc.perform(post("/api/v1/numbers").contentType("application/json").content("{\"number\":\"8001234567\"}"))
				.andExpect(status().isForbidden());
	}
}
