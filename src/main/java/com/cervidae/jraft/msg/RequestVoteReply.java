package com.cervidae.jraft.msg;

import lombok.*;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor

public class RequestVoteReply extends Message {

    String type = "RequestVoteReply";

    public final Class<? extends Message> replyClass = null;

    @NonNull
    int term;

    @NonNull
    boolean voteGranted;

}
