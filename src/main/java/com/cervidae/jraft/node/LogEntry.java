package com.cervidae.jraft.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEntry implements Serializable {

    public static final long serialVersionUID = 4857814132681042987L;

    int term;

    Object command;

}
