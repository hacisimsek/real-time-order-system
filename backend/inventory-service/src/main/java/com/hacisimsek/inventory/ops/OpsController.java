package com.hacisimsek.inventory.ops;


import com.hacisimsek.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
public class OpsController {

    private final DlqReplayer replayer;

    @PostMapping("/replay/{queue}")
    @PreAuthorize("hasAuthority('" + Roles.INVENTORY_OPS + "')")
    public ResponseEntity<?> replay(@PathVariable String queue,
                                    @RequestParam(name = "max", defaultValue = "100") int max) {
        int moved = replayer.replay(queue, max);
        return ResponseEntity.ok(Map.of("queue", queue, "moved", moved));
    }
}
