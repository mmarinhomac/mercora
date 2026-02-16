package com.mercora.orderservice.service;

import com.mercora.orderservice.dto.OrderRequestDTO;
import com.mercora.orderservice.dto.OrderResponseDTO;
import com.mercora.orderservice.mapper.OrderMapper;
import com.mercora.orderservice.model.Order;
import com.mercora.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
  private final OrderRepository orderRepository;

  public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  public List<OrderResponseDTO> getOrders() {
    List<Order> orders = orderRepository.findAll();

    return orders.stream().map(OrderMapper::toDTO).toList();
  }

  public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
    Order newOrder = orderRepository.save(OrderMapper.toModel(orderRequestDTO));

    return OrderMapper.toDTO(newOrder);
  }
}
