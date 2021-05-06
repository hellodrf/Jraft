package com.cervidae.jraft.restful;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@ConditionalOnExpression("${cervidae.jraft.isMonitor:false}")
@RequestMapping("/mon")
public class MonitorController {

    final MonitorService monitorService;

    @Autowired
    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/info")
    public List<String> getClusterInfo() {
        return monitorService.getClusterStatus();
    }

}
