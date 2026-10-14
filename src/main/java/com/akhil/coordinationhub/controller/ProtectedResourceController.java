
package com.akhil.coordinationhub.controller;

import com.akhil.coordinationhub.dto.ResourceWriteRequest;
import com.akhil.coordinationhub.service.ProtectedResourceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
public class ProtectedResourceController {

    private final ProtectedResourceService service;

    public ProtectedResourceController(ProtectedResourceService service) {
        this.service = service;
    }

    @PutMapping("/{resourceKey}")
    public String write(
            @PathVariable String resourceKey,
            @RequestParam String lockName,
            @RequestParam String nodeId,
            @RequestBody ResourceWriteRequest request) {

        return service.write(
                lockName,
                nodeId,
                request.fencingToken(),
                resourceKey,
                request.value()
        );
    }
}
