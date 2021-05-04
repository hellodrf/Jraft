package com.cervidae.jraft.msg;

import com.cervidae.jraft.node.LogEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AppendEntriesRequest extends Message {

    String type = "AppendEntriesRequest";

    LogEntry entry;

    public AppendEntriesRequest(LogEntry entry) {
        this.entry = entry;
    }

}
