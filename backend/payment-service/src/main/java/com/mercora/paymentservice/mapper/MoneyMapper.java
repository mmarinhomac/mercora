package com.mercora.paymentservice.mapper;

import payment.Money;

import java.math.BigDecimal;

public class MoneyMapper {

  private MoneyMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static BigDecimal toBigDecimal(Money money) {
    return BigDecimal.valueOf(money.getUnits())
            .add(BigDecimal.valueOf(money.getNanos(), 9));
  }

  public static Money toMoney(BigDecimal amount, String currency) {
    long units = amount.longValue();
    int nanos = amount.remainder(BigDecimal.ONE)
            .multiply(BigDecimal.valueOf(1_000_000_000))
            .abs()
            .intValue();

    return Money.newBuilder()
            .setUnits(units)
            .setNanos(nanos)
            .setCurrency(currency)
            .build();
  }
}

