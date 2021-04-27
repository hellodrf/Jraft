package com.cervidae.jraft.async;

/**
 * ArgRunnable: A lambda function that takes exactly one argument, no returns, no exceptions.
 * @param <T> Argument type
 */
@FunctionalInterface
public interface ArgRunnable<T> {

    void run(T arg);

}
