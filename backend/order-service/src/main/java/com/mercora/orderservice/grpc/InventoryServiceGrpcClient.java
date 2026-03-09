package com.mercora.orderservice.grpc;

import inventory.InventoryServiceGrpc;
import inventory.Money;
import inventory.ReserveRequest;
import inventory.ReserveResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventoryServiceGrpcClient {
  private static final Logger log = LoggerFactory.getLogger(InventoryServiceGrpcClient.class);

  private final InventoryServiceGrpc.InventoryServiceBlockingStub blockingStub;

  public InventoryServiceGrpcClient(
          @Value("${inventory.service.address:localhost}") String serverAddress,
          @Value("${inventory.service.grpc.port:9001}") int serverPort) {

    log.info("Connecting to inventory service gRPC server at {}:{}", serverAddress, serverPort);

    ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
            .usePlaintext()
            .build();

    blockingStub = InventoryServiceGrpc.newBlockingStub(channel);
  }

  public String reserveStock(String orderId, BigDecimal amount, String currency) {
    Money money = Money.newBuilder()
            .setUnits(amount.longValue())
            .setNanos(amount.remainder(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(1_000_000_000))
                    .abs()
                    .intValue())
            .setCurrency(currency)
            .build();

    ReserveRequest request = ReserveRequest.newBuilder()
            .setOrderId(orderId)
            .setAmount(money)
            .build();

    log.info("Sending Reserve gRPC request for orderId: {}", orderId);

    ReserveResponse response = blockingStub.reserve(request);

    log.info("Received reservationId={} for orderId={}", response.getReservationId(), orderId);

    return response.getReservationId();
  }
}
