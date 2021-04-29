package com.cervidae.jraft.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RequestVoteReply extends Message {

    String type = "RequestVoteReply";

    int term;

    boolean voteGranted;

}
