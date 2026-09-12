package com.routemate.backend.trip.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TripMessageDto {
    private Long id;
    private UUID senderId;
    private String senderName;
    private String content;
    private Instant createdAt;
}
