package com.mercora.inventoryservice.grpc;

import com.mercora.inventoryservice.reservation.ReservationStore;
import inventory.InventoryServiceGrpc;
import inventory.ReserveRequest;
import inventory.ReserveResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(InventoryGrpcService.class);

    private final ReservationStore reservationStore;

    public InventoryGrpcService(ReservationStore reservationStore) {
        this.reservationStore = reservationStore;
    }

    @Override
    public void reserve(ReserveRequest request, StreamObserver<ReserveResponse> responseObserver) {
        try {
            String orderId = request.getOrderId();
            log.info("Reserve request received: [orderId={}]", orderId);

            String reservationId = reservationStore.reserve(orderId);

            ReserveResponse response = ReserveResponse.newBuilder()
                    .setReservationId(reservationId)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Reserve response sent: [orderId={}, reservationId={}]", orderId, reservationId);
        } catch (Exception e) {
            log.error("Error processing reserve request: {}", e.getMessage());
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
