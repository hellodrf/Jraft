package com.cervidae.jraft.restful;

import com.cervidae.jraft.node.RaftContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("raft")
public class RaftController {

    final RaftContext context;

    @Autowired
    public RaftController(RaftContext context) {
        this.context = context;
    }

    @GetMapping(value = "shutdown")
    public Response<?> shutdown() {
        context.shutdown();
        return Response.success("Hello world!");
    }

}
