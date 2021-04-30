package com.cervidae.jraft.msg;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class AppendEntriesReply extends Message {

    String type = "AppendEntriesReply";

    @NonNull
    int term;

    @NonNull
    boolean success;

}
