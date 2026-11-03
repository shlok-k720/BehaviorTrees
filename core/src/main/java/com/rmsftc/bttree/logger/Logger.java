package com.rmsftc.bttree.logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Logger {
    public static final String NODE_STARTED = "behaviorTrees.node.started";
    public static final String NODE_TICKED = "behaviorTrees.node.ticked";
    public static final String NODE_FINISHED = "behaviorTrees.node.finished";
    public static final String NODE_INVALIDATED = "behaviorTrees.node.invalidated";
    public static final String NODE_RESET = "behaviorTrees.node.reset";

    private static final Set<LogSystem> loggers = new CopyOnWriteArraySet<>();

    public Logger() {}

    public static void reset() {
        loggers.clear();
    }

    public static void attachLogger(LogSystem... loggers) {
        Objects.requireNonNull(loggers, "loggers");
        for (LogSystem logger : loggers) {
            attachLogger(logger);
        }
    }

    public static void attachLogger(LogSystem logger) {
        loggers.add(Objects.requireNonNull(logger, "logger"));
    }

    public static void removeLogger(LogSystem logSystem) {
        loggers.remove(Objects.requireNonNull(logSystem, "logSystem"));
    }

    public static void log(String caption, String data) {
        Objects.requireNonNull(caption, "caption");
        Objects.requireNonNull(data, "data");

        double timestampMs = System.currentTimeMillis();
        loggers.forEach(logger -> logger.log(caption, data, timestampMs));
    }
}
