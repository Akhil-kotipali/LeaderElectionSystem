
package com.akhil.coordinationhub.dto;

import com.akhil.coordinationhub.entity.NodeStatus;
import java.time.Instant;

public record NodeResponse(
        String nodeId,
        NodeStatus status,
        Instant lastHeartbeat,
        Instant startedAt
) {}
