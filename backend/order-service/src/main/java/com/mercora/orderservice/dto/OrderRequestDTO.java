package com.mercora.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class OrderRequestDTO {
  @NotBlank(message = "customerId is required")
  private String customerId;

  @NotNull
  @DecimalMin(value = "0.01", message = "amount must be greater than 0")
  private BigDecimal amount;

  @NotBlank(message = "currency is required")
  @Size(max = 3, message = "currency must be 3 characters")
  private String currency;

  @NotBlank(message = "paymentMethod is required")
  private String paymentMethod;

  private String correlationId;

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }
}
