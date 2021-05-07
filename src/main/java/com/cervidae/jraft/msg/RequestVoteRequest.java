package com.cervidae.jraft.msg;

import lombok.*;

@Data
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

    public RequestVoteRequest(@NonNull int term, @NonNull int candidateID,
                              @NonNull int lastLogIndex, @NonNull int lastLogTerm) {
        this.term = term;
        this.candidateID = candidateID;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
        this.source = candidateID;
    }

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
