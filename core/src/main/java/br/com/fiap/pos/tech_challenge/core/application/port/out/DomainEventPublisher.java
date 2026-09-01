package br.com.fiap.pos.tech_challenge.core.application.port.out;

/** Outbound port for publishing application/domain events. */
public interface DomainEventPublisher {
    void publish(Object event);
}
