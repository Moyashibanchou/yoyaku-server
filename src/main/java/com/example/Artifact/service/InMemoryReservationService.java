package com.example.Artifact.service;

import com.example.Artifact.dto.ReservationRequest;
import com.example.Artifact.model.Reservation;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryReservationService implements ReservationService {
    private final ConcurrentHashMap<String, Reservation> store = new ConcurrentHashMap<>();

    @Override
    public String createReservation(ReservationRequest request) {
        String id = UUID.randomUUID().toString();

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setUserId(request.getUserId());
        reservation.setName(request.getName());
        reservation.setMenu(request.getMenu());
        reservation.setDateTime(request.getDateTime());
        reservation.setStaff(request.getStaff());
        reservation.setUseCoupon(request.getUseCoupon());

        store.put(id, reservation);
        return id;
    }

    @Override
    public Reservation getById(String id) {
        return store.get(id);
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }
}

