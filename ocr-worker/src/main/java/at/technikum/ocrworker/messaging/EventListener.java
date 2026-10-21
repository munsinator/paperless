package at.technikum.ocrworker.messaging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class EventListener<T> {
    protected EventListener(){}

    public void consume(T event){
        log.info("Received event {}", event);
    }
}
