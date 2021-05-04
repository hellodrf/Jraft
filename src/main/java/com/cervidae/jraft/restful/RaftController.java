package com.cervidae.jraft.restful;

import com.cervidae.jraft.node.RaftContext;
import com.cervidae.jraft.node.RaftNode;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("raft")
public class RaftController implements ApplicationContextAware {

    private final RaftContext context;
    private ApplicationContext applicationContext;

    @Autowired
    public RaftController(RaftContext context) {
        this.context = context;
    }

    // GET localhost:8080/raft/kill?n=1
    @GetMapping(value = "kill", params = {"n"})
    public Response<?> shutdown(@RequestParam int n) {
        context.getNodes().get(n).shutdown();
        return Response.success(context);
    }

    // GET localhost:8080/raft/info
    @GetMapping(value = "info")
    public String info() {
        var builder = new StringBuilder();
        context.getNodes().forEach((x)-> builder.append(x.toString()).append(System.lineSeparator()));
        return builder.toString();
    }

    @GetMapping(value = "start", params = {"n"})
    public Response<?> start(@RequestParam int n) {
        context.getNodes().set(n, applicationContext.getBean(RaftNode.class));
        return Response.success(context);
    }

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
