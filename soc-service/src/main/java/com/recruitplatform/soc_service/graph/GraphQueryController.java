package com.recruitplatform.soc_service.controller;

import com.recruitplatform.soc_service.graph.GraphQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/graph")
public class GraphQueryController {

    private final GraphQueryService graphQueryService;

    public GraphQueryController(GraphQueryService graphQueryService) {
        this.graphQueryService = graphQueryService;
    }

    @GetMapping("/alerts-by-service")
    public Collection<Map<String, Object>> alertsByService(@RequestParam String service) {
        return graphQueryService.alertsByService(service);
    }

    @GetMapping("/high-severity-users")
    public Collection<Map<String, Object>> highSeverityUsers() {
        return graphQueryService.usersWithHighSeverityAlerts();
    }

    @GetMapping("/controls-for-alert/{alertId}")
    public Collection<Map<String, Object>> controlsForAlert(@PathVariable String alertId) {
        return graphQueryService.controlsForAlert(alertId);
    }

    @GetMapping("/dependency-impact")
    public Collection<Map<String, Object>> dependencyImpact(@RequestParam String service) {
        return graphQueryService.dependencyImpact(service);
    }

    @GetMapping("/threats-for-service")
    public Collection<Map<String, Object>> threatsForService(@RequestParam String service) {
        return graphQueryService.threatsForService(service);
    }

    @GetMapping("/evidence-for-alert/{alertId}")
    public Collection<Map<String, Object>> evidenceForAlert(@PathVariable String alertId) {
        return graphQueryService.evidenceForAlert(alertId);
    }
}