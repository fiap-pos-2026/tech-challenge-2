package br.com.fiap.pos.tech_challenge.core.infrastructure.event;

import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    @Override
    public void publish(Object event) { delegate.publishEvent(event); }
}
