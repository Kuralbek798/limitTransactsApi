package com.example.limittransactsapi.DTO;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckedOnLimitDTO {
    private UUID id;
    private UUID transactionId;
    private UUID limitId;
    private boolean limitExceeded;
    private OffsetDateTime checkedOnLimitDatetime;
}
