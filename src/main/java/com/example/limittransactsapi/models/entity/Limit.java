package com.example.limittransactsapi.models.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor

@NoArgsConstructor
@Table(name = "limits")

public class Limit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "limit_sum", nullable = false)
    private BigDecimal limitSum;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "datetime", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime datetime;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "is_base_limit")
    private boolean baseLimit;

    @Column(name = "is_active")
    private boolean active = true;

    public UUID getId() {
        return this.id;
    }

    public BigDecimal getLimitSum() {
        return this.limitSum;
    }

    public String getCurrency() {
        return this.currency;
    }

    public OffsetDateTime getDatetime() {
        return this.datetime;
    }

    public UUID getClientId() {
        return this.clientId;
    }

    public boolean isBaseLimit() {
        return this.baseLimit;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setLimitSum(BigDecimal limitSum) {
        this.limitSum = limitSum;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setDatetime(OffsetDateTime datetime) {
        this.datetime = datetime;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public void setBaseLimit(boolean baseLimit) {
        this.baseLimit = baseLimit;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Limit)) return false;
        final Limit other = (Limit) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$limitSum = this.getLimitSum();
        final Object other$limitSum = other.getLimitSum();
        if (this$limitSum == null ? other$limitSum != null : !this$limitSum.equals(other$limitSum)) return false;
        final Object this$currency = this.getCurrency();
        final Object other$currency = other.getCurrency();
        if (this$currency == null ? other$currency != null : !this$currency.equals(other$currency)) return false;
        final Object this$datetime = this.getDatetime();
        final Object other$datetime = other.getDatetime();
        if (this$datetime == null ? other$datetime != null : !this$datetime.equals(other$datetime)) return false;
        final Object this$clientId = this.getClientId();
        final Object other$clientId = other.getClientId();
        if (this$clientId == null ? other$clientId != null : !this$clientId.equals(other$clientId)) return false;
        if (this.isBaseLimit() != other.isBaseLimit()) return false;
        if (this.isActive() != other.isActive()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Limit;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $limitSum = this.getLimitSum();
        result = result * PRIME + ($limitSum == null ? 43 : $limitSum.hashCode());
        final Object $currency = this.getCurrency();
        result = result * PRIME + ($currency == null ? 43 : $currency.hashCode());
        final Object $datetime = this.getDatetime();
        result = result * PRIME + ($datetime == null ? 43 : $datetime.hashCode());
        final Object $clientId = this.getClientId();
        result = result * PRIME + ($clientId == null ? 43 : $clientId.hashCode());
        result = result * PRIME + (this.isBaseLimit() ? 79 : 97);
        result = result * PRIME + (this.isActive() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "Limit(id=" + this.getId() + ", limitSum=" + this.getLimitSum() + ", currency=" + this.getCurrency() + ", datetime=" + this.getDatetime() + ", clientId=" + this.getClientId() + ", baseLimit=" + this.isBaseLimit() + ", active=" + this.isActive() + ")";
    }
}
