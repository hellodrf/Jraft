package com.cervidae.jraft.msg;

import lombok.Data;

@Data
public abstract class Message {

    String type = "Message";

    long time = System.currentTimeMillis();

}
