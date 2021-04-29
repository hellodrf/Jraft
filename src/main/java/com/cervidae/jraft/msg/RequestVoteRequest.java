package com.cervidae.jraft.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RequestVoteRequest extends Message {

    String type = "RequestVoteRequest";

    int term;

    int candidateID;

    int lastLogIndex;

    int lastLogTerm;

}
