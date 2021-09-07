package com.redhat.developer.manager.api.models.responses;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.developer.infra.dto.BridgeStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BridgeResponse {

    @JsonProperty("kind")
    private final String kind = "Bridge";

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("href")
    private String href; // TODO: implement bridge endpoint and set href to endpoint

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    @JsonProperty("submitted_at")
    private ZonedDateTime submittedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    @JsonProperty("published_at")
    private ZonedDateTime publishedAt;

    @JsonProperty("status")
    private BridgeStatus status;

    @JsonProperty("endpoint")
    private String endpoint;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHref() {
        return href;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public BridgeStatus getStatus() {
        return status;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}