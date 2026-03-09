package com.mercora.orderservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mercora.orderservice.model.OrderStatus;
import com.mercora.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import payment.PaymentResponse;
import payment.PaymentStatus;

import java.util.UUID;

@Component
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  private final OrderRepository orderRepository;

  public KafkaConsumer(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  @KafkaListener(topics = "payment", groupId = "order-service-group")
  public void consumeEvent(byte[] payload) {
    try {
      PaymentResponse event = PaymentResponse.parseFrom(payload);
      String orderId = event.getOrderId();
      String correlationId = event.getCorrelationId();
      String eventType = event.getMessage();

      log.info("Payment event received: [orderId={}, eventType={}, correlationId={}]",
              orderId, eventType, correlationId);

      orderRepository.findById(UUID.fromString(orderId)).ifPresentOrElse(order -> {
        if (event.getStatus() == PaymentStatus.APPROVED) {
          order.setStatus(OrderStatus.PAID);
          order.setPaymentId(UUID.fromString(event.getPaymentId()));
        } else if (event.getStatus() == PaymentStatus.REJECTED) {
          order.setStatus(OrderStatus.FAILED);
        }
        orderRepository.save(order);
        log.info("Order updated: [orderId={}, status={}, correlationId={}]",
                orderId, order.getStatus(), correlationId);
      }, () -> log.warn("Order not found for orderId={}", orderId));

    } catch (InvalidProtocolBufferException e) {
      log.error("Failed to deserialize payment event: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Error processing payment event: {}", e.getMessage());
    }
  }
}
