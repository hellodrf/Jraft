package com.cervidae.jraft.msg;

import lombok.Data;

@Data
public class AppendEntriesRequest extends Message {

    String type = "AppendEntriesRequest";

}
