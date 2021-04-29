package com.cervidae.jraft.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AppendEntriesReply extends Message {

    String type = "AppendEntriesReply";

}
