package com.mercora.orderservice.kafka;

import com.mercora.orderservice.model.Order;
import order.events.Money;
import order.events.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class KafkaProducer {
  private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

  private final KafkaTemplate<String, byte[]> kafkaTemplate;

  public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendEvent(Order order) {
    Money money = Money.newBuilder()
            .setUnits(order.getTotalAmount().longValue())
            .setNanos(order.getTotalAmount().remainder(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(1_000_000_000))
                    .abs()
                    .intValue())
            .setCurrency(order.getCurrency())
            .build();

    OrderEvent event = OrderEvent.newBuilder()
            .setOrderId(order.getId().toString())
            .setAmount(money)
            .setStatus(order.getStatus().name())
            .setEventType("ORDER_CREATED")
            .build();

    try {
      kafkaTemplate.send("order", event.toByteArray());
      log.info("OrderEvent sent to Kafka for orderId: {}", order.getId());
    } catch (Exception e) {
      log.error("Failed on sendEvent KafkaProducer: {}", event);
    }
  }
}
