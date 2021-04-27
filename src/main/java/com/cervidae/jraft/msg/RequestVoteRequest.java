package com.cervidae.jraft.msg;

import lombok.Data;

@Data
public class RequestVoteRequest extends Message {

    String type = "RequestVoteRequest";

}
