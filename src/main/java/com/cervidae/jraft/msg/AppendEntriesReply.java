package com.cervidae.jraft.msg;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
@NoArgsConstructor
public class AppendEntriesReply extends Message {

    public final String type = "AppendEntriesReply";

    public final Class<? extends Message> replyClass = null;

    @NonNull
    int term;

    @NonNull
    boolean success;

    @NonNull
    int nextIndex;

    public boolean getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "AppendEntriesReply{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }
}
