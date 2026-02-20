package com.mercora.paymentservice.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import payment.PaymentRequest;
import payment.PaymentResponse;
import payment.PaymentServiceGrpc.PaymentServiceImplBase;

@GrpcService
public class PaymentGrpcService extends PaymentServiceImplBase {
  private static final Logger log = LoggerFactory.getLogger(PaymentGrpcService.class);

  @Override
  public void createPayment(PaymentRequest paymentRequest,
                            StreamObserver<PaymentResponse> responseObserver) {

    log.info("createPayment request received: {}", paymentRequest);

    // Business Logic - e.g., validate request, process payment, etc.

    PaymentResponse response = PaymentResponse.newBuilder()
            .setPaymentId("12345")
            .setStatus("ACTIVE")
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
