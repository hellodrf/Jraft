package com.cervidae.jraft.msg;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class Message {

    public final String type = "Message";

    public final Class<? extends Message> replyClass = Message.class;

    long time = System.currentTimeMillis();

}
