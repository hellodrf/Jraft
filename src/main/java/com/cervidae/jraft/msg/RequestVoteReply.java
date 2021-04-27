package com.cervidae.jraft.msg;

import lombok.Data;

@Data
public class RequestVoteReply extends Message {

    String type = "RequestVoteReply";

}
