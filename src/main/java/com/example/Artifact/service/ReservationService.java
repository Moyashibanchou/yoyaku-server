package com.example.Artifact.service;

import com.example.Artifact.dto.ReservationRequest;
import com.example.Artifact.model.Reservation;

public interface ReservationService {
    String createReservation(ReservationRequest request);

    Reservation getById(String id);

    boolean deleteById(String id);
}

