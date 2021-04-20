package com.cervidae.jraft.node;

import lombok.Data;

@Data
public class LogEntry {

    long term;

    String key;

    String action;

}
