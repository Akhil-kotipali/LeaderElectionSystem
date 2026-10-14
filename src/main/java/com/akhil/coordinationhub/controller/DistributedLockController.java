
package com.akhil.coordinationhub.controller;

import com.akhil.coordinationhub.dto.LockResponse;
import com.akhil.coordinationhub.service.DistributedLockService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locks")
public class DistributedLockController {

    private final DistributedLockService service;

    public DistributedLockController(DistributedLockService service) {
        this.service = service;
    }

    @PostMapping("/{name}/acquire/{nodeId}")
    public LockResponse acquire(
            @PathVariable String name,
            @PathVariable String nodeId) {
        return service.acquire(name, nodeId);
    }

    @PostMapping("/{name}/renew/{nodeId}")
    public LockResponse renew(
            @PathVariable String name,
            @PathVariable String nodeId) {
        return service.renew(name, nodeId);
    }

    @PostMapping("/{name}/release/{nodeId}")
    public LockResponse release(
            @PathVariable String name,
            @PathVariable String nodeId) {
        return service.release(name, nodeId);
    }

    @GetMapping("/{name}")
    public LockResponse status(@PathVariable String name) {
        return service.status(name);
    }
}
