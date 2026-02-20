package com.mercora.paymentservice.grpc;

import com.mercora.paymentservice.mapper.MoneyMapper;
import com.mercora.paymentservice.model.Payment;
import com.mercora.paymentservice.repository.PaymentRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import payment.PaymentRequest;
import payment.PaymentResponse;
import payment.PaymentServiceGrpc.PaymentServiceImplBase;
import payment.PaymentStatus;

@GrpcService
public class PaymentGrpcService extends PaymentServiceImplBase {
  private static final Logger log = LoggerFactory.getLogger(PaymentGrpcService.class);

  private final PaymentRepository paymentRepository;

  public PaymentGrpcService(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Override
  public void processPayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
    log.info("processPayment request received for orderId: {}", request.getOrderId());

    Payment payment = new Payment();
    payment.setOrderId(request.getOrderId());
    payment.setAmount(MoneyMapper.toBigDecimal(request.getAmount()));
    payment.setCurrency(request.getAmount().getCurrency());
    payment.setPaymentMethod(request.getPaymentMethod());
    payment.setStatus(com.mercora.paymentservice.model.PaymentStatus.PENDING);

    Payment saved = paymentRepository.save(payment);

    PaymentResponse response = PaymentResponse.newBuilder()
            .setPaymentId(saved.getId().toString())
            .setOrderId(saved.getOrderId())
            .setStatus(PaymentStatus.PENDING)
            .setMessage("Payment record created with status PENDING")
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
