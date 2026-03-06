package com.mercora.systemtests;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;


class OrderIntegrationTest {
  @BeforeAll
  static void setUp() {
    RestAssured.baseURI = "http://localhost:4000";
  }

  @Test
  void shouldReturnOrdersWithValidToken() {
    String loginPayload = """
              {
                "email": "testuser@test.com",
                "password": "password123"
              }
            """;

    String token = given()
            .contentType("application/json")
            .body(loginPayload)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .get("token");

    given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/orders")
            .then()
            .statusCode(200)
            .body("$", not(empty()));
  }
}
