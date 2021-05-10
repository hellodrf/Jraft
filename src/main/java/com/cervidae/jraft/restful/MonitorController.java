package com.cervidae.jraft.restful;

import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@RestController
@ConditionalOnExpression("${cervidae.jraft.isMonitor:false}")
@Log4j2
@RequestMapping("/mon")
@Api("MonitorController")
public class MonitorController {

    final MonitorService monitorService;
    final Logger eventLogger =
            org.apache.logging.log4j.LogManager.getLogger("com.cervidae.jraft.MonitorClusterEvent");

    @Autowired
    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/info")
    public List<String> getClusterInfo() {
        return monitorService.getClusterStatus();
    }

    @ApiIgnore
    @PostMapping("/event")
    public void handleClusterEvent(@RequestBody String eventMsg) {
        eventLogger.info(eventMsg);
    }

    @GetMapping(value = "/check_balance")
    public Response<?> checkBalance(@RequestParam("userId") String userId) {
        Assert.notNull(userId, "error input");
        log.info("check_balance: {}", userId);
        return monitorService.broadcastToLeader("/raft/query", userId);
    }

    // CREATE;userid
    @PostMapping(value = "/create_account")
    public Response<?> createAccount(@RequestParam("userId") String userId) {
        Assert.notNull(userId, "error input");
        log.info("create_account: {}", userId);
        return monitorService.broadcastToLeader("/raft/command", "CREATE;"+userId);
    }

    // DEPOSIT;userid;value
    @PostMapping(value = "/deposit")
    public Response<?> deposit(@RequestParam("userId") String userId, @RequestParam("value") int value) {
        Assert.notNull(userId, "error input");
        log.info("deposit: {}", userId +":"+value);
        return monitorService.broadcastToLeader("/raft/command", "DEPOSIT;"+userId+";"+value);
    }

    // WITHDRAW;userid;value
    @PostMapping(value = "/withdraw")
    public Response<?> withdraw(@RequestParam("userId") String userId, @RequestParam("value") int value) {
        Assert.notNull(userId, "error input");
        log.info("withdraw: {}", userId +":"+value);
        return monitorService.broadcastToLeader("/raft/command", "WITHDRAW;"+userId+";"+value);
    }

    @GetMapping(value = "consensus")
    public List<String> check_consensus() {
        return monitorService.broadcastForString("/raft/log");
    }
}
