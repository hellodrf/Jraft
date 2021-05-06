package com.cervidae.jraft.msg;

import lombok.*;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RequestVoteRequest extends Message {

    String type = "RequestVoteRequest";

    public final Class<? extends Message> replyClass = RequestVoteReply.class;

    @NonNull
    int term;

    @NonNull
    int candidateID;

    @NonNull
    int lastLogIndex;

    @NonNull
    int lastLogTerm;

    @Override
    public String toString() {
        return "RequestVoteRequest{" +
                "term=" + term +
                ", candidateID=" + candidateID +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
