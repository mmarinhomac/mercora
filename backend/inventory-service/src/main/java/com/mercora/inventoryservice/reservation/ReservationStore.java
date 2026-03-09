package com.mercora.inventoryservice.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReservationStore {

    private static final Logger log = LoggerFactory.getLogger(ReservationStore.class);

    private final ConcurrentHashMap<String, String> reservations = new ConcurrentHashMap<>();

    public String reserve(String orderId) {
        String reservationId = UUID.randomUUID().toString();
        reservations.put(reservationId, orderId);
        log.info("Reservation created: [orderId={}, reservationId={}]", orderId, reservationId);
        return reservationId;
    }

    public void confirm(String reservationId) {
        String removed = reservations.remove(reservationId);
        if (removed == null) {
            log.warn("Confirm: reservationId not found [reservationId={}]", reservationId);
            return;
        }
        log.info("Reservation confirmed: [reservationId={}]", reservationId);
    }

    public void release(String reservationId) {
        String removed = reservations.remove(reservationId);
        if (removed == null) {
            log.warn("Release: reservationId not found [reservationId={}]", reservationId);
            return;
        }
        log.info("Reservation released: [reservationId={}]", reservationId);
    }

    public Optional<String> findByOrderId(String orderId) {
        return reservations.entrySet().stream()
                .filter(e -> orderId.equals(e.getValue()))
                .map(java.util.Map.Entry::getKey)
                .findFirst();
    }
}
