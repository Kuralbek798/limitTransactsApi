package com.example.limittransactsapi.models.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "path_for_api")
public class PathForApi {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "encrypted_api_path", nullable = false)
    private String encryptedApiPath;

    @Column(name = "datetime", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime datetime;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

}
