package com.example.limittransactsapi.Models.DTO;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@SuppressWarnings("unused")
@NoArgsConstructor
public class PathForApiDTO {
    private UUID id;
    private String apiPath;

    private String description;

    private OffsetDateTime dateTime;


}
