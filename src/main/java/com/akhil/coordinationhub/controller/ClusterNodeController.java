
package com.akhil.coordinationhub.controller;

import com.akhil.coordinationhub.dto.NodeResponse;
import com.akhil.coordinationhub.service.ClusterNodeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
public class ClusterNodeController {

    private final ClusterNodeService service;

    public ClusterNodeController(ClusterNodeService service) {
        this.service = service;
    }

    @PostMapping("/{nodeId}/register")
    public NodeResponse register(@PathVariable String nodeId) {
        return service.register(nodeId);
    }

    @PostMapping("/{nodeId}/heartbeat")
    public NodeResponse heartbeat(@PathVariable String nodeId) {
        return service.heartbeat(nodeId);
    }

    @GetMapping
    public List<NodeResponse> listNodes() {
        return service.listNodes();
    }
}
