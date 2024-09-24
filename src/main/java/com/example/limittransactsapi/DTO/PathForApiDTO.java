package com.example.limittransactsapi.DTO;


import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@SuppressWarnings("unused")
@NoArgsConstructor
public class PathForApiDTO {

    private String apiPath;

    private String description;

    private OffsetDateTime dateTime;

    public String getApiPath() {
        return this.apiPath;
    }

    public String getDescription() {
        return this.description;
    }

    public OffsetDateTime getDateTime() {
        return this.dateTime;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDateTime(OffsetDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PathForApiDTO)) return false;
        final PathForApiDTO other = (PathForApiDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$apiPath = this.getApiPath();
        final Object other$apiPath = other.getApiPath();
        if (this$apiPath == null ? other$apiPath != null : !this$apiPath.equals(other$apiPath)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final Object this$dateTime = this.getDateTime();
        final Object other$dateTime = other.getDateTime();
        if (this$dateTime == null ? other$dateTime != null : !this$dateTime.equals(other$dateTime)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof PathForApiDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $apiPath = this.getApiPath();
        result = result * PRIME + ($apiPath == null ? 43 : $apiPath.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $dateTime = this.getDateTime();
        result = result * PRIME + ($dateTime == null ? 43 : $dateTime.hashCode());
        return result;
    }

    public String toString() {
        return "PathForApiDTO(apiPath=" + this.getApiPath() + ", description=" + this.getDescription() + ", dateTime=" + this.getDateTime() + ")";
    }
}
