
package com.akhil.coordinationhub.controller;

import com.akhil.coordinationhub.dto.LeaderResponse;
import com.akhil.coordinationhub.service.LeaderElectionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leader")
public class LeaderController {

    private final LeaderElectionService service;

    public LeaderController(LeaderElectionService service) {
        this.service = service;
    }

    @PostMapping("/claim/{nodeId}")
    public LeaderResponse claim(@PathVariable String nodeId) {
        return service.claim(nodeId);
    }

    @PostMapping("/renew/{nodeId}")
    public LeaderResponse renew(@PathVariable String nodeId) {
        return service.renew(nodeId);
    }

    @GetMapping
    public LeaderResponse status() {
        return service.status();
    }
}
