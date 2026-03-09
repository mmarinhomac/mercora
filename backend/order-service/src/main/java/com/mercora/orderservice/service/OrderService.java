package com.mercora.orderservice.service;

import com.mercora.orderservice.dto.OrderRequestDTO;
import com.mercora.orderservice.dto.OrderResponseDTO;
import com.mercora.orderservice.grpc.InventoryServiceGrpcClient;
import com.mercora.orderservice.kafka.KafkaProducer;
import com.mercora.orderservice.mapper.OrderMapper;
import com.mercora.orderservice.model.Order;
import com.mercora.orderservice.model.OrderStatus;
import com.mercora.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
  private final OrderRepository orderRepository;
  private final InventoryServiceGrpcClient inventoryServiceGrpcClient;
  private final KafkaProducer kafkaProducer;

  public OrderService(OrderRepository orderRepository, InventoryServiceGrpcClient inventoryServiceGrpcClient, KafkaProducer kafkaProducer) {
    this.orderRepository = orderRepository;
    this.inventoryServiceGrpcClient = inventoryServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }

  public List<OrderResponseDTO> getOrders() {
    return orderRepository.findAll().stream().map(OrderMapper::toDTO).toList();
  }

  public OrderResponseDTO createOrder(OrderRequestDTO request) {
    Order order = OrderMapper.toModel(request);
    Order saved = orderRepository.save(order);

    String reservationId = inventoryServiceGrpcClient.reserveStock(
            saved.getId().toString(),
            saved.getTotalAmount(),
            saved.getCurrency()
    );

    saved.setReservationId(UUID.fromString(reservationId));
    saved.setStatus(OrderStatus.PAYMENT_PENDING);
    Order persisted = orderRepository.save(saved);

    kafkaProducer.sendEvent(persisted);

    return OrderMapper.toDTO(persisted);
  }
}
