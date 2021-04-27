package com.cervidae.jraft.node;

import lombok.Data;

import java.io.Serializable;

@Data
public class LogEntry implements Serializable {

    public static final long serialVersionUID = 4857814132681042987L;

    long term;

    Object command;

}
