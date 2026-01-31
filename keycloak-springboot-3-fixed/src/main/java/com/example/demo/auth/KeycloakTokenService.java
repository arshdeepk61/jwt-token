package com.example.demo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class KeycloakTokenService {

  @Value("${keycloak.token-url}")
  private String tokenUrl;

  @Value("${keycloak.client-id}")
  private String clientId;

  @Value("${keycloak.client-secret}")
  private String clientSecret;

  private final RestTemplate restTemplate = new RestTemplate();

  public String getAccessToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "client_credentials");
    body.add("client_id", clientId);
    body.add("client_secret", clientSecret);

    HttpEntity<MultiValueMap<String, String>> request =
        new HttpEntity<>(body, headers);

    ResponseEntity<Map> response =
        restTemplate.postForEntity(tokenUrl, request, Map.class);

    return response.getBody().get("access_token").toString();
  }
}
