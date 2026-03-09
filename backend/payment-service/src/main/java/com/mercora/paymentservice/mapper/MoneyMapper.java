package com.mercora.paymentservice.mapper;

import order.events.Money;

import java.math.BigDecimal;

public class MoneyMapper {

  private MoneyMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static BigDecimal toBigDecimal(Money money) {
    return BigDecimal.valueOf(money.getUnits())
            .add(BigDecimal.valueOf(money.getNanos(), 9));
  }
}

