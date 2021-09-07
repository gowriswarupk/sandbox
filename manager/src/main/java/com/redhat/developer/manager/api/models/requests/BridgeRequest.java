package com.redhat.developer.manager.api.models.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.developer.manager.models.Bridge;

public class BridgeRequest {

    @JsonProperty("name")
    private String name;

    public BridgeRequest() {
    }

    public BridgeRequest(String name) {
        this.name = name;
    }

    public Bridge toEntity() {
        Bridge bridge = new Bridge(name);
        return bridge;
    }

    public String getName() {
        return name;
    }
}