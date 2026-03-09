package com.mercora.inventoryservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mercora.inventoryservice.reservation.ReservationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import payment.PaymentResponse;
import payment.PaymentStatus;

@Service
public class KafkaConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  private final ReservationStore reservationStore;

  public KafkaConsumer(ReservationStore reservationStore) {
    this.reservationStore = reservationStore;
  }

  @KafkaListener(topics = "payment", groupId = "inventory-service-group")
  public void consumeEvent(byte[] event) {
    try {
      PaymentResponse paymentResponse = PaymentResponse.parseFrom(event);
      String orderId = paymentResponse.getOrderId();
      String paymentId = paymentResponse.getPaymentId();
      PaymentStatus status = paymentResponse.getStatus();

      log.info("Received Payment Event: [orderId={}, paymentId={}, status={}]", orderId, paymentId, status);

      reservationStore.findByOrderId(orderId).ifPresentOrElse(
              reservationId -> {
                if (status == PaymentStatus.APPROVED) {
                  reservationStore.confirm(reservationId);
                } else if (status == PaymentStatus.REJECTED) {
                  reservationStore.release(reservationId);
                }
              },
              () -> log.warn("No reservation found for orderId={}", orderId)
      );
    } catch (InvalidProtocolBufferException e) {
      log.error("Error deserializing PaymentResponse: {}", e.getMessage());
    }
  }
}
