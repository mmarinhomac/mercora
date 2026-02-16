package com.mercora.orderservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class OrderRequestDTO {
  @NotBlank(message = "customerId is required")
  private String customerId;

  @NotBlank(message = "status is required")
  @Size(max = 20, message = "status must be less than 20 characters")
  private String status;

  @NotNull
  @DecimalMin(value = "0.01", message = "total must be greater than 0")
  @Positive(message = "total must be positive")
  private BigDecimal total;

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }
}
