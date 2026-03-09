package com.mercora.paymentservice.kafka;

import com.mercora.paymentservice.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import payment.PaymentResponse;
import payment.PaymentStatus;

@Service
public class KafkaProducer {

  private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

  private final KafkaTemplate<String, byte[]> kafkaTemplate;

  public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publishResult(Payment payment, String correlationId) {
    String message = payment.getStatus() == com.mercora.paymentservice.model.PaymentStatus.APPROVED
            ? "PAYMENT_PROCESSED"
            : "PAYMENT_FAILED";

    PaymentStatus protoStatus = payment.getStatus() == com.mercora.paymentservice.model.PaymentStatus.APPROVED
            ? PaymentStatus.APPROVED
            : PaymentStatus.REJECTED;

    PaymentResponse event = PaymentResponse.newBuilder()
            .setPaymentId(payment.getId().toString())
            .setOrderId(payment.getOrderId())
            .setStatus(protoStatus)
            .setMessage(message)
            .setCorrelationId(correlationId)
            .build();

    try {
      kafkaTemplate.send("payment", event.toByteArray());
      log.info("Payment event published: [orderId={}, status={}, correlationId={}]",
              payment.getOrderId(), message, correlationId);
    } catch (Exception e) {
      log.error("Failed to publish payment event: [orderId={}, status={}, correlationId={}] error={}",
              payment.getOrderId(), message, correlationId, e.getMessage());
    }
  }
}
