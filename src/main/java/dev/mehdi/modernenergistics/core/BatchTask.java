package dev.mehdi.modernenergistics.core;

/** Access to the CPU's remaining, already-planned pattern executions. */
public interface BatchTask {
    long me$remaining();
    void me$remaining(long value);
}
