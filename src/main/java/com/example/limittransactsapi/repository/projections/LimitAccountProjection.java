package com.example.limittransactsapi.repository.projections;

import org.hibernate.type.descriptor.jdbc.TimeAsTimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface LimitAccountProjection {
    UUID getId();
    BigDecimal getLimitSum();
    String getCurrency();
    Timestamp  getDatetime();
    UUID getClientId();
    Boolean getIsBaseLimit();
    Boolean getIsActive();
    Integer getAccountNumber();
}
