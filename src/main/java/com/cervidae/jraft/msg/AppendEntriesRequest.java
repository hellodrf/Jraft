package com.cervidae.jraft.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AppendEntriesRequest extends Message {

    String type = "AppendEntriesRequest";

}
