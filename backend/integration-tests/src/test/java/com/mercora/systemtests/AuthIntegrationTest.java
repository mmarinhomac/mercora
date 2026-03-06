package com.mercora.systemtests;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class AuthIntegrationTest {

  @BeforeAll
  static void setUp() {
    RestAssured.baseURI = "http://localhost:4000";
  }

  @Test
  void shouldReturnOKWithValidToken() {
    String loginPayload = """
              {
                "email": "testuser@test.com",
                "password": "password123"
              }
            """;

    Response response = given()
            .contentType("application/json")
            .body(loginPayload)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .extract()
            .response();

    System.out.println("Generated Token: " + response.jsonPath().getString("token"));
  }

  @Test
  void shouldReturnUnauthorizedOnInvalidLogin() {
    String loginPayload = """
              {
                "email": "invalid_user@test.com",
                "password": "wrongpassword"
              }
            """;

    given()
            .contentType("application/json")
            .body(loginPayload)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(401);
  }
}
