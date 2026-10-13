package com.akhil.coordinationhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NodeIdentity {

    private final String nodeId;

    public NodeIdentity(@Value("${cluster.node-id}") String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("cluster.node-id must not be blank");
        }
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
