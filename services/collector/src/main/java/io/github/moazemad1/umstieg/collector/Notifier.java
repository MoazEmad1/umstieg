package io.github.moazemad1.umstieg.collector;

@FunctionalInterface
public interface Notifier {

    Notifier NONE = text -> { };

    void send(String text);
}
