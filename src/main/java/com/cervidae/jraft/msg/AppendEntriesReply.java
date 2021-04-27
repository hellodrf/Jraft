package com.cervidae.jraft.msg;

import lombok.Data;

@Data
public class AppendEntriesReply extends Message {

    String type = "AppendEntriesReply";

}
