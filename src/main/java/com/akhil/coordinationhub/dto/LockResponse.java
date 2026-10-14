
package com.akhil.coordinationhub.dto;

import java.time.Instant;

public record LockResponse(
        boolean success,
        String message,
        String lockName,
        String ownerNodeId,
        long fencingToken,
        Instant leaseUntil
) {}
