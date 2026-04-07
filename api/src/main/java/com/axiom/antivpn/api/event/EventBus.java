package com.axiom.antivpn.api.event;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    private final List<Consumer<IpCheckEvent>> listeners = new CopyOnWriteArrayList<>();

    private EventBus() {
    }

    public static @NotNull EventBus get() {
        return INSTANCE;
    }

    public void subscribe(@NotNull Consumer<IpCheckEvent> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(@NotNull Consumer<IpCheckEvent> listener) {
        listeners.remove(listener);
    }

    public @NotNull IpCheckEvent fire(@NotNull IpCheckEvent event) {
        for (Consumer<IpCheckEvent> listener : listeners) {
            listener.accept(event);
        }
        return event;
    }

    public void clear() {
        listeners.clear();
    }
}
