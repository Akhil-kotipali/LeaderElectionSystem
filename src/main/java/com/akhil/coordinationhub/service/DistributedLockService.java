
package com.akhil.coordinationhub.service;

import com.akhil.coordinationhub.dto.LockResponse;
import com.akhil.coordinationhub.repository.ClusterNodeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;

@Service
public class DistributedLockService {

    private static final int LEASE_SECONDS = 15;

    private final JdbcTemplate jdbc;
    private final ClusterNodeRepository nodes;

    public DistributedLockService(
            JdbcTemplate jdbc,
            ClusterNodeRepository nodes) {
        this.jdbc = jdbc;
        this.nodes = nodes;
    }

    @Transactional
    public LockResponse acquire(String name, String nodeId) {
        validate(name, nodeId);

        // Ensure lock entry exists before pessimistic lock acquisition
        jdbc.update("""
            INSERT INTO distributed_lock
                (lock_name, owner_node_id, lease_until, fencing_token)
            VALUES (?, NULL, NULL, 0)
            ON DUPLICATE KEY UPDATE lock_name = VALUES(lock_name)
            """, name);

        LockRow row = lockRow(name);

        if (row.active()) {
            return response(false, "Lock is already held", name, row);
        }

        jdbc.update("""
            UPDATE distributed_lock
            SET owner_node_id = ?,
                lease_until = TIMESTAMPADD(SECOND, ?, CURRENT_TIMESTAMP(6)),
                fencing_token = fencing_token + 1
            WHERE lock_name = ?
            """, nodeId, LEASE_SECONDS, name);

        return response(true, "Lock acquired", name, lockRow(name));
    }

    @Transactional
    public LockResponse renew(String name, String nodeId) {
        LockRow row = lockRow(name);

        if (!row.active() || !nodeId.equals(row.owner())) {
            return response(false, "No active lock owned by this node",
                    name, row);
        }

        int updated = jdbc.update("""
            UPDATE distributed_lock
            SET lease_until = TIMESTAMPADD(SECOND, ?, CURRENT_TIMESTAMP(6))
            WHERE lock_name = ?
              AND owner_node_id = ?
              AND lease_until > CURRENT_TIMESTAMP(6)
            """, LEASE_SECONDS, name, nodeId);

        if (updated == 0) {
            return response(false, "Lease expired before renewal", name,
                    lockRow(name));
        }
        return response(true, "Lock renewed", name, lockRow(name));
    }

    @Transactional
    public LockResponse release(String name, String nodeId) {
        LockRow row = lockRow(name);

        if (!row.active() || !nodeId.equals(row.owner())) {
            return response(false, "No active lock owned by this node",
                    name, row);
        }

        int updated = jdbc.update("""
            UPDATE distributed_lock
            SET owner_node_id = NULL, lease_until = NULL
            WHERE lock_name = ?
              AND owner_node_id = ?
              AND lease_until > CURRENT_TIMESTAMP(6)
            """, name, nodeId);

        if (updated == 0) {
            return response(false, "Lease expired before release", name,
                    lockRow(name));
        }

        return response(true, "Lock released", name, lockRow(name));
    }

    @Transactional(readOnly = true)
    public LockResponse status(String name) {
        LockRow row = readRow(name);
        return response(true, "Lock status", name, row);
    }

    private void validate(String name, String nodeId) {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid lock name");
        }

        if (nodeId == null || nodeId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "nodeId is required");
        }

        if (!nodes.existsById(nodeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Node is not registered");
        }
    }

    private LockRow lockRow(String name) {
        return jdbc.queryForObject("""
            SELECT lock_name, owner_node_id, lease_until,
                   fencing_token,
                   (lease_until > CURRENT_TIMESTAMP(6)) AS active
            FROM distributed_lock
            WHERE lock_name = ?
            FOR UPDATE
            """, (rs, rowNum) -> new LockRow(
                rs.getString("lock_name"),
                rs.getString("owner_node_id"),
                rs.getTimestamp("lease_until"),
                rs.getLong("fencing_token"),
                rs.getBoolean("active")
            ), name);
    }

    private LockRow readRow(String name) {
        return jdbc.queryForObject("""
            SELECT lock_name, owner_node_id, lease_until,
                   fencing_token,
                   (lease_until > CURRENT_TIMESTAMP(6)) AS active
            FROM distributed_lock
            WHERE lock_name = ?
            """, (rs, rowNum) -> new LockRow(
                rs.getString("lock_name"),
                rs.getString("owner_node_id"),
                rs.getTimestamp("lease_until"),
                rs.getLong("fencing_token"),
                rs.getBoolean("active")
            ), name);
    }

    private LockResponse response(
            boolean success, String message,
            String name, LockRow row) {
        return new LockResponse(
                success, message, name,
                row.active() ? row.owner() : null,
                row.token(),
                row.active() && row.leaseUntil() != null
                        ? row.leaseUntil().toInstant() : null
        );
    }

    private record LockRow(
            String name,
            String owner,
            Timestamp leaseUntil,
            long token,
            boolean active) {}
}
