package com.analysiscore.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.analysiscore.application.dto.TestDTO;

@RestController
public class TestController {

	@PostMapping()
	public ResponseEntity<TestDTO.TestResponse> test() {
		return ResponseEntity.ok(new TestDTO.TestResponse("test"));
	}

}
