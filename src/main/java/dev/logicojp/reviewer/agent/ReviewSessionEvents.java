package dev.logicojp.reviewer.agent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/// Binds session events to a {@link ContentCollector} in a transport-agnostic way.
final class ReviewSessionEvents {

    @FunctionalInterface
    interface SessionSubscription {
        AutoCloseable subscribe(Consumer<EventData> handler);
    }

    @FunctionalInterface
    interface TypedSessionSubscription<T> {
        AutoCloseable subscribe(Consumer<T> handler);
    }

    @FunctionalInterface
    interface TraceLogger {
        void log(Supplier<String> messageSupplier);
    }

    record EventData(String type, String content, int toolCalls, String errorMessage) {
    }

    private ReviewSessionEvents() {
    }

    static EventSubscriptions register(String agentName,
                                       ContentCollector collector,
                                       SessionSubscription allEvents,
                                       TypedSessionSubscription<EventData> messages,
                                       TypedSessionSubscription<EventData> idle,
                                       TypedSessionSubscription<EventData> error,
                                       TraceLogger traceLogger) {
        var allEventsSub = subscribeAllEvents(agentName, collector, allEvents, traceLogger);
        var messageSub = subscribeMessages(collector, messages);
        var idleSub = subscribeIdle(collector, idle);
        var errorSub = subscribeError(collector, error);

        return new EventSubscriptions(allEventsSub, messageSub, idleSub, errorSub);
    }

    private static AutoCloseable subscribeAllEvents(String agentName,
                                                    ContentCollector collector,
                                                    SessionSubscription allEvents,
                                                    TraceLogger traceLogger) {
        return allEvents.subscribe(event -> {
            collector.onActivity();
            traceLogger.log(() -> "Agent " + agentName + ": event received \u2014 " + event.type());
        });
    }

    private static AutoCloseable subscribeMessages(ContentCollector collector,
                                                   TypedSessionSubscription<EventData> messages) {
        return messages.subscribe(event -> collector.onMessage(event.content(), Math.max(0, event.toolCalls())));
    }

    private static AutoCloseable subscribeIdle(ContentCollector collector,
                                               TypedSessionSubscription<EventData> idle) {
        return idle.subscribe(_ -> collector.onIdle());
    }

    private static AutoCloseable subscribeError(ContentCollector collector,
                                                TypedSessionSubscription<EventData> error) {
        return error.subscribe(event -> collector.onError(errorMessageOrDefault(event)));
    }

    private static String errorMessageOrDefault(EventData event) {
        return event.errorMessage() != null ? event.errorMessage() : "session error";
    }
}
