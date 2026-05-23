package com.lawencon.leave_service.controller;

import com.lawencon.leave_service.dto.AuthLoginRequest;
import com.lawencon.leave_service.dto.AuthLoginResponse;
import com.lawencon.leave_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
}
