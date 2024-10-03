package com.example.limittransactsapi.repository;

import com.example.limittransactsapi.models.entity.PathForApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface PathForApiRepository extends JpaRepository<PathForApi, UUID> {

    PathForApi findByDescription(String description);
}
