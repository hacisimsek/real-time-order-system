package com.hacisimsek.rtos.reporting.ops;

import com.hacisimsek.rtos.security.Roles;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops")
public class OpsController {

    private final DlqReplayer replayer;

    public OpsController(DlqReplayer replayer) {
        this.replayer = replayer;
    }

    @PostMapping("/replay/{queue}")
    @PreAuthorize("hasAuthority('" + Roles.REPORTING_EXPORT + "')")
    public ResponseEntity<?> replay(@PathVariable String queue,
                                    @RequestParam(name = "max", defaultValue = "100") int max) {
        int moved = replayer.replay(queue, max);
        return ResponseEntity.ok(Map.of("queue", queue, "moved", moved));
    }
}
