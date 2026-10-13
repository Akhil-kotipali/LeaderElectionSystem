
package com.akhil.coordinationhub.service;

import com.akhil.coordinationhub.dto.NodeResponse;
import com.akhil.coordinationhub.entity.ClusterNode;
import com.akhil.coordinationhub.repository.ClusterNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ClusterNodeService {

    private final ClusterNodeRepository repository;

    public ClusterNodeService(ClusterNodeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NodeResponse register(String nodeId) {
        Instant now = Instant.now();

        ClusterNode node = repository.findById(nodeId)
                .orElseGet(() -> new ClusterNode(nodeId, now));

        node.recordHeartbeat(now);
        ClusterNode saved = repository.save(node);

        return toResponse(saved);
    }

    @Transactional
    public NodeResponse heartbeat(String nodeId) {
        ClusterNode node = repository.findById(nodeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown node: " + nodeId));

        node.recordHeartbeat(Instant.now());

        return toResponse(repository.save(node));
    }

    @Transactional(readOnly = true)
    public List<NodeResponse> listNodes() {
        return repository.findAll().stream()
                .map(ClusterNodeService::toResponse)
                .toList();
    }

    private static NodeResponse toResponse(ClusterNode node) {
        return new NodeResponse(
                node.getNodeId(),
                node.getStatus(),
                node.getLastHeartbeat(),
                node.getStartedAt()
        );
    }
}
