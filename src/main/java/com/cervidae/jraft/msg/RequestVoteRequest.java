package com.cervidae.jraft.msg;

import lombok.*;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestVoteRequest extends Message {

    String type = "RequestVoteRequest";

    @NonNull
    int term;

    @NonNull
    int candidateID;

    @NonNull
    int lastLogIndex;

    @NonNull
    int lastLogTerm;

}
