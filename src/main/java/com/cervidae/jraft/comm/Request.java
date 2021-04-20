package com.cervidae.jraft.comm;

import lombok.Data;

@Data
public class Request {

    long time = System.currentTimeMillis();

    ReferenceToken token;

    String method;

    Object[] payloads;

    String[] payloadClass;

}
