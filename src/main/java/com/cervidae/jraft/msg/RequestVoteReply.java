package com.cervidae.jraft.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestVoteReply extends Message {

    String type = "RequestVoteReply";

    @NonNull
    int term;

    @NonNull
    boolean voteGranted;

}
