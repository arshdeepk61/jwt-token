package com.example.demo.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProtectedController {

  @GetMapping("/api/protected")
  public String protectedApi(
      @RequestHeader(value = "Authorization", required = false) String auth) {

    if (auth == null || !auth.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
    }

    return "Protected API accessed successfully";
  }
}
