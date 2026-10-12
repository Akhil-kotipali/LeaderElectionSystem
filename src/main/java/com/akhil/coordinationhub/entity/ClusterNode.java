
package com.akhil.coordinationhub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cluster_node")
public class ClusterNode {

    @Id
    @Column(name = "node_id", length = 100, nullable = false)
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NodeStatus status;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    protected ClusterNode() {}

    public ClusterNode(String nodeId, Instant startedAt) {
        this.nodeId = nodeId;
        this.startedAt = startedAt;
        this.status = NodeStatus.ACTIVE;
        this.lastHeartbeat = startedAt;
    }

    public String getNodeId() {
        return nodeId;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void recordHeartbeat(Instant heartbeatTime) {
        this.lastHeartbeat = heartbeatTime;
        this.status = NodeStatus.ACTIVE;
    }

    public void markInactive() {
        this.status = NodeStatus.INACTIVE;
    }
}
