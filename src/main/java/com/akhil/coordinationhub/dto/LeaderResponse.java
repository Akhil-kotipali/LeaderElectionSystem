
package com.akhil.coordinationhub.dto;

import java.time.Instant;

public record LeaderResponse(
        boolean success,
        String message,
        String leaderNodeId,
        long epoch,
        Instant leaseUntil
) {}
