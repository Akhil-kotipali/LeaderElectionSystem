
package com.akhil.coordinationhub.service;

import com.akhil.coordinationhub.dto.LeaderResponse;
import com.akhil.coordinationhub.entity.ClusterNode;
import com.akhil.coordinationhub.entity.LeaderLease;
import com.akhil.coordinationhub.entity.NodeStatus;
import com.akhil.coordinationhub.repository.ClusterNodeRepository;
import com.akhil.coordinationhub.repository.LeaderLeaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
public class LeaderElectionService {

    private static final int LEADER_ROW_ID = 1;
    private static final Duration LEASE_DURATION =
            Duration.ofSeconds(10);

    private final LeaderLeaseRepository leaderRepository;
    private final ClusterNodeRepository nodeRepository;

    public LeaderElectionService(
            LeaderLeaseRepository leaderRepository,
            ClusterNodeRepository nodeRepository) {
        this.leaderRepository = leaderRepository;
        this.nodeRepository = nodeRepository;
    }

    @Transactional
    public LeaderResponse claim(String nodeId) {
        requireActiveNode(nodeId);

        LeaderLease lease = lockLeaderRow();
        Instant now = Instant.now();

        if (lease.isLeaseActive(now)) {
            return response(
                    false,
                    "Leadership lease is still active",
                    lease,
                    now
            );
        }

        lease.claim(nodeId, now.plus(LEASE_DURATION));

        return response(true, "Leadership claimed", lease, now);
    }

    @Transactional
    public LeaderResponse renew(String nodeId) {
        LeaderLease lease = lockLeaderRow();
        Instant now = Instant.now();

        if (!lease.isLeaseActive(now)
                || !nodeId.equals(lease.getLeaderNodeId())) {
            return response(
                    false,
                    "Node does not hold an active leadership lease",
                    lease,
                    now
            );
        }

        lease.renew(now.plus(LEASE_DURATION));

        return response(true, "Leadership renewed", lease, now);
    }

    @Transactional(readOnly = true)
    public LeaderResponse status() {
        LeaderLease lease = leaderRepository.findById(LEADER_ROW_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Leader row is not initialized"
                ));

        return response(
                true,
                "Current leadership state",
                lease,
                Instant.now()
        );
    }

    private void requireActiveNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "nodeId is required");
        }

        ClusterNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node is not registered"));

        if (node.getStatus() != NodeStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Node is inactive");
        }
    }

    private LeaderLease lockLeaderRow() {
        return leaderRepository.findByIdForUpdate(LEADER_ROW_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Leader row is not initialized"
                ));
    }

    private LeaderResponse response(
            boolean success,
            String message,
            LeaderLease lease,
            Instant now) {

        boolean active = lease.isLeaseActive(now);

        return new LeaderResponse(
                success,
                message,
                active ? lease.getLeaderNodeId() : null,
                lease.getEpoch(),
                active ? lease.getLeaseUntil() : null
        );
    }
}
