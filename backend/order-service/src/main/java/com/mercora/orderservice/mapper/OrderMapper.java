package com.mercora.orderservice.mapper;

import com.mercora.orderservice.dto.OrderRequestDTO;
import com.mercora.orderservice.dto.OrderResponseDTO;
import com.mercora.orderservice.model.Order;

public class OrderMapper {
  private OrderMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static OrderResponseDTO toDTO(Order order) {
    OrderResponseDTO orderDTO = new OrderResponseDTO();
    orderDTO.setId(order.getId().toString());
    orderDTO.setCustomerId(order.getCustomerId());
    orderDTO.setStatus(order.getStatus());
    orderDTO.setTotal(order.getTotal());
    return orderDTO;
  }

  public static Order toModel(OrderRequestDTO orderRequestDTO) {
    Order order = new Order();
    order.setCustomerId(orderRequestDTO.getCustomerId());
    order.setStatus(orderRequestDTO.getStatus());
    order.setTotal(orderRequestDTO.getTotal());
    return order;
  }
}
