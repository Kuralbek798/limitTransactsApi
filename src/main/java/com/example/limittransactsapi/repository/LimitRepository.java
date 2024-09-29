package com.example.limittransactsapi.repository;

import com.example.limittransactsapi.Models.DTO.LimitAccountDTO;
import com.example.limittransactsapi.Models.Entity.Limit;
import com.example.limittransactsapi.repository.projections.LimitAccountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface LimitRepository extends JpaRepository<Limit, UUID> {

   // Optional<Limit> findTopByOrderByDatetimeDesc();
   Optional<Limit> findTopByClientIdAndIsActiveTrueOrderByDatetimeDesc(UUID id);


    @Procedure(procedureName  = "update_status_is_active")
    void updateStatusIsActive();

    default Optional<Limit> saveWithOptional(Limit limit) {
        return Optional.ofNullable(save(limit));}



    @Query(value = "SELECT " +
            "id AS id, " +
            "limit_sum AS limitSum," +
            "currency AS limitCurrency, " +
            "datetime AT TIME ZONE 'UTC' AS dateTime," +
            "client_id AS clientId, " +
            "is_base_limit AS isBaseLimit," +
            "is_active AS isActive, " +
            "account_number AS accountNumber " +
            "FROM public.get_latest_active_limits()",
            nativeQuery = true)
    List<LimitAccountProjection> findLatestActiveLimits();


}
