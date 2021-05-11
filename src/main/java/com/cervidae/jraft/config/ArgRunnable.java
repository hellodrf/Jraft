package com.cervidae.jraft.config;

/**
 * ArgRunnable: A lambda function that takes exactly one argument, no returns, no exceptions.
 * @param <T> Argument type
 */
@FunctionalInterface
public interface ArgRunnable<T> {

    void run(T arg);

}
