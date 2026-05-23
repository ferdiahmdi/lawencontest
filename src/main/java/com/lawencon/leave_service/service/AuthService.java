package com.lawencon.leave_service.service;

import com.lawencon.leave_service.config.JwtProperties;
import com.lawencon.leave_service.dto.AuthLoginRequest;
import com.lawencon.leave_service.dto.AuthLoginResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;

  public AuthService(
      AuthenticationManager authenticationManager,
      JwtEncoder jwtEncoder,
      JwtProperties jwtProperties) {
    this.authenticationManager = authenticationManager;
    this.jwtEncoder = jwtEncoder;
    this.jwtProperties = jwtProperties;
  }

  public AuthLoginResponse login(AuthLoginRequest request) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    Instant now = Instant.now();
    List<String> roles = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
        .toList();

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(jwtProperties.issuer())
        .subject(authentication.getName())
        .issuedAt(now)
        .claim("roles", roles)
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return new AuthLoginResponse(token, "Bearer");
  }
}
