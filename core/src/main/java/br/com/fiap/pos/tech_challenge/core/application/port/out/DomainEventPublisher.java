package br.com.fiap.pos.tech_challenge.core.application.port.out;

public interface DomainEventPublisher {
    void publish(Object event);
}
