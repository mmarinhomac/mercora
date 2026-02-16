package com.mercora.orderservice.controller;

import com.mercora.orderservice.dto.OrderRequestDTO;
import com.mercora.orderservice.dto.OrderResponseDTO;
import com.mercora.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  public ResponseEntity<List<OrderResponseDTO>> getOrders() {
    List<OrderResponseDTO> orders = orderService.getOrders();
    return ResponseEntity.ok().body(orders);
  }

  @PostMapping
  public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO orderRequestDTO) {
    OrderResponseDTO orderResponseDTO = orderService.createOrder(orderRequestDTO);

    return ResponseEntity.ok().body(orderResponseDTO);
  }
}
