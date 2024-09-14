package com.example.limittransactsapi.DTO;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@SuppressWarnings("unused")
@NoArgsConstructor
public class PathForApiDTO {

    private String apiPath;

    private String description;

    private OffsetDateTime dateTime;
}
