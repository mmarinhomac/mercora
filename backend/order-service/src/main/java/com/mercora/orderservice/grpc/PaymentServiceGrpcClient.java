package com.mercora.orderservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import payment.Money;
import payment.PaymentRequest;
import payment.PaymentResponse;
import payment.PaymentServiceGrpc;

import java.math.BigDecimal;

@Component
public class PaymentServiceGrpcClient {
  private static final Logger log = LoggerFactory.getLogger(PaymentServiceGrpcClient.class);

  private final PaymentServiceGrpc.PaymentServiceBlockingStub blockingStub;

  public PaymentServiceGrpcClient(
          @Value("${payment.service.address:localhost}") String serverAddress,
          @Value("${payment.service.grpc.port:9001}") int serverPort) {

    log.info("Connecting to payment service gRPC server at {}:{}", serverAddress, serverPort);

    ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
            .usePlaintext()
            .build();

    blockingStub = PaymentServiceGrpc.newBlockingStub(channel);
  }

  public PaymentResponse sendPendingPayment(String orderId, BigDecimal amount, String currency, String paymentMethod) {
    Money money = Money.newBuilder()
            .setUnits(amount.longValue())
            .setNanos(amount.remainder(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(1_000_000_000))
                    .abs()
                    .intValue())
            .setCurrency(currency)
            .build();

    PaymentRequest request = PaymentRequest.newBuilder()
            .setOrderId(orderId)
            .setAmount(money)
            .setPaymentMethod(paymentMethod)
            .build();

    log.info("Sending processPayment gRPC request for orderId: {}:{}", orderId, request);

    PaymentResponse response = blockingStub.processPayment(request);

    log.info("Received payment response via gRPC: paymentId={}, status={}", response.getPaymentId(), response.getStatus());

    return response;
  }
}
