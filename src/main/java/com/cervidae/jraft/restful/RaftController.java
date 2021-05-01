package com.cervidae.jraft.restful;

import com.cervidae.jraft.node.RaftContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("raft")
public class RaftController {

    private final RaftContext context;

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
}
