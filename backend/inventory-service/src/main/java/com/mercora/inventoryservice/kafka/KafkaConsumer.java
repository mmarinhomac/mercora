package com.mercora.inventoryservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import order.events.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {
  private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

  @KafkaListener(topics = "order", groupId = "inventory-service-group")
  public void consumeEvent(byte[] event) {
    try {
      OrderEvent orderEvent = OrderEvent.parseFrom(event);
      // perform any business logic with the orderEvent, e.g. update inventory

      log.info("Receive Order Event: [OrderId={}, OrderAmount={}, OrderStatus={}]",
              orderEvent.getOrderId(), orderEvent.getAmount(), orderEvent.getStatus());
    } catch (InvalidProtocolBufferException e) {
      log.error("Error deserializing OrderEvent: {}", e.getMessage());
    }
  }
}
