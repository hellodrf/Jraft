package com.cervidae.jraft.restful;

import com.cervidae.jraft.model.Command;
import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@ConditionalOnExpression("${cervidae.jraft.isMonitor:false}")
@Log4j2
@RequestMapping("/mon")
@Api("MonitorController")
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
    public Response<?> deposit(@RequestBody Command command) {
        Assert.notNull(command, "error input");
        Assert.notNull(command.getUserId(), "error input");
        log.info("deposit: {}", command);
        return monitorService.broadcastToLeader("/raft/command", "DEPOSIT;"+command.userId+";"+command.value);
    }

    // WITHDRAW;userid;value
    @PostMapping(value = "/withdraw")
    public Response<?> withdraw(@RequestBody Command command) {
        Assert.notNull(command, "error input");
        Assert.notNull(command.getUserId(), "error input");
        Assert.notNull(command.getValue(), "error input");
        log.info("withdraw: {}", command);
        return monitorService.broadcastToLeader("/raft/command", "WITHDRAW;"+command.userId+";"+command.value);
    }

}
