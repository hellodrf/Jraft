package com.cervidae.jraft.restful;

import com.cervidae.jraft.node.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@Log4j2
@RequestMapping("/raft")
@Api("RaftController")
public class RaftController implements ApplicationContextAware {

    private final RaftContext context;
    private ApplicationContext applicationContext;

    @Autowired
    public RaftController(RaftContext context) {
        this.context = context;
    }

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    // GET localhost:8080/raft/kill?n=1
    @GetMapping(value = "/kill", params = {"n"})
    @ApiOperation("/killNode")
    @ApiImplicitParam(name = "n", value = "n", required = true, paramType = "query", dataType = "int")
    public String kill(@RequestParam int n) {
        if (context instanceof LocalRaftContext) {
            context.getNodes().get(n).shutdown();
        } else {
            context.shutdown();
        }
        log.warn("Killed node " + n);
        return info();
    }

    // GET localhost:8080/raft/info
    @GetMapping(value = "/info")
    public String info() {
        var builder = new StringBuilder();
        context.getNodes().forEach((x)-> builder.append(x.toString()).append(System.lineSeparator()));
        return builder.toString();
    }

    @GetMapping(value = "/start", params = {"n"})
    public String start(@RequestParam int n) {
        context.getNodes().set(n, applicationContext.getBean(RaftNode.class));
        context.getNodes().get(n).start();
        log.warn("Started node " + n);
        return info();
    }

    @GetMapping(value = "/log")
    public List<String> getMessageLogs() {
        return context.getMessageLogs();
    }

    @PostMapping(value = "/query")
    public Response<?> query(@RequestBody String id) {
        log.info("Query recv: " + id);
        RaftNode node;
        if (context instanceof ClusteredRaftContext) {
            node = ((ClusteredRaftContext) context).getNode();
        } else {
            node = getLeader();
        }
        Assert.notNull(node, "Node is null?");
        int reply;
        try {
            reply = node.getStateMachine().query(id);
        } catch (IllegalArgumentException e) {
            return Response.success("entry do not exist");
        }
        log.info("Query reply: " + reply);
        return Response.success(Integer.toString(reply));
    }

    @PostMapping(value = "/command")
    public Response<?> command(@RequestBody String command) {
        log.info("Command recv: " + command);
        RaftNode node = getLeader();
        if (node == null || node.getState() != RaftNode.State.LEADER) {
            log.info("I am not leader, discarding command");
            return Response.fail("not_leader");
        } else {
            return Response.success(node.newEntry(command));
        }
    }

    private RaftNode getLeader() {
        if (context instanceof ClusteredRaftContext) {
            var node = context.getNodes().get(0);
            if (node.getState() == RaftNode.State.LEADER) {
                return node;
            } else {
                return null;
            }
        } else {
            for (RaftNode n: context.getNodes()) {
                if (n.getState() == RaftNode.State.LEADER) {
                    return n;
                }
            }
        }
        return null;
    }

    @PostMapping(value = "/sm")
    public Response<?> smPost(@RequestParam("k") String key, @RequestParam("v") int value) {
        RaftNode node;
        if (context instanceof ClusteredRaftContext) {
            node = ((ClusteredRaftContext) context).getNode();
        } else {
            node = getLeader();
        }
        Assert.notNull(node, "node not found?");
        var sm = node.getStateMachine();
        return Response.success(sm.put(key, value));
    }

    @GetMapping(value = "/sm")
    public Response<?> smGet(@RequestParam("k") String key) {
        RaftNode node;
        if (context instanceof ClusteredRaftContext) {
            node = ((ClusteredRaftContext) context).getNode();
        } else {
            node = getLeader();
        }
        Assert.notNull(node, "node not found?");
        var sm = node.getStateMachine();
        return Response.success(sm.query(key));
    }
}
