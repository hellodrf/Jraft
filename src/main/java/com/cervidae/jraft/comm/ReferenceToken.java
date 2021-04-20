package com.cervidae.jraft.comm;

import lombok.Data;

@Data
public class ReferenceToken {

    String clazz;

    long serial;

    String[] methods;

}
