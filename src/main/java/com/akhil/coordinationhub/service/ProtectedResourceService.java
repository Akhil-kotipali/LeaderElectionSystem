
package com.akhil.coordinationhub.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProtectedResourceService {

    private final JdbcTemplate jdbc;

    public ProtectedResourceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public String write(
            String lockName,
            String nodeId,
            long fencingToken,
            String resourceKey,
            String value) {

        if (value == null || value.length() > 500
                || resourceKey == null || resourceKey.isBlank()
                || resourceKey.length() > 120) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid resource input");
        }

        // Validate authoritative lock state and fencing token under row lock
        var lease = jdbc.queryForObject("""
            SELECT owner_node_id, fencing_token,
                   (lease_until > CURRENT_TIMESTAMP(6)) AS active
            FROM distributed_lock
            WHERE lock_name = ?
            FOR UPDATE
            """, (rs, rowNum) -> new Object[] {
                rs.getString("owner_node_id"),
                rs.getLong("fencing_token"),
                rs.getBoolean("active")
            }, lockName);

        String owner = (String) lease[0];
        long currentToken = (long) lease[1];
        boolean active = (boolean) lease[2];

        if (!active || !nodeId.equals(owner)
                || fencingToken != currentToken) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stale or invalid lock owner; write rejected");
        }

        jdbc.update("""
            INSERT INTO protected_resource
                (resource_key, value_text, last_fencing_token)
            VALUES (?, '', 0)
            ON DUPLICATE KEY UPDATE resource_key = VALUES(resource_key)
            """, resourceKey);

        var resource = jdbc.queryForObject("""
            SELECT last_fencing_token
            FROM protected_resource
            WHERE resource_key = ?
            FOR UPDATE
            """, (rs, rowNum) -> rs.getLong(1), resourceKey);

        if (fencingToken < resource) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Fencing token is older than the resource token");
        }

        jdbc.update("""
            UPDATE protected_resource
            SET value_text = ?, last_fencing_token = ?
            WHERE resource_key = ?
            """, value, fencingToken, resourceKey);

        return "Write accepted with fencing token " + fencingToken;
    }
}
