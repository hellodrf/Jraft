package com.cervidae.jraft.restful;

import com.cervidae.jraft.node.LocalRaftContext;
import com.cervidae.jraft.node.RaftContext;
import com.cervidae.jraft.node.RaftNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


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

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
