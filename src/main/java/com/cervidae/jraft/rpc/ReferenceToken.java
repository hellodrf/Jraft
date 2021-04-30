package com.cervidae.jraft.rpc;

import lombok.Data;

@Data
public class ReferenceToken {

    String clazz;

    long serial;

    String[] methods;

}
