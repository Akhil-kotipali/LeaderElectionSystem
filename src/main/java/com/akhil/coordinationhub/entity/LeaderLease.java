
package com.akhil.coordinationhub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "leader_lease")
public class LeaderLease {

    @Id
    private Integer id;

    @Column(name = "leader_node_id", length = 100)
    private String leaderNodeId;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(nullable = false)
    private long epoch;

    protected LeaderLease() {}

    public Integer getId() {
        return id;
    }

    public String getLeaderNodeId() {
        return leaderNodeId;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public long getEpoch() {
        return epoch;
    }

    public boolean isLeaseActive(Instant now) {
        return leaderNodeId != null
                && leaseUntil != null
                && leaseUntil.isAfter(now);
    }

    public void claim(String nodeId, Instant expiry) {
        this.leaderNodeId = nodeId;
        this.leaseUntil = expiry;
        this.epoch++;
    }

    public void renew(Instant expiry) {
        this.leaseUntil = expiry;
    }
}
