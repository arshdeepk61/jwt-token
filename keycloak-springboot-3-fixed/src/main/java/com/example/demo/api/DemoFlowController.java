package com.example.demo.api;

import com.example.demo.auth.KeycloakTokenService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class DemoFlowController {

  private final KeycloakTokenService tokenService;
  private final RestTemplate restTemplate = new RestTemplate();

  public DemoFlowController(KeycloakTokenService tokenService) {
    this.tokenService = tokenService;
  }

  @GetMapping("/demo")
  public String demo() {
    String token = tokenService.getAccessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(
        "http://localhost:8081/api/protected",
        HttpMethod.GET,
        entity,
        String.class);

    return response.getBody();
  }
}
