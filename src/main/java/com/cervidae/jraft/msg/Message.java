package com.cervidae.jraft.msg;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class Message {

    public final String type = "Message";

    public int source = -1;

    public final Class<? extends Message> replyClass = Message.class;

    long time = System.currentTimeMillis();

}
