package com.cervidae.jraft.restful;

import com.cervidae.jraft.msg.*;
import com.cervidae.jraft.node.ClusteredRaftContext;
import com.cervidae.jraft.node.RaftContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
@RequestMapping("/rpc")
public class RPCController {

    private final RaftContext context;

    @Autowired
    public RPCController(RaftContext context) {
        this.context = context;
    }

    @PostMapping(value = "/vote")
    public RequestVoteReply requestVote(@RequestBody RequestVoteRequest msg) {
        Assert.isTrue(context instanceof ClusteredRaftContext, "LocalContext doesn't support RPC");
        return (RequestVoteReply)context.getNodes().get(0).dispatchRequest(msg);
    }

    @PostMapping(value = "/entry")
    public AppendEntriesReply appendEntries(@RequestBody AppendEntriesRequest msg) {
        Assert.isTrue(context instanceof ClusteredRaftContext, "LocalContext doesn't support RPC");
        return (AppendEntriesReply)context.getNodes().get(0).dispatchRequest(msg);
    }
}
