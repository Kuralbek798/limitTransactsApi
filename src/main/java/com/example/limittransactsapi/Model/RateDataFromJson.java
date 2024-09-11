package com.example.limittransactsapi.Model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RateDataFromJson {
    private Double closeRate;
    private OffsetDateTime date;
}
