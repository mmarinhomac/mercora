package com.mercora.orderservice.service;

import com.mercora.orderservice.dto.OrderRequestDTO;
import com.mercora.orderservice.dto.OrderResponseDTO;
import com.mercora.orderservice.grpc.PaymentServiceGrpcClient;
import com.mercora.orderservice.kafka.KafkaProducer;
import com.mercora.orderservice.mapper.OrderMapper;
import com.mercora.orderservice.model.Order;
import com.mercora.orderservice.model.OrderStatus;
import com.mercora.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import payment.PaymentResponse;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
  private final OrderRepository orderRepository;
  private final PaymentServiceGrpcClient paymentServiceGrpcClient;
  private final KafkaProducer kafkaProducer;

  public OrderService(OrderRepository orderRepository, PaymentServiceGrpcClient paymentServiceGrpcClient, KafkaProducer kafkaProducer) {
    this.orderRepository = orderRepository;
    this.paymentServiceGrpcClient = paymentServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }

  public List<OrderResponseDTO> getOrders() {
    return orderRepository.findAll().stream().map(OrderMapper::toDTO).toList();
  }

  public OrderResponseDTO createOrder(OrderRequestDTO request) {
    Order order = OrderMapper.toModel(request);
    Order saved = orderRepository.save(order);

    // TODO: publish OrderCreatedEvent to Kafka
    PaymentResponse paymentResponse = paymentServiceGrpcClient.sendPendingPayment(
            saved.getId().toString(),
            saved.getTotalAmount(),
            saved.getCurrency(),
            saved.getPaymentMethod()
    );

    saved.setStatus(OrderStatus.PAYMENT_PENDING);
    saved.setPaymentId(UUID.fromString(paymentResponse.getPaymentId()));
    orderRepository.save(saved);

    kafkaProducer.sendEvent(saved);

    return OrderMapper.toDTO(saved);
  }
}
