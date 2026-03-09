package com.mercora.paymentservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mercora.paymentservice.mapper.MoneyMapper;
import com.mercora.paymentservice.model.Payment;
import com.mercora.paymentservice.model.PaymentStatus;
import com.mercora.paymentservice.repository.PaymentRepository;
import order.events.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  private final PaymentRepository paymentRepository;
  private final KafkaProducer kafkaProducer;

  public KafkaConsumer(PaymentRepository paymentRepository, KafkaProducer kafkaProducer) {
    this.paymentRepository = paymentRepository;
    this.kafkaProducer = kafkaProducer;
  }

  @KafkaListener(topics = "order", groupId = "payment-service-group")
  public void consumeEvent(byte[] payload) {
    String orderId = null;
    try {
      OrderEvent event = OrderEvent.parseFrom(payload);
      orderId = event.getOrderId();
      String correlationId = event.getCorrelationId();

      log.info("ORDER_CREATED event received: [orderId={}, correlationId={}]", orderId, correlationId);

      var existing = paymentRepository.findByOrderId(orderId);
      if (existing.isPresent()) {
        log.info("Payment already processed for orderId={}, status={}, skipping",
                orderId, existing.get().getStatus());
        return;
      }

      BigDecimal amount = MoneyMapper.toBigDecimal(event.getAmount());

      Payment payment = new Payment();
      payment.setOrderId(orderId);
      payment.setAmount(amount);
      payment.setCurrency(event.getAmount().getCurrency());
      payment.setPaymentMethod(event.getPaymentMethod());
      payment.setStatus(PaymentStatus.PENDING);

      if (amount.compareTo(new BigDecimal("10.00")) == 0) {
        payment.setStatus(PaymentStatus.REJECTED);
      } else {
        payment.setStatus(PaymentStatus.APPROVED);
      }

      Payment savedPayment = paymentRepository.save(payment);

      kafkaProducer.publishResult(savedPayment, correlationId);

      log.info("Payment processed: [orderId={}, status={}, correlationId={}]",
              orderId, savedPayment.getStatus(), correlationId);

    } catch (InvalidProtocolBufferException e) {
      log.error("Failed to deserialize ORDER_CREATED event: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Error processing ORDER_CREATED event: [orderId={}] error={}", orderId, e.getMessage());
    }
  }
}
