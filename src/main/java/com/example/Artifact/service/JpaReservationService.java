package com.example.Artifact.service;

import com.example.Artifact.dto.ReservationRequest;
import com.example.Artifact.model.Reservation;
import com.example.Artifact.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JpaReservationService implements ReservationService {
    private final ReservationRepository reservationRepository;

    public JpaReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public String createReservation(ReservationRequest request) {
        String id = UUID.randomUUID().toString();

        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setUserId(request.getUserId());
        reservation.setUserName(request.getUserName());
        reservation.setUserPhone(request.getUserPhone());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setReservationTime(request.getReservationTime());
        reservation.setAssistantName(request.getAssistantName());
        reservation.setMenuName(request.getMenuName());
        reservation.setCouponName(request.getCouponName());
        reservation.setStatus("ACTIVE");
        reservation.setCreatedAt(Instant.now());

        reservationRepository.save(reservation);
        return id;
    }

    @Override
    public Reservation getById(String id) {
        Optional<Reservation> reservation = reservationRepository.findById(id);
        return reservation.orElse(null);
    }

    @Override
    public boolean deleteById(String id) {
        if (!reservationRepository.existsById(id)) {
            return false;
        }
        reservationRepository.deleteById(id);
        return true;
    }
}
