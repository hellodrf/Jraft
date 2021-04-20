package com.cervidae.jraft.comm;

import lombok.Data;

@Data
public class Reply {

    long time = System.currentTimeMillis();

    ReferenceToken token;

    boolean ok;

    String exception;

    Object payload;

    String payloadClass;

    public Reply(ReferenceToken token, Object payload, String payloadClass) {
        this.ok = true;
        this.token = token;
        this.payload = payload;
        this.payloadClass = payloadClass;
    }

    public Reply(ReferenceToken token, String exception) {
        this.ok = true;
        this.token = token;
        this.exception = exception;
    }

}
