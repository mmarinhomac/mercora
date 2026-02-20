package com.mercora.orderservice.mapper;

import com.mercora.orderservice.dto.OrderRequestDTO;
import com.mercora.orderservice.dto.OrderResponseDTO;
import com.mercora.orderservice.model.Order;
import com.mercora.orderservice.model.OrderStatus;

public class OrderMapper {

  private OrderMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static OrderResponseDTO toDTO(Order order) {
    OrderResponseDTO dto = new OrderResponseDTO();
    dto.setOrderId(order.getId().toString());
    dto.setStatus(order.getStatus().name());
    dto.setPaymentId(order.getPaymentId() != null ? order.getPaymentId().toString() : null);
    return dto;
  }

  public static Order toModel(OrderRequestDTO dto) {
    Order order = new Order();
    order.setCustomerId(dto.getCustomerId());
    order.setTotalAmount(dto.getAmount());
    order.setCurrency(dto.getCurrency());
    order.setPaymentMethod(dto.getPaymentMethod());
    order.setStatus(OrderStatus.CREATED);
    return order;
  }
}
